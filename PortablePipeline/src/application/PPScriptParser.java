package application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PPScriptParser {

    private PPScriptParser() {
    }

    public static List<PPScript> loadScripts(Path scriptDir) throws IOException {
        List<PPScript> scripts = new ArrayList<>();
        try (var stream = Files.list(scriptDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("common.sh"))
                    .filter(path -> !path.getFileName().toString().equals("pp.py"))
                    .filter(path -> !path.getFileName().toString().equals("pp"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        try {
                            scripts.add(parse(path));
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse script: " + path, e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
        return scripts;
    }

    public static PPScript parse(Path scriptPath) throws IOException {
        List<String> lines = Files.readAllLines(scriptPath, StandardCharsets.UTF_8);
        PPScript script = new PPScript(scriptPath.getFileName().toString());

        boolean explanationField = false;
        boolean inputField = false;
        boolean optionField = false;
        boolean optionLongField = false;
        String currentOptionId = "";
        StringBuilder explanationBuilder = new StringBuilder();
        StringBuilder optionLongBuilder = new StringBuilder();

        for (String line : lines) {
            if ("'".equals(line)) {
                explanationField = false;
                inputField = false;
                optionField = false;
            } else if ("explanation='".equals(line)) {
                explanationField = true;
                continue;
            } else if ("inputdef='".equals(line)) {
                inputField = true;
                continue;
            } else if ("optiondef='".equals(line)) {
                optionField = true;
                continue;
            } else if ("#<option detail>".equals(line)) {
                optionLongField = true;
                continue;
            } else if ("#</option detail>".equals(line)) {
                optionLongField = false;
                flushOptionLongDescription(script, currentOptionId, optionLongBuilder.toString());
                currentOptionId = "";
                optionLongBuilder.setLength(0);
                continue;
            }

            if (explanationField) {
                if (explanationBuilder.length() > 0) {
                    explanationBuilder.append(System.lineSeparator());
                }
                explanationBuilder.append(line);
                continue;
            }

            if (inputField) {
                String[] array = line.split(":", -1);
                if (array.length >= 4) {
                    InputItem item = new InputItem();
                    item.id = array[0];
                    item.num = array[1];
                    item.desc = array[2];
                    item.filetype = array[3];
                    script.inputs.add(item);
                }
                continue;
            }

            if (optionField) {
                String[] array = line.split(":", -1);
                if (array.length >= 3) {
                    OptionItem item = new OptionItem();
                    item.id = array[0];
                    item.desc = array[1];
                    item.defaultopt = array[2];
                    script.options.add(item);
                }
                continue;
            }

            if (optionLongField) {
                if (line.startsWith("#<") && line.endsWith(">") && !line.startsWith("#</")) {
                    flushOptionLongDescription(script, currentOptionId, optionLongBuilder.toString());
                    currentOptionId = line.substring(2, line.length() - 1);
                    optionLongBuilder.setLength(0);
                    continue;
                }
                if (line.startsWith("#</") && line.endsWith(">")) {
                    flushOptionLongDescription(script, currentOptionId, optionLongBuilder.toString());
                    currentOptionId = "";
                    optionLongBuilder.setLength(0);
                    continue;
                }
                if (optionLongBuilder.length() > 0) {
                    optionLongBuilder.append(System.lineSeparator());
                }
                optionLongBuilder.append(line);
                continue;
            }

            if (line.startsWith("runcmd=\"$0")) {
                script.runCommand = line.substring(11, line.length() - 1);
            }
        }

        script.explanation = explanationBuilder.toString().trim();
        return script;
    }

    private static void flushOptionLongDescription(PPScript script, String optionId, String text) {
        if (optionId == null || optionId.isEmpty()) {
            return;
        }
        for (OptionItem item : script.options) {
            if (item.id.equals(optionId)) {
                item.longdesc = text.trim();
                return;
            }
        }
    }
}
