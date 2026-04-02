package application;

import java.util.ArrayList;
import java.util.List;

public class PPScript {
    public final String filename;
    public String explanation;
    public String runCommand;
    public final List<InputItem> inputs;
    public final List<OptionItem> options;

    public PPScript(String filename) {
        this.filename = filename;
        this.explanation = "";
        this.runCommand = "";
        this.inputs = new ArrayList<>();
        this.options = new ArrayList<>();
    }

    public String getCategory() {
        int tildeIndex = filename.indexOf("~");
        if (tildeIndex <= 0) {
            return "";
        }
        return filename.substring(0, tildeIndex);
    }

    public String getSummary() {
        for (String line : explanation.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }
}
