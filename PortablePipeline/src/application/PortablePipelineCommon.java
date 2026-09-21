package application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

public class PortablePipelineCommon {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // jobs.jsonに後から項目が増えても、古いバージョンで書かれたファイルや
        // 逆に新しいバージョンで書かれたファイルを読めるようにしておく
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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

    /** ジョブ一覧に表示する日時の形式。 */
    private static final DateTimeFormatter JOB_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 現在時刻の日時文字列。 */
    public static String nowStamp() {
        return LocalDateTime.now().format(JOB_STAMP_FORMAT);
    }

    /** ファイルの最終更新時刻の日時文字列。取得できなければ空。 */
    public static String fileStamp(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault())
                    .format(JOB_STAMP_FORMAT);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 日時の記録がないジョブ（この変更より前に投入したもの）について、出力フォルダーの
     * ファイルの更新時刻から投入日時と終了日時を補う。ファイルにも書き戻さず、読み込んだ
     * 一覧の上だけで補完する。
     */
    private static void fillMissingTimestamps(List<JobNode> jobs) {
        boolean needed = false;
        for (JobNode job : jobs) {
            if (job.submittedAt == null || job.submittedAt.isEmpty()
                    || (job.finishedAt == null || job.finishedAt.isEmpty()) && isFinishedStatus(job.status)) {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return;
        }
        Path outputRoot;
        try {
            outputRoot = getBaseDirPath().resolve(loadOrCreateSettings().get("outputfolder"));
        } catch (Exception e) {
            return;
        }
        for (JobNode job : jobs) {
            Path jobDir = outputRoot.resolve(job.id);
            if (job.submittedAt == null || job.submittedAt.isEmpty()) {
                // settings.jsonはジョブを投入したときに書き出している
                job.submittedAt = fileStamp(jobDir.resolve("settings.json"));
            }
            if ((job.finishedAt == null || job.finishedAt.isEmpty()) && isFinishedStatus(job.status)) {
                job.finishedAt = fileStamp(jobDir.resolve("results").resolve("fin_status"));
            }
        }
    }

    /** 実行が終わっている状態か。 */
    public static boolean isFinishedStatus(String status) {
        return "finished".equals(status) || "aborted".equals(status) || "cancelled".equals(status);
    }

    public static List<JobNode> loadJobs() throws IOException {
        Path jobsPath = getJobsPath();
        if (!Files.exists(jobsPath)) {
            return new ArrayList<>();
        }
        List<JobNode> jobs = new ArrayList<>(MAPPER.readValue(jobsPath.toFile(), new TypeReference<List<JobNode>>() {
        }));
        fillMissingTimestamps(jobs);
        return jobs;
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

    /** リモート実行でアップロードした入力ファイルの一覧を記録するファイル名（resultsフォルダーの中）。 */
    public static final String INPUT_LIST_FILE = ".pp-input-files";

    /**
     * ジョブを投入するたびに入力ファイルの記録を作り直すため、古い一覧を消す。
     */
    public static void clearStagedInputs(Path resultsDir) {
        try {
            Files.deleteIfExists(resultsDir.resolve(INPUT_LIST_FILE));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * リモート実行でアップロードした入力ファイルを1行1件で記録する（「input_1/ファイル名」の形）。
     * 結果をダウンロードするときに、自分が送った入力ファイルを取得し直さないために使う。
     * ローカル実行のプリセットではダウンロード自体が起きないので記録しない。
     */
    public static void recordStagedInput(Path resultsDir, String inputId, Path source) throws IOException {
        Files.createDirectories(resultsDir);
        String line = inputId + "/" + source.getFileName() + "\n";
        Files.write(resultsDir.resolve(INPUT_LIST_FILE), line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 記録済みの入力ファイル一覧を読む。一覧がなければ空を返すので、この変更より前に投入した
     * ジョブでも今までどおり（シンボリックリンクの有無で判定）動く。
     */
    public static Set<String> readStagedInputs(Path resultsDir) {
        Set<String> names = new LinkedHashSet<>();
        Path listFile = resultsDir.resolve(INPUT_LIST_FILE);
        if (!Files.isRegularFile(listFile)) {
            return names;
        }
        try {
            for (String line : readLinesLenient(listFile)) {
                if (!line.isEmpty()) {
                    names.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return names;
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

    /**
     * cmd.exeが実行するファイル（wrapper.bat）用の文字コード。cmd.exeはバッチファイルをJava既定のUTF-8では
     * なくコンソールのコードページ（日本語Windowsなら932）で読むので、sun.jnu.encodingが示す実行環境の
     * 文字コードを使い、取得できなければ既定の文字コードにフォールバックする。
     */
    public static Charset batchFileCharset() {
        String encoding = System.getProperty("sun.jnu.encoding");
        if (encoding != null && !encoding.isEmpty()) {
            try {
                return Charset.forName(encoding);
            } catch (Exception e) {
                // 変換できなければ下の既定値を使う
            }
        }
        return Charset.defaultCharset();
    }

    /**
     * cmd.exeが実行するファイル（wrapper.bat）を書き出すためのWriter。実行環境の文字コードで書くが、
     * その文字コードで表現できない文字（英語版Windowsでの日本語コメントなど）があっても例外にはせず
     * 「?」に置き換える。バッチファイル本体は必ずASCIIだけで構成してあるので、置き換えが起きても
     * 動作には影響しない。
     */
    public static Writer batchFileWriter(Path path) throws IOException {
        CharsetEncoder encoder = batchFileCharset().newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), encoder));
    }

    /**
     * バイナリディレクトリ配下のパス（アイコンなど）に対するfile:URLを作る。File.toURI()がパーセント
     * エンコードするので、スペースや非ASCII文字が入っていても安全。
     */
    public static String fileUrl(String dir, String relativePath) {
        return new File(dir, relativePath).toURI().toString();
    }

    /**
     * テキストファイルをUTF-8として読む。不正なバイト列があっても例外にしない。log.txtはパイプラインの
     * 出力をそのまま記録したもので、コンテナ内のツールが別の文字コードで出力することがあるため。
     */
    public static List<String> readLinesLenient(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).lines().collect(Collectors.toList());
    }

    public static void recursiveFolderDownload(String sourcePath, String destinationPath, ChannelSftp channelSftp)
            throws SftpException {
        // 自分がアップロードした入力ファイルは取得し直さない（結果フォルダーの一覧を先に読んでおく）
        recursiveFolderDownload(sourcePath, destinationPath, channelSftp,
                readStagedInputs(Path.of(destinationPath)), "");
    }

    @SuppressWarnings("unchecked")
    private static void recursiveFolderDownload(String sourcePath, String destinationPath, ChannelSftp channelSftp,
            Set<String> stagedInputs, String relativePrefix) throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath);
        if (!(new File(destinationPath)).exists()) {
            (new File(destinationPath)).mkdirs();
        }

        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            Path localPath = Path.of(destinationPath, item.getFilename());
            String relativePath = relativePrefix + item.getFilename();
            if (!item.getAttrs().isDir()) {
                if (stagedInputs.contains(relativePath)) {
                    continue;
                }
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
                                    channelSftp,
                                    stagedInputs,
                                    relativePath + "/");
                        }
                    }
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                localPath.toFile().mkdirs();
                recursiveFolderDownload(sourcePath + "/" + item.getFilename(), localPath.toString(), channelSftp,
                        stagedInputs, relativePath + "/");
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
