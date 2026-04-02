package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TerminalFilePicker {

    private static final String ANSI_CLEAR = "\033[H\033[2J";
    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_LIST_ROWS = 14;

    private TerminalFilePicker() {
    }

    public static boolean isAvailable() {
        return System.console() != null;
    }

    public static PPScript pickScript(
            String title,
            List<PPScript> scripts) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Terminal picker requires an interactive TTY.");
        }
        if (scripts.isEmpty()) {
            return null;
        }

        int cursor = 0;
        int scrollOffset = 0;
        try (RawTerminal terminal = new RawTerminal()) {
            while (true) {
                cursor = Math.max(0, Math.min(cursor, scripts.size() - 1));
                int visibleRows = Math.max(6, terminal.rows() - 12);
                if (cursor < scrollOffset) {
                    scrollOffset = cursor;
                } else if (cursor >= scrollOffset + visibleRows) {
                    scrollOffset = cursor - visibleRows + 1;
                }

                renderScriptPicker(title, scripts, cursor, scrollOffset, terminal.rows());
                Key key = readKey(System.in);
                if (key == Key.UP) {
                    if (cursor > 0) {
                        cursor--;
                    }
                    continue;
                }
                if (key == Key.DOWN) {
                    if (cursor < scripts.size() - 1) {
                        cursor++;
                    }
                    continue;
                }
                if (key == Key.OPEN || key == Key.RIGHT || key == Key.CONFIRM) {
                    return scripts.get(cursor);
                }
                if (key == Key.QUIT || key == Key.LEFT || key == Key.BACKSPACE) {
                    return null;
                }
            }
        }
    }

    public static SelectionResult pick(
            String title,
            InputItem item,
            Path startDir) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Terminal file picker requires an interactive TTY.");
        }

        Path currentDir = normalizeStartDir(startDir);
        List<PathMatcher> matchers = buildMatchers(item.filetype);
        boolean multiple = item.num.contains("directory");
        boolean allowEmpty = item.num.contains("option");
        LinkedHashSet<Path> selected = new LinkedHashSet<>();
        int cursor = 0;
        int scrollOffset = 0;

        try (RawTerminal terminal = new RawTerminal()) {
            while (true) {
                List<FileEntry> entries = listEntries(currentDir, matchers);
                if (entries.isEmpty()) {
                    cursor = 0;
                    scrollOffset = 0;
                } else {
                    cursor = Math.max(0, Math.min(cursor, entries.size() - 1));
                    int visibleRows = Math.max(6, terminal.rows() - 10);
                    if (cursor < scrollOffset) {
                        scrollOffset = cursor;
                    } else if (cursor >= scrollOffset + visibleRows) {
                        scrollOffset = cursor - visibleRows + 1;
                    }
                }

                render(title, item, currentDir, entries, cursor, scrollOffset, selected, terminal.rows(), multiple,
                        allowEmpty, matchers);
                Key key = readKey(System.in);
                if (key == Key.UP) {
                    if (cursor > 0) {
                        cursor--;
                    }
                    continue;
                }
                if (key == Key.DOWN) {
                    if (cursor < entries.size() - 1) {
                        cursor++;
                    }
                    continue;
                }
                if (key == Key.LEFT || key == Key.BACKSPACE) {
                    Path parent = currentDir.getParent();
                    if (parent != null) {
                        currentDir = parent;
                        cursor = 0;
                        scrollOffset = 0;
                    }
                    continue;
                }
                if (key == Key.QUIT) {
                    return new SelectionResult(currentDir, new ArrayList<>(selected), true);
                }
                if (key == Key.CONFIRM) {
                    if (!selected.isEmpty() || allowEmpty) {
                        return new SelectionResult(currentDir, new ArrayList<>(selected), false);
                    }
                    continue;
                }
                if (entries.isEmpty()) {
                    continue;
                }

                FileEntry entry = entries.get(cursor);
                if (key == Key.RIGHT || key == Key.OPEN) {
                    if (entry.parentEntry) {
                        if (currentDir.getParent() != null) {
                            currentDir = currentDir.getParent();
                            cursor = 0;
                            scrollOffset = 0;
                        }
                    } else if (entry.directory) {
                        currentDir = entry.path;
                        cursor = 0;
                        scrollOffset = 0;
                    } else if (!multiple) {
                        selected.clear();
                        selected.add(entry.path);
                        return new SelectionResult(currentDir, new ArrayList<>(selected), false);
                    } else {
                        toggleSingleSelection(selected, entry.path);
                    }
                    continue;
                }

                if (key == Key.TOGGLE) {
                    if (entry.parentEntry) {
                        continue;
                    }
                    if (entry.directory) {
                        toggleDirectorySelection(selected, entry.path, matchers);
                    } else {
                        toggleSingleSelection(selected, entry.path);
                        if (!multiple) {
                            return new SelectionResult(currentDir, new ArrayList<>(selected), false);
                        }
                    }
                }
            }
        }
    }

    private static Path normalizeStartDir(Path startDir) {
        Path resolved = (startDir == null ? Path.of(".") : startDir).toAbsolutePath().normalize();
        if (Files.isDirectory(resolved)) {
            return resolved;
        }
        Path parent = resolved.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            return parent;
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    private static List<PathMatcher> buildMatchers(String fileTypes) {
        List<PathMatcher> matchers = new ArrayList<>();
        if (fileTypes == null || fileTypes.isBlank()) {
            return matchers;
        }
        for (String token : fileTypes.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + trimmed));
            }
        }
        return matchers;
    }

    private static List<FileEntry> listEntries(Path directory, List<PathMatcher> matchers) throws IOException {
        List<FileEntry> directories = new ArrayList<>();
        List<FileEntry> files = new ArrayList<>();

        if (directory.getParent() != null) {
            directories.add(FileEntry.parent(directory.getParent()));
        }

        try (var stream = Files.list(directory)) {
            for (Path path : stream.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList()) {
                if (Files.isDirectory(path)) {
                    directories.add(FileEntry.directory(path));
                } else if (Files.isRegularFile(path) && matches(path, matchers)) {
                    files.add(FileEntry.file(path));
                }
            }
        }

        directories.addAll(files);
        return directories;
    }

    private static boolean matches(Path path, List<PathMatcher> matchers) {
        if (matchers.isEmpty()) {
            return true;
        }
        Path fileName = path.getFileName();
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static void toggleSingleSelection(Set<Path> selected, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (selected.contains(normalized)) {
            selected.remove(normalized);
        } else {
            selected.add(normalized);
        }
    }

    private static void toggleDirectorySelection(Set<Path> selected, Path directory, List<PathMatcher> matchers)
            throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            for (Path path : stream.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList()) {
                if (Files.isRegularFile(path) && matches(path, matchers)) {
                    files.add(path.toAbsolutePath().normalize());
                }
            }
        }
        if (files.isEmpty()) {
            return;
        }
        boolean allSelected = true;
        for (Path file : files) {
            if (!selected.contains(file)) {
                allSelected = false;
                break;
            }
        }
        if (allSelected) {
            selected.removeAll(files);
        } else {
            selected.addAll(files);
        }
    }

    private static void render(
            String title,
            InputItem item,
            Path currentDir,
            List<FileEntry> entries,
            int cursor,
            int scrollOffset,
            Set<Path> selected,
            int terminalRows,
            boolean multiple,
            boolean allowEmpty,
            List<PathMatcher> matchers) {
        int visibleRows = Math.max(6, terminalRows - 10);
        StringBuilder screen = new StringBuilder();
        screen.append(ANSI_CLEAR);
        screen.append(title).append(System.lineSeparator());
        screen.append(item.id).append(" : ").append(item.desc).append(System.lineSeparator());
        screen.append("Current: ").append(currentDir).append(System.lineSeparator());
        screen.append("Filter : ").append(item.filetype == null || item.filetype.isBlank() ? "(all files)" : item.filetype)
                .append(System.lineSeparator());
        if (multiple) {
            screen.append(
                    "Keys   : Up/Down move, Enter open dir, Space select file or all matching files in dir, Left/Backspace parent, c confirm, q cancel")
                    .append(System.lineSeparator());
        } else {
            screen.append(
                    "Keys   : Up/Down move, Enter select file or open dir, Space select current, Left/Backspace parent, c confirm current selection, q cancel")
                    .append(System.lineSeparator());
        }
        if (allowEmpty) {
            screen.append("Optional input. `c` can confirm with no selection.").append(System.lineSeparator());
        }
        screen.append("Selected: ").append(selected.size()).append(System.lineSeparator());
        screen.append(System.lineSeparator());

        int end = Math.min(entries.size(), scrollOffset + visibleRows);
        if (entries.isEmpty()) {
            screen.append("(No matching files in this directory)").append(System.lineSeparator());
        } else {
            for (int i = scrollOffset; i < end; i++) {
                FileEntry entry = entries.get(i);
                boolean selectedEntry = isEntrySelected(entry, selected, matchers);
                screen.append(i == cursor ? "> " : "  ");
                screen.append(selectedEntry ? "[x] " : "[ ] ");
                screen.append(entry.displayName()).append(System.lineSeparator());
            }
        }

        screen.append(System.lineSeparator());
        if (!selected.isEmpty()) {
            screen.append("Picked :").append(System.lineSeparator());
            int shown = 0;
            for (Path path : selected) {
                screen.append("  ").append(path.getFileName()).append(System.lineSeparator());
                shown++;
                if (shown >= Math.max(4, terminalRows - visibleRows - 9)) {
                    if (selected.size() > shown) {
                        screen.append("  ... ").append(selected.size() - shown).append(" more").append(System.lineSeparator());
                    }
                    break;
                }
            }
        }
        System.out.print(screen.toString());
        System.out.flush();
    }

    private static void renderScriptPicker(
            String title,
            List<PPScript> scripts,
            int cursor,
            int scrollOffset,
            int terminalRows) {
        int visibleRows = Math.max(6, terminalRows - 12);
        int end = Math.min(scripts.size(), scrollOffset + visibleRows);
        PPScript current = scripts.get(cursor);

        StringBuilder screen = new StringBuilder();
        screen.append(ANSI_CLEAR);
        screen.append(title).append(System.lineSeparator());
        screen.append("Keys   : Up/Down move, Enter select, q cancel").append(System.lineSeparator());
        screen.append("Scripts: ").append(scripts.size()).append(System.lineSeparator());
        screen.append(System.lineSeparator());

        for (int i = scrollOffset; i < end; i++) {
            PPScript script = scripts.get(i);
            screen.append(i == cursor ? "> " : "  ");
            screen.append(script.filename);
            String summary = script.getSummary();
            if (!summary.isBlank()) {
                screen.append("  -  ").append(summary);
            }
            screen.append(System.lineSeparator());
        }

        screen.append(System.lineSeparator());
        screen.append("Selected").append(System.lineSeparator());
        screen.append("  Name : ").append(current.filename).append(System.lineSeparator());
        if (!current.getCategory().isBlank()) {
            screen.append("  Type : ").append(current.getCategory()).append(System.lineSeparator());
        }
        String explanation = current.explanation == null ? "" : current.explanation.trim();
        if (!explanation.isBlank()) {
            String[] lines = explanation.split("\\R");
            screen.append("  Info : ").append(lines[0]).append(System.lineSeparator());
            for (int i = 1; i < lines.length && i < 4; i++) {
                if (!lines[i].isBlank()) {
                    screen.append("         ").append(lines[i]).append(System.lineSeparator());
                }
            }
        }
        if (!current.inputs.isEmpty()) {
            screen.append("  In   : ").append(current.inputs.size()).append(" input(s)").append(System.lineSeparator());
        }
        if (!current.options.isEmpty()) {
            screen.append("  Opt  : ").append(current.options.size()).append(" option(s)").append(System.lineSeparator());
        }

        System.out.print(screen.toString());
        System.out.flush();
    }

    private static boolean isEntrySelected(FileEntry entry, Set<Path> selected, List<PathMatcher> matchers) {
        if (entry.parentEntry) {
            return false;
        }
        if (!entry.directory) {
            return selected.contains(entry.path.toAbsolutePath().normalize());
        }
        try (var stream = Files.list(entry.path)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(path -> matches(path, matchers))
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
            return !files.isEmpty() && selected.containsAll(files);
        } catch (IOException e) {
            return false;
        }
    }

    private static Key readKey(InputStream inputStream) throws IOException {
        int first = inputStream.read();
        if (first == -1) {
            return Key.QUIT;
        }
        if (first == 27) {
            int second = inputStream.read();
            if (second != '[') {
                return Key.NONE;
            }
            int third = inputStream.read();
            return switch (third) {
            case 'A' -> Key.UP;
            case 'B' -> Key.DOWN;
            case 'C' -> Key.RIGHT;
            case 'D' -> Key.LEFT;
            default -> Key.NONE;
            };
        }
        return switch (first) {
        case '\r', '\n' -> Key.OPEN;
        case ' ' -> Key.TOGGLE;
        case 'c', 'C' -> Key.CONFIRM;
        case 'q', 'Q' -> Key.QUIT;
        case 127, 8 -> Key.BACKSPACE;
        default -> Key.NONE;
        };
    }

    private static int detectRows() {
        try {
            Process process = new ProcessBuilder("bash", "-lc", "stty size < /dev/tty").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                process.waitFor();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2) {
                        return Integer.parseInt(parts[0]);
                    }
                }
            }
        } catch (Exception e) {
        }
        return DEFAULT_ROWS;
    }

    private enum Key {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        OPEN,
        TOGGLE,
        CONFIRM,
        BACKSPACE,
        QUIT,
        NONE
    }

    private static final class FileEntry {
        private final Path path;
        private final boolean directory;
        private final boolean parentEntry;

        private FileEntry(Path path, boolean directory, boolean parentEntry) {
            this.path = path;
            this.directory = directory;
            this.parentEntry = parentEntry;
        }

        static FileEntry parent(Path path) {
            return new FileEntry(path, true, true);
        }

        static FileEntry directory(Path path) {
            return new FileEntry(path, true, false);
        }

        static FileEntry file(Path path) {
            return new FileEntry(path, false, false);
        }

        String displayName() {
            if (parentEntry) {
                return "../";
            }
            return path.getFileName().toString() + (directory ? "/" : "");
        }
    }

    private static final class RawTerminal implements AutoCloseable {
        private final String savedState;
        private final int rows;

        RawTerminal() throws IOException {
            this.savedState = runStty("stty -g < /dev/tty");
            runStty("stty -echo -icanon min 1 time 0 < /dev/tty");
            this.rows = detectRows();
        }

        int rows() {
            return rows > 0 ? rows : DEFAULT_LIST_ROWS;
        }

        @Override
        public void close() throws IOException {
            try {
                runStty("stty " + savedState + " < /dev/tty");
            } finally {
                System.out.print(ANSI_CLEAR);
                System.out.flush();
            }
        }

        private static String runStty(String command) throws IOException {
            Process process = new ProcessBuilder("bash", "-lc", command).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String out = reader.readLine();
                try {
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        String err = errReader.readLine();
                        throw new IOException(err == null ? "stty failed" : err);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while configuring terminal", e);
                }
                return out == null ? "" : out.trim();
            }
        }
    }

    public static final class SelectionResult {
        private final Path lastDirectory;
        private final List<Path> selectedPaths;
        private final boolean cancelled;

        SelectionResult(Path lastDirectory, List<Path> selectedPaths, boolean cancelled) {
            this.lastDirectory = lastDirectory;
            this.selectedPaths = selectedPaths;
            this.cancelled = cancelled;
        }

        public Path lastDirectory() {
            return lastDirectory;
        }

        public List<Path> selectedPaths() {
            return selectedPaths;
        }

        public boolean cancelled() {
            return cancelled;
        }
    }
}
