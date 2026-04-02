package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class PortablePipelineJobManager {

    private PortablePipelineJobManager() {
    }

    public static JobNode submitJob(
            PPScript script,
            Map<String, List<String>> inputValues,
            Map<String, String> optionValues) throws Exception {
        Map<String, String> settings = PortablePipelineCommon.normalizeSettingsForPreset(
                PortablePipelineCommon.loadOrCreateSettings());
        String preset = settings.get("preset");
        String userDir = System.getProperty("user.dir", "");
        if ((PortablePipelineCommon.containsUnicode(userDir) || userDir.contains(" "))
                && ("WSL".equals(preset) || "Mac".equals(preset))) {
            throw new IllegalStateException(
                    "Don't put Portable Pipeline in a folder which contains Unicode or space characters.");
        }

        validateInputs(script, inputValues);

        List<JobNode> jobs = PortablePipelineCommon.loadJobs();
        Path outputRoot = PortablePipelineCommon.getBaseDirPath().resolve(settings.get("outputfolder"));
        Files.createDirectories(outputRoot);
        int workId = PortablePipelineCommon.nextJobId(jobs, outputRoot);
        String jobId = String.valueOf(workId);

        JobNode preparingJob = new JobNode(jobId, "preparing", script.filename);
        PortablePipelineCommon.upsertJob(jobs, preparingJob);
        PortablePipelineCommon.saveJobs(jobs);

        Path jobDir = outputRoot.resolve(jobId);
        Path resultsDir = jobDir.resolve("results");
        Files.createDirectories(resultsDir);
        writeJobSettings(settings, jobDir.resolve("settings.json"));

        String jobDesc = buildJobDescription(script, inputValues, optionValues);
        String scriptFolderPath = PortablePipelineCommon.getBinDirPath().resolve(settings.get("scriptfolder")).toString();
        String workDir = settings.get("workfolder") + "/" + jobId;
        String numCpu = optionValueByDescription(script, optionValues, "cpu threads", "1");
        String numMem = optionValueByDescription(script, optionValues, "memory limit (GB)", "8");
        String runCmd = buildRunCommand(script, inputValues, optionValues);

        Session sshSession = null;
        Channel sftpChannel = null;
        ChannelSftp channelSftp = null;

        try {
            if (!PortablePipelineCommon.isLocalPreset(preset)) {
                JSch jsch = new JSch();
                Path tempPrivateKeyPath = jobDir.resolve("id_rsa");
                if (!settings.get("privatekey").isEmpty()) {
                    Files.writeString(tempPrivateKeyPath, settings.get("privatekey"), StandardCharsets.UTF_8);
                    jsch.addIdentity(tempPrivateKeyPath.toString(), settings.get("password"));
                }
                sshSession = jsch.getSession(
                        settings.get("user"),
                        settings.get("hostname"),
                        Integer.parseInt(settings.get("port")));
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.setPassword(settings.get("password"));
                sshSession.connect();

                sftpChannel = sshSession.openChannel("sftp");
                sftpChannel.setInputStream(System.in);
                sftpChannel.setOutputStream(System.out);
                sftpChannel.connect();
                channelSftp = (ChannelSftp) sftpChannel;
                ensureRemoteDirectories(channelSftp, workDir);
            }

            List<String> childScripts = new ArrayList<>();
            PortablePipelineCommon.searchChildScripts(scriptFolderPath, script.filename, childScripts);

            transferScriptBundle(script.filename, childScripts, settings.get("scriptfolder"), resultsDir, workDir, channelSftp);
            stageInputs(script, inputValues, preset, resultsDir, workDir, channelSftp);

            String commandString = createWrapperAndLaunchCommand(
                    settings,
                    script.filename,
                    resultsDir,
                    workDir,
                    runCmd,
                    numCpu,
                    numMem);

            if (channelSftp != null) {
                channelSftp.put(resultsDir.resolve("wrapper.sh").toString(), workDir);
            }

            if (PortablePipelineCommon.isLocalPreset(preset)) {
                launchLocalJob(commandString, preset, resultsDir);
            } else {
                if (PortablePipelineCommon.usesTwoStepSsh(preset)) {
                    ConnectSsh.getSshCmdResult2StepSession(
                            settings.get("hostname"),
                            Integer.parseInt(settings.get("port")),
                            settings.get("user"),
                            settings.get("password"),
                            settings.get("privatekey"),
                            jobDir.resolve("id_rsa").toString(),
                            commandString);
                } else {
                    ConnectSsh.getSshCmdResult(
                            settings.get("hostname"),
                            Integer.parseInt(settings.get("port")),
                            settings.get("user"),
                            settings.get("password"),
                            settings.get("privatekey"),
                            jobDir.resolve("id_rsa").toString(),
                            commandString);
                }
            }

            JobNode runningJob = new JobNode(jobId, "running", jobDesc);
            PortablePipelineCommon.upsertJob(jobs, runningJob);
            PortablePipelineCommon.saveJobs(jobs);
            return runningJob;
        } catch (Exception e) {
            PortablePipelineCommon.upsertJob(jobs, new JobNode(jobId, "aborted", e.toString()));
            PortablePipelineCommon.saveJobs(jobs);
            throw e;
        } finally {
            if (channelSftp != null) {
                try {
                    channelSftp.exit();
                } catch (Exception e) {
                }
            }
            if (sftpChannel != null) {
                try {
                    sftpChannel.disconnect();
                } catch (Exception e) {
                }
            }
            if (sshSession != null) {
                try {
                    sshSession.disconnect();
                } catch (Exception e) {
                }
            }
        }
    }

    public static List<JobNode> refreshJobs(boolean verbose) throws Exception {
        List<JobNode> jobs = PortablePipelineCommon.loadJobs();
        boolean changed = false;
        for (int i = 0; i < jobs.size(); i++) {
            JobNode job = jobs.get(i);
            if (!Objects.equals(job.status, "running")) {
                continue;
            }
            JobNode refreshed = refreshJob(job, verbose);
            if (!Objects.equals(job.status, refreshed.status) || !Objects.equals(job.desc, refreshed.desc)) {
                jobs.set(i, refreshed);
                changed = true;
            }
        }
        if (changed) {
            PortablePipelineCommon.saveJobs(jobs);
        }
        return jobs;
    }

    public static JobNode refreshJob(String jobId, boolean verbose) throws Exception {
        List<JobNode> jobs = refreshJobs(verbose);
        for (JobNode job : jobs) {
            if (job.id.equals(jobId)) {
                return job;
            }
        }
        throw new IllegalArgumentException("Unknown job id: " + jobId);
    }

    public static void stopJob(String jobId) throws Exception {
        List<JobNode> jobs = PortablePipelineCommon.loadJobs();
        JobNode job = findJob(jobs, jobId);
        if (!"running".equals(job.status) && !"preparing".equals(job.status)) {
            throw new IllegalStateException("Job " + jobId + " is not running.");
        }

        Path jobDir = PortablePipelineCommon.resolveJobDir(jobId);
        Path resultsDir = jobDir.resolve("results");
        JsonNode node = PortablePipelineCommon.MAPPER.readTree(jobDir.resolve("settings.json").toFile());
        String preset = node.path("preset").asText();
        String outputDir = jobDir.toString();
        String workDir = node.path("workfolder").asText() + "/" + jobId;
        String keyFilePath = jobDir.resolve("id_rsa").toString();

        if ("shirokane".equals(preset) || "ddbj".equals(preset) || "ssh (SGE)".equals(preset)) {
            ChannelSftp sftp = ConnectSsh.getSftpChannel(node, outputDir);
            try {
                sftp.get(workDir + "/save_jid.txt", resultsDir.resolve("save_jid.txt").toString());
                List<String> lines = Files.readAllLines(resultsDir.resolve("save_jid.txt"), StandardCharsets.UTF_8);
                int remoteJobId = Integer.parseInt(lines.get(0).trim());
                if (!"ddbj".equals(preset)) {
                    ConnectSsh.getSshCmdResult(
                            node,
                            keyFilePath,
                            "if [ `find " + workDir
                                    + " |grep /qsub.log$|wc -l` != 0 ];then cat `find . |grep /qsub.log$`; fi|awk '{print $3}'|xargs qdel; qdel "
                                    + remoteJobId + " 2>&1");
                } else {
                    ConnectSsh.getSshCmdResult2StepSession(node, keyFilePath, "scancel " + remoteJobId + " 2>&1");
                }
                PortablePipelineCommon.recursiveFolderDownload(workDir, resultsDir.toString(), sftp);
                if ("true".equals(node.path("checkdelete").asText())) {
                    PortablePipelineCommon.lsFolderRemove(workDir, sftp);
                }
            } finally {
                sftp.exit();
                sftp.getSession().disconnect();
            }
        } else if ("ssh".equals(preset)) {
            ChannelSftp sftp = ConnectSsh.getSftpChannel(node, outputDir);
            try {
                sftp.get(workDir + "/save_pid.txt", resultsDir.resolve("save_pid.txt").toString());
                List<String> lines = Files.readAllLines(resultsDir.resolve("save_pid.txt"), StandardCharsets.UTF_8);
                int remotePid = Integer.parseInt(lines.get(0).trim());
                ConnectSsh.getSshCmdResult(node, keyFilePath, "kill " + remotePid);
                PortablePipelineCommon.recursiveFolderDownload(workDir, resultsDir.toString(), sftp);
                if ("true".equals(node.path("checkdelete").asText())) {
                    PortablePipelineCommon.lsFolderRemove(workDir, sftp);
                }
            } finally {
                sftp.exit();
                sftp.getSession().disconnect();
            }
        } else if ("WSL".equals(preset)) {
            int pid = readPid(resultsDir.resolve("save_pid.txt"));
            new ProcessBuilder("bash.exe", "-c", "kill " + pid).start();
        } else {
            int pid = readPid(resultsDir.resolve("save_pid.txt"));
            new ProcessBuilder("kill", String.valueOf(pid)).start();
        }

        PortablePipelineCommon.upsertJob(jobs, new JobNode(jobId, "cancelled", job.desc));
        PortablePipelineCommon.saveJobs(jobs);
    }

    public static void deleteJob(String jobId) throws Exception {
        List<JobNode> jobs = PortablePipelineCommon.loadJobs();
        JobNode job = findJob(jobs, jobId);
        if ("running".equals(job.status) || "preparing".equals(job.status)) {
            throw new IllegalStateException("Stop the job before deleting it.");
        }
        jobs.removeIf(item -> item.id.equals(jobId));
        PortablePipelineCommon.saveJobs(jobs);
        PortablePipelineCommon.deleteDirectoryRecursively(PortablePipelineCommon.resolveJobDir(jobId));
    }

    public static void printLog(String jobId, boolean follow) throws Exception {
        Path logPath = PortablePipelineCommon.getJobResultsDir(jobId).resolve("log.txt");
        if (!follow) {
            if (!Files.exists(logPath)) {
                refreshJob(jobId, false);
            }
            if (!Files.exists(logPath)) {
                System.out.println("No log file yet.");
                return;
            }
            Files.readAllLines(logPath, StandardCharsets.UTF_8).forEach(System.out::println);
            return;
        }

        int printedLines = 0;
        while (true) {
            JobNode currentJob = refreshJob(jobId, false);
            if (Files.exists(logPath)) {
                List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
                for (int i = printedLines; i < lines.size(); i++) {
                    System.out.println(lines.get(i));
                }
                printedLines = lines.size();
            }
            if (!"running".equals(currentJob.status) && !"preparing".equals(currentJob.status)) {
                break;
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }

    private static void validateInputs(PPScript script, Map<String, List<String>> inputValues) {
        for (InputItem item : script.inputs) {
            List<String> values = inputValues.getOrDefault(item.id, List.of());
            boolean required = !item.num.contains("option");
            if (required && values.isEmpty()) {
                throw new IllegalArgumentException(item.desc + " is required.");
            }
            for (String value : values) {
                if (!Files.exists(Path.of(value))) {
                    throw new IllegalArgumentException("Input not found: " + value);
                }
            }
        }
    }

    private static String buildJobDescription(
            PPScript script,
            Map<String, List<String>> inputValues,
            Map<String, String> optionValues) {
        StringBuilder builder = new StringBuilder(script.filename);
        for (InputItem item : script.inputs) {
            List<String> values = inputValues.getOrDefault(item.id, List.of());
            builder.append(" ").append(item.id).append(":").append(String.join(",", values));
        }
        for (OptionItem item : script.options) {
            builder.append(" ")
                    .append(item.id)
                    .append(":")
                    .append(optionValues.getOrDefault(item.id, item.defaultopt));
        }
        return builder.toString();
    }

    private static String optionValueByDescription(
            PPScript script,
            Map<String, String> optionValues,
            String description,
            String fallback) {
        for (OptionItem item : script.options) {
            if (description.equals(item.desc)) {
                return optionValues.getOrDefault(item.id, item.defaultopt);
            }
        }
        return fallback;
    }

    private static String buildRunCommand(
            PPScript script,
            Map<String, List<String>> inputValues,
            Map<String, String> optionValues) {
        String command = script.runCommand;
        for (InputItem item : script.inputs) {
            List<String> values = inputValues.getOrDefault(item.id, List.of());
            String replacement;
            if (values.isEmpty()) {
                replacement = "''";
            } else if (item.num.contains("directory")) {
                replacement = PortablePipelineCommon.shellQuote(item.id);
            } else {
                Path value = Path.of(values.get(0));
                replacement = PortablePipelineCommon.shellQuote(item.id + "/" + value.getFileName());
            }
            command = command.replace("#" + item.id + "#", replacement);
        }
        for (OptionItem item : script.options) {
            String value = optionValues.getOrDefault(item.id, item.defaultopt);
            command = command.replace("#" + item.id + "#", PortablePipelineCommon.shellQuote(value));
        }
        return command;
    }

    private static void ensureRemoteDirectories(ChannelSftp channelSftp, String workDir) throws Exception {
        String tempWorkPath = "";
        for (String node : workDir.split("/")) {
            if (node.isEmpty()) {
                continue;
            }
            if (tempWorkPath.isEmpty()) {
                if (workDir.startsWith("/")) {
                    tempWorkPath = "/" + node;
                } else {
                    tempWorkPath = node;
                }
            } else {
                tempWorkPath = tempWorkPath + "/" + node;
            }
            try {
                channelSftp.mkdir(tempWorkPath);
            } catch (Exception e) {
            }
        }
    }

    private static void transferScriptBundle(
            String selectedScript,
            List<String> childScripts,
            String scriptFolder,
            Path resultsDir,
            String workDir,
            ChannelSftp channelSftp) throws Exception {
        Path scriptDir = PortablePipelineCommon.getBinDirPath().resolve(scriptFolder);
        List<String> filesToStage = new ArrayList<>();
        filesToStage.add(selectedScript);
        filesToStage.add("common.sh");
        filesToStage.add("pp.py");
        filesToStage.addAll(childScripts);

        for (String fileName : filesToStage) {
            Path source = scriptDir.resolve(fileName);
            PortablePipelineCommon.copyFileWithTimestamp(source, resultsDir.resolve(source.getFileName()));
            if (channelSftp != null) {
                channelSftp.put(source.toString(), workDir);
            }
        }
    }

    private static void stageInputs(
            PPScript script,
            Map<String, List<String>> inputValues,
            String preset,
            Path resultsDir,
            String workDir,
            ChannelSftp channelSftp) throws Exception {
        for (InputItem item : script.inputs) {
            List<String> values = inputValues.getOrDefault(item.id, List.of());
            if (values.isEmpty()) {
                continue;
            }
            Path localTargetDir = resultsDir.resolve(item.id);
            Files.createDirectories(localTargetDir);
            if (channelSftp != null) {
                try {
                    channelSftp.mkdir(workDir + "/" + item.id);
                } catch (Exception e) {
                }
            }
            for (String value : values) {
                Path source = Path.of(value);
                if (channelSftp != null) {
                    channelSftp.put(source.toString(), workDir + "/" + item.id);
                }
                PortablePipelineCommon.mkSymLinkOrCopy(preset, source, localTargetDir);
            }
        }
    }

    private static String createWrapperAndLaunchCommand(
            Map<String, String> settings,
            String selectedScript,
            Path resultsDir,
            String workDir,
            String runCmd,
            String numCpu,
            String numMem) throws IOException {
        String preset = settings.get("preset");
        Path templateFile;
        String commandString;
        if ("ssh".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/ssh-wrapper.sh");
            commandString = "cd " + workDir + "; bash wrapper.sh";
        } else if ("shirokane".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/shirokane-wrapper.sh");
            commandString = "cd " + workDir + "; qsub -terse wrapper.sh 2>&1 > save_jid.txt";
        } else if ("ddbj".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/ddbj-wrapper.sh");
            commandString = "cd " + workDir + "; sbatch --parsable wrapper.sh 2>&1 > save_jid.txt";
        } else if ("ssh (SGE)".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/sshsge-wrapper.sh");
            commandString = "cd " + workDir + "; qsub -terse wrapper.sh 2>&1 > save_jid.txt";
        } else if ("WSL".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/WSL-wrapper.sh");
            Path batchTemplate = PortablePipelineCommon.getBinDirPath().resolve("templates/WSL-wrapper.bat");
            Path batchOutput = resultsDir.resolve("wrapper.bat");
            String currentDir = PortablePipelineCommon.getBaseDirPath().toAbsolutePath().toString();
            String wslCurrentDir = "/mnt/" + currentDir.substring(0, 1).toLowerCase() + currentDir.substring(2);
            wslCurrentDir = wslCurrentDir.replace("\\", "/");
            try (BufferedReader reader = new BufferedReader(new FileReader(batchTemplate.toFile()));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(batchOutput.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("@wslcurDir@", wslCurrentDir);
                    line = line.replace("@password@", settings.get("password"));
                    writer.write(line);
                    writer.newLine();
                }
            }
            commandString = "cmd.exe /c start cmd.exe /k " + batchOutput.toAbsolutePath().toString().replace("/", "\\");
        } else if ("Mac".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/Mac-wrapper.sh");
            commandString = "bash wrapper.sh";
        } else if ("Linux".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/Linux-wrapper.sh");
            commandString = "bash wrapper.sh";
        } else if ("Linux (SGE)".equals(preset)) {
            templateFile = PortablePipelineCommon.getBinDirPath().resolve("templates/Linuxsge-wrapper.sh");
            commandString = "bash wrapper.sh";
        } else {
            throw new IllegalArgumentException("Unsupported preset: " + preset);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(templateFile.toFile()));
                BufferedWriter writer = new BufferedWriter(new FileWriter(resultsDir.resolve("wrapper.sh").toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("@imagefolder@", settings.get("imagefolder"));
                line = line.replace("@selectedScript@", selectedScript);
                line = line.replace("@runcmd@", runCmd);
                line = line.replace("@numCPU@", numCpu);
                line = line.replace(
                        "@numMEM_1core@",
                        String.valueOf(Double.valueOf(numMem) / Double.valueOf(numCpu)));
                line = line.replace("@numMEM_total@", numMem);
                writer.write(line);
                writer.newLine();
            }
        }

        return commandString;
    }

    private static void launchLocalJob(String commandString, String preset, Path resultsDir) throws Exception {
        if ("WSL".equals(preset)) {
            Process process = Runtime.getRuntime().exec(commandString, null, resultsDir.toFile());
            process.waitFor();
            return;
        }
        Process process = new ProcessBuilder("bash", "wrapper.sh")
                .directory(resultsDir.toFile())
                .inheritIO()
                .start();
        process.waitFor();
    }

    private static void writeJobSettings(Map<String, String> settings, Path outputPath) throws IOException {
        var node = PortablePipelineCommon.MAPPER.createObjectNode();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            node.put(entry.getKey(), entry.getValue());
        }
        Files.writeString(
                outputPath,
                PortablePipelineCommon.MAPPER
                        .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                        .writeValueAsString(node),
                StandardCharsets.UTF_8);
    }

    private static JobNode refreshJob(JobNode job, boolean verbose) throws Exception {
        Path jobDir = PortablePipelineCommon.resolveJobDir(job.id);
        Path resultsDir = jobDir.resolve("results");
        JsonNode node = PortablePipelineCommon.MAPPER.readTree(jobDir.resolve("settings.json").toFile());
        String preset = node.path("preset").asText();
        String workDir = node.path("workfolder").asText() + "/" + job.id;
        String keyFilePath = jobDir.resolve("id_rsa").toString();

        if (!PortablePipelineCommon.isLocalPreset(preset)) {
            ChannelSftp channelSftp = ConnectSsh.getSftpChannel(node, jobDir.toString());
            try {
                try {
                    channelSftp.ls(workDir);
                } catch (Exception e) {
                    return new JobNode(job.id, "aborted", job.desc);
                }

                try {
                    Files.createDirectories(resultsDir);
                    channelSftp.get(workDir + "/log.txt", resultsDir.toString());
                } catch (Exception e) {
                    if (verbose) {
                        System.out.println("log.txt is not available yet for job " + job.id);
                    }
                }

                try {
                    channelSftp.get(workDir + "/fin_status", resultsDir.toString());
                } catch (Exception e) {
                    if (verbose) {
                        System.out.println("fin_status is not available yet for job " + job.id);
                    }
                }

                Path finStatus = resultsDir.resolve("fin_status");
                if (Files.exists(finStatus)) {
                    PortablePipelineCommon.recursiveFolderDownload(workDir, resultsDir.toString(), channelSftp);
                    if ("true".equals(node.path("checkdelete").asText())) {
                        PortablePipelineCommon.lsFolderRemove(workDir, channelSftp);
                    }
                    return finalizeJobStatus(job, finStatus);
                }

                int processStatus = remoteProcessStatus(node, keyFilePath, preset, workDir);
                if (processStatus != 0) {
                    PortablePipelineCommon.recursiveFolderDownload(workDir, resultsDir.toString(), channelSftp);
                    if ("true".equals(node.path("checkdelete").asText())) {
                        PortablePipelineCommon.lsFolderRemove(workDir, channelSftp);
                    }
                    return new JobNode(job.id, "aborted", job.desc);
                }
                return job;
            } finally {
                channelSftp.exit();
                channelSftp.getSession().disconnect();
            }
        }

        Path finStatus = resultsDir.resolve("fin_status");
        if (Files.exists(finStatus)) {
            return finalizeJobStatus(job, finStatus);
        }

        int returnCode = localProcessStatus(preset, resultsDir);
        if (returnCode != 0) {
            return new JobNode(job.id, "aborted", job.desc);
        }
        return job;
    }

    private static JobNode finalizeJobStatus(JobNode job, Path finStatus) throws IOException {
        List<String> lines = Files.readAllLines(finStatus, StandardCharsets.UTF_8);
        String status = (!lines.isEmpty() && "0".equals(lines.get(0).trim())) ? "finished" : "aborted";
        return new JobNode(job.id, status, job.desc);
    }

    private static int remoteProcessStatus(JsonNode node, String keyFilePath, String preset, String workDir)
            throws Exception {
        List<String> list;
        if ("ssh".equals(preset)) {
            list = ConnectSsh.getSshCmdResult(
                    node,
                    keyFilePath,
                    "cd " + workDir + "; ps -p `cat save_pid.txt`|grep python|wc -l");
        } else if ("ddbj".equals(preset)) {
            list = ConnectSsh.getSshCmdResult2StepSession(
                    node,
                    keyFilePath,
                    "cd " + workDir
                            + "; if [ -s save_jid.txt ]; then squeue -j `cat save_jid.txt` 2> /dev/null|tail -n+2|wc -l; else echo The job was rejected. It is likely that the requested amount of resources was too large. >> log.txt; echo 0; fi");
        } else {
            list = ConnectSsh.getSshCmdResult(
                    node,
                    keyFilePath,
                    "cd " + workDir
                            + "; qstat -j `cat save_jid.txt`|awk '$1==\"error\"{system(\"qdel '`cat save_jid.txt`' >> log.txt 2>&1\"); e=1} END{if(e==1){print 0}else{print NR}}'");
        }
        return (!list.isEmpty() && "0".equals(list.get(0).trim())) ? 1 : 0;
    }

    private static int localProcessStatus(String preset, Path resultsDir) {
        try {
            if ("WSL".equals(preset)) {
                Process process = Runtime.getRuntime()
                        .exec("bash -c 'ps -p `cat save_pid.txt`'", null, resultsDir.toFile());
                return process.waitFor();
            }
            int pid = readPid(resultsDir.resolve("save_pid.txt"));
            Process process = new ProcessBuilder("ps", "-p", String.valueOf(pid))
                    .directory(resultsDir.toFile())
                    .start();
            return process.waitFor();
        } catch (Exception e) {
            return 1;
        }
    }

    private static int readPid(Path savePidPath) throws IOException {
        List<String> lines = Files.readAllLines(savePidPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IllegalStateException("No PID recorded in " + savePidPath);
        }
        return Integer.parseInt(lines.get(0).trim());
    }

    private static JobNode findJob(List<JobNode> jobs, String jobId) {
        for (JobNode job : jobs) {
            if (job.id.equals(jobId)) {
                return job;
            }
        }
        throw new IllegalArgumentException("Unknown job id: " + jobId);
    }
}
