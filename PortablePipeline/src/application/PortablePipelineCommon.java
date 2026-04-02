package application;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

public class PortablePipelineCommon {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> SETTING_KEYS = Arrays.asList(
            "hostname",
            "port",
            "user",
            "password",
            "privatekey",
            "workfolder",
            "preset",
            "outputfolder",
            "scriptfolder",
            "imagefolder",
            "checkdelete");

    private PortablePipelineCommon() {
    }

    public static void initSystemProperties() {
        String ppBinDir = System.getProperty("PP_BIN_DIR");
        if (ppBinDir == null || ppBinDir.isEmpty()) {
            System.setProperty("PP_BIN_DIR", ".");
        }

        String ppOutDir = System.getProperty("PP_OUT_DIR");
        if (ppOutDir == null || ppOutDir.isEmpty()) {
            System.setProperty("PP_OUT_DIR", ".");
        }
    }

    public static Path getBaseDirPath() {
        initSystemProperties();
        return Path.of(PPSetting.getBaseDir()).toAbsolutePath().normalize();
    }

    public static Path getBinDirPath() {
        initSystemProperties();
        return Path.of(System.getProperty("PP_BIN_DIR")).toAbsolutePath().normalize();
    }

    public static Path getScriptDir() throws IOException {
        Map<String, String> settings = loadOrCreateSettings();
        return getBinDirPath().resolve(settings.get("scriptfolder")).normalize();
    }

    public static Path getSettingsPath() {
        return getBaseDirPath().resolve("settings.json");
    }

    public static Path getJobsPath() {
        return getBaseDirPath().resolve("jobs.json");
    }

    public static Map<String, String> loadOrCreateSettings() throws IOException {
        Path settingsPath = getSettingsPath();
        if (!Files.exists(settingsPath)) {
            saveSettings(createDefaultSettings());
        }

        JsonNode node = MAPPER.readTree(settingsPath.toFile());
        LinkedHashMap<String, String> settings = new LinkedHashMap<>();
        for (String key : SETTING_KEYS) {
            settings.put(key, node.path(key).asText(""));
        }

        if (settings.get("outputfolder").isEmpty()) {
            settings.put("outputfolder", "output");
        }
        if (settings.get("scriptfolder").isEmpty()) {
            settings.put("scriptfolder", "scripts");
        }
        if (settings.get("preset").isEmpty()) {
            settings.put("preset", defaultPreset());
        }
        if (settings.get("imagefolder").isEmpty() && settings.get("preset").startsWith("Linux")) {
            settings.put("imagefolder", "$HOME/img");
        }

        return settings;
    }

    public static void saveSettings(Map<String, String> settings) throws IOException {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (String key : SETTING_KEYS) {
            normalized.put(key, settings.getOrDefault(key, ""));
        }
        ObjectNode node = MAPPER.createObjectNode();
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            node.put(entry.getKey(), entry.getValue());
        }
        Files.writeString(
                getSettingsPath(),
                MAPPER.enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(node),
                StandardCharsets.UTF_8);
    }

    public static Map<String, String> createDefaultSettings() {
        LinkedHashMap<String, String> settings = new LinkedHashMap<>();
        for (String key : SETTING_KEYS) {
            settings.put(key, "");
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            settings.put("user", "your_wsl_user_name");
            settings.put("password", "your_wsl_password");
            settings.put("preset", "WSL");
            settings.put("outputfolder", "output");
            settings.put("scriptfolder", "scripts");
            settings.put("checkdelete", "false");
        } else if (osName.startsWith("mac")) {
            settings.put("preset", "Mac");
            settings.put("outputfolder", "output");
            settings.put("scriptfolder", "scripts");
            settings.put("checkdelete", "false");
        } else if (osName.startsWith("linux")) {
            settings.put("imagefolder", "$HOME/img");
            settings.put("preset", "Linux");
            settings.put("outputfolder", "output");
            settings.put("scriptfolder", "scripts");
            settings.put("checkdelete", "false");
        } else {
            settings.put("hostname", "m208.s");
            settings.put("port", "22");
            settings.put("user", "user2");
            settings.put("password", "user2");
            settings.put("workfolder", "work");
            settings.put("preset", "ssh");
            settings.put("outputfolder", "output");
            settings.put("scriptfolder", "scripts");
            settings.put("imagefolder", "$HOME/img");
            settings.put("checkdelete", "true");
        }
        return settings;
    }

    public static String defaultPreset() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            return "WSL";
        }
        if (osName.startsWith("mac")) {
            return "Mac";
        }
        if (osName.startsWith("linux")) {
            return "Linux";
        }
        return "ssh";
    }

    public static Map<String, String> normalizeSettingsForPreset(Map<String, String> input) {
        LinkedHashMap<String, String> settings = new LinkedHashMap<>();
        for (String key : SETTING_KEYS) {
            settings.put(key, input.getOrDefault(key, ""));
        }

        String preset = settings.getOrDefault("preset", defaultPreset());
        switch (preset) {
        case "WSL":
            settings.put("hostname", "");
            settings.put("port", "");
            settings.put("privatekey", "");
            settings.put("imagefolder", "");
            settings.put("workfolder", "");
            settings.put("checkdelete", "false");
            break;
        case "Mac":
            settings.put("hostname", "");
            settings.put("port", "");
            settings.put("privatekey", "");
            settings.put("user", "");
            settings.put("password", "");
            settings.put("imagefolder", "");
            settings.put("workfolder", "");
            settings.put("checkdelete", "false");
            break;
        case "Linux":
            settings.put("hostname", "");
            settings.put("port", "");
            settings.put("privatekey", "");
            settings.put("user", "");
            settings.put("password", "");
            settings.put("workfolder", "");
            settings.put("checkdelete", "false");
            if (settings.get("imagefolder").isEmpty()) {
                settings.put("imagefolder", "$HOME/img");
            }
            break;
        case "Linux (SGE)":
            settings.put("hostname", "");
            settings.put("port", "");
            settings.put("privatekey", "");
            settings.put("user", "");
            settings.put("password", "");
            settings.put("workfolder", "");
            settings.put("checkdelete", "false");
            if (settings.get("imagefolder").isEmpty()) {
                settings.put("imagefolder", "$HOME/img");
            }
            break;
        case "ddbj":
            if (settings.get("hostname").isEmpty()) {
                settings.put("hostname", "gw.ddbj.nig.ac.jp");
            }
            if (settings.get("port").isEmpty()) {
                settings.put("port", "22");
            }
            if (settings.get("workfolder").isEmpty()) {
                settings.put("workfolder", "work");
            }
            if (settings.get("imagefolder").isEmpty()) {
                settings.put("imagefolder", "$HOME/img");
            }
            if (settings.get("checkdelete").isEmpty()) {
                settings.put("checkdelete", "true");
            }
            break;
        case "shirokane":
            if (settings.get("hostname").isEmpty()) {
                settings.put("hostname", "slogin.hgc.jp");
            }
            if (settings.get("port").isEmpty()) {
                settings.put("port", "22");
            }
            if (settings.get("workfolder").isEmpty()) {
                settings.put("workfolder", "work");
            }
            if (settings.get("imagefolder").isEmpty()) {
                settings.put("imagefolder", "$HOME/img");
            }
            if (settings.get("checkdelete").isEmpty()) {
                settings.put("checkdelete", "true");
            }
            break;
        case "ssh":
        case "ssh (SGE)":
            if (settings.get("port").isEmpty()) {
                settings.put("port", "22");
            }
            if (settings.get("workfolder").isEmpty()) {
                settings.put("workfolder", "work");
            }
            if (settings.get("imagefolder").isEmpty()) {
                settings.put("imagefolder", "$HOME/img");
            }
            if (settings.get("checkdelete").isEmpty()) {
                settings.put("checkdelete", "true");
            }
            break;
        default:
            break;
        }

        if (settings.get("outputfolder").isEmpty()) {
            settings.put("outputfolder", "output");
        }
        if (settings.get("scriptfolder").isEmpty()) {
            settings.put("scriptfolder", "scripts");
        }

        return settings;
    }

    public static List<JobNode> loadJobs() throws IOException {
        Path jobsPath = getJobsPath();
        if (!Files.exists(jobsPath)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(MAPPER.readValue(jobsPath.toFile(), new TypeReference<List<JobNode>>() {
        }));
    }

    public static void saveJobs(List<JobNode> jobs) throws IOException {
        Files.writeString(
                getJobsPath(),
                MAPPER.enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jobs),
                StandardCharsets.UTF_8);
    }

    public static int nextJobId(List<JobNode> jobs, Path outputRoot) throws IOException {
        int maxId = 0;
        for (JobNode job : jobs) {
            maxId = Math.max(maxId, parseIntSafe(job.id));
        }

        if (Files.exists(outputRoot)) {
            try (Stream<Path> stream = Files.list(outputRoot)) {
                for (Path path : stream.toList()) {
                    maxId = Math.max(maxId, parseIntSafe(path.getFileName().toString()));
                }
            }
        }
        return maxId + 1;
    }

    public static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void upsertJob(List<JobNode> jobs, JobNode newJob) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).id.equals(newJob.id)) {
                jobs.set(i, newJob);
                return;
            }
        }
        jobs.add(newJob);
    }

    public static Path resolveJobDir(String jobId) throws IOException {
        Map<String, String> settings = loadOrCreateSettings();
        Path candidate = getBaseDirPath().resolve(settings.get("outputfolder")).resolve(jobId);
        if (Files.exists(candidate)) {
            return candidate;
        }
        try (Stream<Path> stream = Files.find(
                getBaseDirPath(),
                3,
                (path, attrs) -> attrs.isRegularFile()
                        && path.getFileName().toString().equals("settings.json")
                        && path.getParent() != null
                        && path.getParent().getFileName().toString().equals(jobId))) {
            return stream.findFirst().map(Path::getParent).orElse(candidate);
        }
    }

    public static JsonNode loadJobSettingsNode(String jobId) throws IOException {
        Path jobDir = resolveJobDir(jobId);
        Path settingsPath = jobDir.resolve("settings.json");
        return MAPPER.readTree(settingsPath.toFile());
    }

    public static Path getJobResultsDir(String jobId) throws IOException {
        return resolveJobDir(jobId).resolve("results");
    }

    public static String readAll(Path path) throws IOException {
        return Files.lines(path, Charset.forName("UTF-8")).collect(Collectors.joining("\n"));
    }

    public static void searchChildScripts(String scriptFolderPath, String scriptPath, List<String> childScripts)
            throws IOException {
        String scriptContent = readAll(Path.of(scriptFolderPath, scriptPath));
        String[] tokens = scriptContent.replaceAll("[ \t']", "\n").split("\n");
        for (String token : tokens) {
            if (token.startsWith("\"$scriptdir\"/")) {
                String child = token.substring(13);
                if (!childScripts.contains(child)) {
                    childScripts.add(child);
                    searchChildScripts(scriptFolderPath, child, childScripts);
                }
            }
        }

        String[] lines = scriptContent.split("\n");
        for (String line : lines) {
            extractChildScript(line, "PP_DO_CHILD", childScripts, scriptFolderPath);
            extractChildScript(line, "PP_ENV_CHILD", childScripts, scriptFolderPath);
        }
    }

    private static void extractChildScript(String line, String marker, List<String> childScripts, String scriptFolderPath)
            throws IOException {
        int index = line.indexOf(marker);
        if (index == -1) {
            return;
        }
        String result = line.substring(index + marker.length()).replaceAll("^\\s+", "");
        int spaceIndex = result.indexOf(' ');
        if (spaceIndex != -1) {
            result = result.substring(0, spaceIndex);
        }
        if (!result.isEmpty() && !childScripts.contains(result)) {
            childScripts.add(result);
            searchChildScripts(scriptFolderPath, result, childScripts);
        }
    }

    public static void copyFileWithTimestamp(Path source, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        dest.toFile().setLastModified(Calendar.getInstance().getTimeInMillis());
    }

    public static void mkSymLinkOrCopy(String preset, Path target, Path linkDir) throws IOException {
        Files.createDirectories(linkDir);
        Path linkPath = linkDir.resolve(target.getFileName());
        if (Files.exists(linkPath)) {
            return;
        }
        try {
            if (!"WSL".equals(preset) && !"Mac".equals(preset)) {
                Files.createSymbolicLink(linkPath, target.toAbsolutePath());
            } else {
                Files.createLink(linkPath, target.toAbsolutePath());
            }
        } catch (Exception e) {
            Files.copy(target, linkPath, StandardCopyOption.REPLACE_EXISTING);
            linkPath.toFile().setLastModified(Calendar.getInstance().getTimeInMillis());
        }
    }

    public static boolean isLocalPreset(String preset) {
        return "WSL".equals(preset)
                || "Mac".equals(preset)
                || "Linux".equals(preset)
                || "Linux (SGE)".equals(preset);
    }

    public static boolean usesTwoStepSsh(String preset) {
        return "ddbj".equals(preset);
    }

    public static boolean usesGridEngine(String preset) {
        return "ssh (SGE)".equals(preset) || "shirokane".equals(preset) || "ddbj".equals(preset);
    }

    public static boolean usesDirectPid(String preset) {
        return "ssh".equals(preset) || isLocalPreset(preset);
    }

    public static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    @SuppressWarnings("unchecked")
    public static void recursiveFolderDownload(String sourcePath, String destinationPath, ChannelSftp channelSftp)
            throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath);
        if (!(new File(destinationPath)).exists()) {
            (new File(destinationPath)).mkdirs();
        }

        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            Path localPath = Path.of(destinationPath, item.getFilename());
            if (!item.getAttrs().isDir()) {
                if (!Files.exists(localPath)
                        || (item.getAttrs().getMTime() > Long.valueOf(localPath.toFile().lastModified() / 1000L)
                                .intValue())
                        || (item.getAttrs().getSize() > localPath.toFile().length())) {
                    if (!Files.isSymbolicLink(localPath)) {
                        if (!channelSftp.stat(channelSftp.realpath(sourcePath + "/" + item.getFilename())).isDir()) {
                            channelSftp.get(
                                    channelSftp.realpath(sourcePath + "/" + item.getFilename()),
                                    localPath.toString());
                        } else {
                            localPath.toFile().mkdirs();
                            recursiveFolderDownload(
                                    sourcePath + "/" + item.getFilename(),
                                    localPath.toString(),
                                    channelSftp);
                        }
                    }
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                localPath.toFile().mkdirs();
                recursiveFolderDownload(sourcePath + "/" + item.getFilename(), localPath.toString(), channelSftp);
            }
        }
    }

    public static void lsFolderRemove(String dir, ChannelSftp channelSftp) {
        try {
            Vector<ChannelSftp.LsEntry> list = channelSftp.ls(dir);
            for (ChannelSftp.LsEntry item : list) {
                if (!item.getAttrs().isDir()) {
                    channelSftp.rm(dir + "/" + item.getFilename());
                } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                    try {
                        channelSftp.rmdir(dir + "/" + item.getFilename());
                    } catch (Exception e) {
                        lsFolderRemove(dir + "/" + item.getFilename(), channelSftp);
                    }
                }
            }
            channelSftp.rmdir(dir);
        } catch (SftpException e) {
            System.out.println("Removing " + dir + " failed. It may be already deleted.");
        }
    }

    public static boolean containsUnicode(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);

            if (Character.UnicodeBlock.HIRAGANA.equals(unicodeBlock)) {
                return true;
            }
            if (Character.UnicodeBlock.KATAKANA.equals(unicodeBlock)) {
                return true;
            }
            if (Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS.equals(unicodeBlock)) {
                return true;
            }
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(unicodeBlock)) {
                return true;
            }
            if (Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(unicodeBlock)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteDirectoryRecursively(Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(target)) {
            for (Path path : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static List<String> readLinesIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }
}
