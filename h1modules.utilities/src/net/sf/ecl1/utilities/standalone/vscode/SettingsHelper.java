package net.sf.ecl1.utilities.standalone.vscode;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Helper class to manage vscode settings.
 */
public class SettingsHelper {

    private static final ICommonLogger logger = LoggerFactory.getLogger(SettingsHelper.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    private final Path SETTINGS_PATH;
    private JsonObject settings;

    public SettingsHelper(){
        Path workspaceParentPath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath().getParent();
        this.SETTINGS_PATH = workspaceParentPath.resolve(".vscode/settings.json");

        if(!SETTINGS_PATH.toFile().exists()){
            this.settings = new JsonObject();
        }else{
            readSettings();
        }
    }


    private void readSettings(){
        Gson gson = new Gson();
        try (JsonReader settingsReader = new JsonReader(new FileReader(SETTINGS_PATH.toFile()))) {
            this.settings = gson.fromJson(settingsReader, JsonObject.class);
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            logger.error("Error reading settings.json " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public void toggleExclusion(String property, String value) {
        JsonElement exclude = settings.get(property);
        JsonObject jsonObject;

        if (exclude == null) {
            // Create property if it doesnt exist
            jsonObject = new JsonObject();
            jsonObject.addProperty(value, true);
        } else {
            jsonObject = exclude.getAsJsonObject();
            if (jsonObject.has(value)) {
                boolean currentValue = jsonObject.get(value).getAsBoolean();
                jsonObject.addProperty(value, !currentValue);
            } else {
                jsonObject.addProperty(value, true);
            }
        }
        settings.add(property, jsonObject);
    }

    public void save() {
        if(!SETTINGS_PATH.toFile().exists()){
            try {
                // create .vscode if not exists
                if(!SETTINGS_PATH.getParent().toFile().exists()){
                    SETTINGS_PATH.getParent().toFile().mkdir();
                }
                // create settings file
                SETTINGS_PATH.toFile().createNewFile();
            } catch (IOException e) {
                logger.error("Error createing settings file.\npath: " + SETTINGS_PATH.toString() + "\n" +e.getMessage(), e);
                return;
            }
        }
        // write settings to file
        try (FileWriter writer = new FileWriter(SETTINGS_PATH.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(settings, writer);
        } catch (IOException e) {
            logger.error("Error writing JSON to settings.\npath: " + SETTINGS_PATH.toString() + "\njsonObject: " + settings + "\n" +e.getMessage(), e);
        }
    }
}