package application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class PortablePipelineCLI {

    private static Path lastBrowseDir = Path.of(".").toAbsolutePath().normalize();

    public static void main(String[] args) {
        try {
            PortablePipelineCommon.initSystemProperties();
            if (args.length == 0) {
                runInteractive();
                return;
            }

            switch (args[0]) {
            case "help":
            case "--help":
            case "-h":
                printHelp();
                break;
            case "list-scripts":
                listScripts(joinArgs(args, 1));
                break;
            case "describe":
                ensureArgs(args, 2, "describe <script>");
                describe(resolveScript(joinArgs(args, 1)));
                break;
            case "run":
                handleRun(Arrays.copyOfRange(args, 1, args.length));
                break;
            case "jobs":
                printJobs();
                break;
            case "log":
                handleLog(Arrays.copyOfRange(args, 1, args.length));
                break;
            case "stop":
                ensureArgs(args, 2, "stop <jobId>");
                PortablePipelineJobManager.stopJob(args[1]);
                System.out.println("Stopped job " + args[1]);
                break;
            case "delete-job":
                ensureArgs(args, 2, "delete-job <jobId>");
                PortablePipelineJobManager.deleteJob(args[1]);
                System.out.println("Deleted job " + args[1]);
                break;
            case "settings":
                showSettings();
                break;
            case "configure":
                configureInteractive(new Scanner(System.in));
                break;
            default:
                System.err.println("Unknown command: " + args[0]);
                printHelp();
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void runInteractive() throws Exception {
        Scanner scanner = new Scanner(System.in);
        printBanner();
        printHelp();
        while (true) {
            System.out.print("pp-cui> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            String command = tokens[0].toLowerCase(Locale.ROOT);
            try {
                switch (command) {
                case "help":
                    printHelp();
                    break;
                case "scripts":
                    listScripts(line.length() > command.length() ? line.substring(command.length()).trim() : "");
                    break;
                case "describe":
                    if (tokens.length < 2) {
                        System.out.println("Usage: describe <script>");
                    } else {
                        describe(resolveScript(line.substring(command.length()).trim()));
                    }
                    break;
                case "run":
                    if (tokens.length == 1) {
                        runGuided(scanner);
                    } else {
                        handleRun(Arrays.copyOfRange(tokens, 1, tokens.length));
                    }
                    break;
                case "jobs":
                    printJobs();
                    break;
                case "log":
                    if (tokens.length < 2) {
                        System.out.println("Usage: log <jobId> [follow]");
                    } else {
                        boolean follow = tokens.length >= 3 && "follow".equalsIgnoreCase(tokens[2]);
                        PortablePipelineJobManager.printLog(tokens[1], follow);
                    }
                    break;
                case "stop":
                    if (tokens.length < 2) {
                        System.out.println("Usage: stop <jobId>");
                    } else {
                        PortablePipelineJobManager.stopJob(tokens[1]);
                        System.out.println("Stopped job " + tokens[1]);
                    }
                    break;
                case "delete":
                case "delete-job":
                    if (tokens.length < 2) {
                        System.out.println("Usage: delete <jobId>");
                    } else {
                        PortablePipelineJobManager.deleteJob(tokens[1]);
                        System.out.println("Deleted job " + tokens[1]);
                    }
                    break;
                case "settings":
                    showSettings();
                    break;
                case "configure":
                    configureInteractive(scanner);
                    break;
                case "exit":
                case "quit":
                    return;
                default:
                    System.out.println("Unknown command. Type `help`.");
                    break;
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void runGuided(Scanner scanner) throws Exception {
        List<PPScript> scripts = PPScriptParser.loadScripts(PortablePipelineCommon.getScriptDir());
        System.out.print("Search keyword (blank for all): ");
        String keyword = scanner.nextLine().trim();
        List<PPScript> filtered = new ArrayList<>();
        for (PPScript script : scripts) {
            if (keyword.isEmpty() || matchesScript(script, keyword)) {
                filtered.add(script);
            }
        }
        if (filtered.isEmpty()) {
            System.out.println("No scripts matched.");
            return;
        }
        PPScript script;
        if (TerminalFilePicker.isAvailable()) {
            script = TerminalFilePicker.pickScript("Portable Pipeline Script Picker", filtered);
            if (script == null) {
                return;
            }
        } else {
            printScriptTable(filtered);
            System.out.print("Select script number: ");
            String selector = scanner.nextLine().trim();
            if (selector.isEmpty()) {
                return;
            }
            script = resolveScript(selector, filtered);
        }
        describe(script);

        LinkedHashMap<String, List<String>> inputs = new LinkedHashMap<>();
        for (InputItem item : script.inputs) {
            List<String> values = promptForInputSelection(scanner, item);
            boolean required = !item.num.contains("option");
            if (required && values.isEmpty()) {
                System.out.println(item.desc + " is required.");
                return;
            }
            inputs.put(item.id, values);
        }

        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        for (OptionItem item : script.options) {
            System.out.print(item.id + " [" + item.desc + "] default=" + item.defaultopt + ": ");
            String raw = scanner.nextLine();
            options.put(item.id, raw.isEmpty() ? item.defaultopt : raw);
        }

        System.out.print("Run this job? [y/N]: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y")) {
            return;
        }

        JobNode job = PortablePipelineJobManager.submitJob(script, inputs, options);
        System.out.println("Submitted job " + job.id);
        System.out.print("Follow log now? [Y/n]: ");
        String follow = scanner.nextLine().trim();
        if (!follow.equalsIgnoreCase("n")) {
            PortablePipelineJobManager.printLog(job.id, true);
        }
    }

    private static void handleRun(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: run <script> [--input id=path1,path2] [--opt id=value] [--wait]");
        }

        String selector = null;
        boolean wait = false;
        LinkedHashMap<String, List<String>> inputs = new LinkedHashMap<>();
        LinkedHashMap<String, String> options = new LinkedHashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--wait".equals(arg)) {
                wait = true;
                continue;
            }
            if ("--input".equals(arg)) {
                i++;
                ensureIndex(args, i, "--input requires id=value");
                putInputKeyValue(inputs, args[i]);
                continue;
            }
            if (arg.startsWith("--input=")) {
                putInputKeyValue(inputs, arg.substring("--input=".length()));
                continue;
            }
            if ("--opt".equals(arg)) {
                i++;
                ensureIndex(args, i, "--opt requires id=value");
                putOptionKeyValue(options, args[i]);
                continue;
            }
            if (arg.startsWith("--opt=")) {
                putOptionKeyValue(options, arg.substring("--opt=".length()));
                continue;
            }
            if (selector == null) {
                selector = arg;
                continue;
            }
            throw new IllegalArgumentException("Unexpected argument: " + arg);
        }

        if (selector == null) {
            throw new IllegalArgumentException("Script name is required.");
        }

        PPScript script = resolveScript(selector);
        for (OptionItem item : script.options) {
            options.putIfAbsent(item.id, item.defaultopt);
        }

        JobNode job = PortablePipelineJobManager.submitJob(script, inputs, options);
        System.out.println("Submitted job " + job.id);
        System.out.println("Status: " + job.status);
        if (wait) {
            PortablePipelineJobManager.printLog(job.id, true);
            JobNode refreshed = PortablePipelineJobManager.refreshJob(job.id, false);
            System.out.println("Final status: " + refreshed.status);
        }
    }

    private static void handleLog(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: log <jobId> [--follow]");
        }
        boolean follow = args.length >= 2 && "--follow".equals(args[1]);
        PortablePipelineJobManager.printLog(args[0], follow);
    }

    private static void listScripts(String keyword) throws IOException {
        List<PPScript> scripts = PPScriptParser.loadScripts(PortablePipelineCommon.getScriptDir());
        List<PPScript> filtered = new ArrayList<>();
        for (PPScript script : scripts) {
            if (keyword == null || keyword.isBlank() || matchesScript(script, keyword)) {
                filtered.add(script);
            }
        }
        printScriptTable(filtered);
    }

    private static void describe(PPScript script) {
        System.out.println(script.filename);
        if (!script.explanation.isBlank()) {
            System.out.println(script.explanation);
        }
        if (!script.inputs.isEmpty()) {
            System.out.println();
            System.out.println("Inputs:");
            for (InputItem item : script.inputs) {
                System.out.println("  " + item.id + " : " + item.desc + " [" + item.num + "] " + item.filetype);
            }
        }
        if (!script.options.isEmpty()) {
            System.out.println();
            System.out.println("Options:");
            for (OptionItem item : script.options) {
                System.out.println("  " + item.id + " : " + item.desc + " = " + item.defaultopt);
                if (!item.longdesc.isBlank()) {
                    System.out.println(item.longdesc);
                }
            }
        }
        if (!script.runCommand.isBlank()) {
            System.out.println();
            System.out.println("Command template:");
            System.out.println("  " + script.runCommand);
        }
    }

    private static void printJobs() throws Exception {
        List<JobNode> jobs = PortablePipelineJobManager.refreshJobs(false);
        if (jobs.isEmpty()) {
            System.out.println("No jobs.");
            return;
        }
        for (JobNode job : jobs) {
            System.out.println(job.id + "\t" + job.status + "\t" + job.desc);
        }
    }

    private static void showSettings() throws IOException {
        Map<String, String> settings = PortablePipelineCommon.normalizeSettingsForPreset(
                PortablePipelineCommon.loadOrCreateSettings());
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
    }

    private static void configureInteractive(Scanner scanner) throws IOException {
        Map<String, String> settings = PortablePipelineCommon.normalizeSettingsForPreset(
                PortablePipelineCommon.loadOrCreateSettings());
        List<String> presets = List.of("ssh", "ssh (SGE)", "ddbj", "shirokane", "WSL", "Mac", "Linux", "Linux (SGE)");

        System.out.println("Available presets: " + String.join(", ", presets));
        settings.put("preset", prompt(scanner, "preset", settings.get("preset")));
        settings.put("outputfolder", prompt(scanner, "outputfolder", settings.get("outputfolder")));
        settings.put("scriptfolder", prompt(scanner, "scriptfolder", settings.get("scriptfolder")));

        String preset = settings.get("preset");
        if ("WSL".equals(preset)) {
            settings.put("user", prompt(scanner, "user", settings.get("user")));
            settings.put("password", prompt(scanner, "password", settings.get("password")));
        } else if ("Mac".equals(preset)) {
            // no additional prompts
        } else if ("Linux".equals(preset) || "Linux (SGE)".equals(preset)) {
            settings.put("imagefolder", prompt(scanner, "imagefolder", settings.get("imagefolder")));
        } else {
            settings.put("hostname", prompt(scanner, "hostname", settings.get("hostname")));
            settings.put("port", prompt(scanner, "port", settings.get("port")));
            settings.put("user", prompt(scanner, "user", settings.get("user")));
            settings.put("password", prompt(scanner, "password", settings.get("password")));
            settings.put("privatekey", prompt(scanner, "privatekey", settings.get("privatekey")));
            settings.put("workfolder", prompt(scanner, "workfolder", settings.get("workfolder")));
            settings.put("imagefolder", prompt(scanner, "imagefolder", settings.get("imagefolder")));
            settings.put("checkdelete", prompt(scanner, "checkdelete(true/false)", settings.get("checkdelete")));
        }

        Map<String, String> normalized = PortablePipelineCommon.normalizeSettingsForPreset(settings);
        PortablePipelineCommon.saveSettings(normalized);
        System.out.println("Saved settings.");
    }

    private static PPScript resolveScript(String selector) throws IOException {
        List<PPScript> scripts = PPScriptParser.loadScripts(PortablePipelineCommon.getScriptDir());
        return resolveScript(selector, scripts);
    }

    private static PPScript resolveScript(String selector, List<PPScript> scripts) {
        if (selector.matches("\\d+")) {
            int index = Integer.parseInt(selector);
            if (index < 1 || index > scripts.size()) {
                throw new IllegalArgumentException("Script index out of range: " + selector);
            }
            return scripts.get(index - 1);
        }

        for (PPScript script : scripts) {
            if (script.filename.equals(selector)) {
                return script;
            }
        }
        for (PPScript script : scripts) {
            if (script.filename.equalsIgnoreCase(selector)) {
                return script;
            }
        }
        List<PPScript> matched = new ArrayList<>();
        for (PPScript script : scripts) {
            if (matchesScript(script, selector)) {
                matched.add(script);
            }
        }
        if (matched.size() == 1) {
            return matched.get(0);
        }
        if (matched.isEmpty()) {
            throw new IllegalArgumentException("No script matched: " + selector);
        }
        throw new IllegalArgumentException("Multiple scripts matched: " + selector);
    }

    private static boolean matchesScript(PPScript script, String keyword) {
        String upper = keyword.toUpperCase(Locale.ROOT);
        return script.filename.toUpperCase(Locale.ROOT).contains(upper)
                || script.explanation.toUpperCase(Locale.ROOT).contains(upper);
    }

    private static void printScriptTable(List<PPScript> scripts) {
        if (scripts.isEmpty()) {
            System.out.println("No scripts.");
            return;
        }
        int index = 1;
        for (PPScript script : scripts) {
            System.out.println(index + "\t" + script.filename + "\t" + script.getSummary());
            index++;
        }
    }

    private static void printBanner() throws IOException {
        Path scriptDir = PortablePipelineCommon.getScriptDir();
        System.out.println("Portable Pipeline CUI");
        System.out.println("scripts=" + scriptDir);
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  list-scripts [keyword]                                List available scripts. Filter by keyword if given.");
        System.out.println("  describe <script>                                     Show script inputs, options, and command template.");
        System.out.println("  run <script> [--input id=path1,path2] [--opt id=value] [--wait]");
        System.out.println("                                                         Run a script directly from the command line.");
        System.out.println("  jobs                                                  Show saved jobs and refresh running status.");
        System.out.println("  log <jobId> [--follow]                                Show a job log. Use --follow to keep tailing.");
        System.out.println("  stop <jobId>                                          Stop a running job.");
        System.out.println("  delete-job <jobId>                                    Remove a finished/aborted/cancelled job record.");
        System.out.println("  settings                                              Show current settings.");
        System.out.println("  configure                                             Edit settings interactively.");
        System.out.println();
        System.out.println("Interactive mode:");
        System.out.println("  run                                                   Start guided script selection and input picking.");
        System.out.println("  scripts [keyword]                                     List scripts inside the interactive shell.");
        System.out.println("  describe <script>                                     Show script details inside the interactive shell.");
        System.out.println("  jobs                                                  Show job list.");
        System.out.println("  log <jobId> [follow]                                  Show a job log. Add follow to keep tailing.");
        System.out.println("  stop <jobId>                                          Stop a running job.");
        System.out.println("  delete <jobId>                                        Delete a non-running job.");
        System.out.println("  settings                                              Show current settings.");
        System.out.println("  configure                                             Edit settings interactively.");
        System.out.println("  exit                                                  Leave the interactive shell.");
    }

    private static List<String> promptForInputSelection(Scanner scanner, InputItem item) throws Exception {
        if (TerminalFilePicker.isAvailable()) {
            TerminalFilePicker.SelectionResult result = TerminalFilePicker.pick(
                    "Portable Pipeline File Picker",
                    item,
                    lastBrowseDir);
            lastBrowseDir = result.lastDirectory();
            if (result.cancelled()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (Path path : result.selectedPaths()) {
                values.add(path.toString());
            }
            return values;
        }

        boolean required = !item.num.contains("option");
        boolean multiple = item.num.contains("directory");
        String defaultHint = multiple ? "comma-separated paths" : "path";
        System.out.print((required ? "* " : "  ") + item.id + " [" + item.desc + ", " + defaultHint + "]: ");
        String raw = scanner.nextLine().trim();
        return splitCsv(raw);
    }

    private static void putInputKeyValue(Map<String, List<String>> target, String expr) {
        int index = expr.indexOf('=');
        if (index <= 0) {
            throw new IllegalArgumentException("Expected id=value but got: " + expr);
        }
        String key = expr.substring(0, index);
        String value = expr.substring(index + 1);
        target.put(key, splitCsv(value));
    }

    private static void putOptionKeyValue(Map<String, String> target, String expr) {
        int index = expr.indexOf('=');
        if (index <= 0) {
            throw new IllegalArgumentException("Expected id=value but got: " + expr);
        }
        target.put(expr.substring(0, index), expr.substring(index + 1));
    }

    private static List<String> splitCsv(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String value : raw.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String prompt(Scanner scanner, String label, String currentValue) {
        System.out.print(label + " [" + currentValue + "]: ");
        String value = scanner.nextLine();
        return value.isEmpty() ? currentValue : value;
    }

    private static String joinArgs(String[] args, int start) {
        if (args.length <= start) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static void ensureArgs(String[] args, int min, String usage) {
        if (args.length < min) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void ensureIndex(String[] args, int index, String message) {
        if (index >= args.length) {
            throw new IllegalArgumentException(message);
        }
    }
}
