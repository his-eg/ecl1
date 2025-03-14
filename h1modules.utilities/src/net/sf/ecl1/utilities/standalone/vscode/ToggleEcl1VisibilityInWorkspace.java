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
 * Class to toggle ecl1 visibility in vscode workspace.
 */
public class ToggleEcl1VisibilityInWorkspace {

    private static final ICommonLogger logger = LoggerFactory.getLogger(CopyTasks.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    private static final String ECL1_FOLDER = "**/ecl1";
    
    public static void main(String[] args) {
        Path workspacePath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath();
        Path settingsPath = workspacePath.resolve(".vscode/settings.json");

        if(!settingsPath.toFile().exists()){
            JsonObject settings = initSettingsObject(ECL1_FOLDER);
            saveJsonToFile(settings, settingsPath);
            return; // Exit success
        }

        Gson gson = new Gson();
        try (JsonReader settingsReader = new JsonReader(new FileReader(settingsPath.toFile()))) 
        {
            JsonObject settingsObject;
            try {
                settingsObject = gson.fromJson(settingsReader, JsonObject.class);
            } catch (JsonIOException | JsonSyntaxException e) {
                // If target file is malformed replace it by copying
                JsonObject settings = initSettingsObject(ECL1_FOLDER);
                saveJsonToFile(settings, settingsPath);
                return; // Exit success
            }
            // If target file is empty
            if(settingsObject == null) {
                settingsReader.close();
                JsonObject settings = initSettingsObject(ECL1_FOLDER);
                saveJsonToFile(settings, settingsPath);
                return; // Exit success
            }
            
            toggleExclusion(settingsObject, "files.exclude");
            toggleExclusion(settingsObject, "search.exclude");

            saveJsonToFile(settingsObject, settingsPath);

        } catch (IOException e) {
            logger.error("Error processing JSON files\n" + e.getMessage(), e);
        }

    }

    /** Saves JsonObject to a filePath. */
    private static void saveJsonToFile(JsonObject jsonObject, Path filePath) {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        } catch (IOException e) {
            logger.error("Error writing JSON to file.\npath: " + filePath + "\njsonObject: " + jsonObject, e);
        }
    }

    private static JsonObject initSettingsObject(String ECL1_FOLDER){
            JsonObject settings = new JsonObject();

            // Add files.exclude and search.exclude to settings object
            JsonObject excludeObject = new JsonObject();
            excludeObject.addProperty(ECL1_FOLDER, true);
            settings.add("files.exclude", excludeObject);
            settings.add("search.exclude", excludeObject);

            return settings;
    }

    private static void toggleExclusion(JsonObject settinsObject, String property) {
        JsonElement exclude = settinsObject.get(property);
        JsonObject jsonObject;

        if (exclude == null) {
            // Create property if it doesnt exist
            jsonObject = new JsonObject();
            jsonObject.addProperty(ECL1_FOLDER, true);
        } else {
            jsonObject = exclude.getAsJsonObject();
            if (jsonObject.has(ECL1_FOLDER)) {
                boolean currentValue = jsonObject.get(ECL1_FOLDER).getAsBoolean();
                jsonObject.addProperty(ECL1_FOLDER, !currentValue);
            } else {
                jsonObject.addProperty(ECL1_FOLDER, true);
            }
        }
        settinsObject.add(property, jsonObject);
    }
}