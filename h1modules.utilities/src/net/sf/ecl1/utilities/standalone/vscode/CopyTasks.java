package net.sf.ecl1.utilities.standalone.vscode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
 * Class to add/copy tasks from tasks.json to vscode workspace tasks.json.
 */
public class CopyTasks {

    private static final ICommonLogger logger = LoggerFactory.getLogger(CopyTasks.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());
    
    public static void main(String[] args) {
        Path sourcePath = Paths.get("src/net/sf/ecl1/utilities/standalone/vscode/tasks.json");
        Path targetPath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath().getParent().resolve(".vscode/tasks.json");

        // If tasks.json does not exist, create a copy from the source
        if(!targetPath.toFile().exists()){
            copyFile(sourcePath, targetPath);
            return; // Exit success
        }
        Gson gson = new Gson();

        try (
            JsonReader targetReader = new JsonReader(new FileReader(targetPath.toFile()));
            JsonReader srcReader = new JsonReader(new FileReader(sourcePath.toFile()));
            ) 
        {
            JsonObject jsonObjectTarget;
            try {
                jsonObjectTarget = gson.fromJson(targetReader, JsonObject.class);
            } catch (JsonIOException | JsonSyntaxException e) {
                // If target file is malformed replace it by copying
                targetReader.close();
                srcReader.close();
                copyFile(sourcePath, targetPath);
                return; // Exit success
            }
            // If target file is empty
            if(jsonObjectTarget == null) {
                targetReader.close();
                srcReader.close();
                copyFile(sourcePath, targetPath);
                return; // Exit success
            }
            
            JsonObject jsonObjectSource = gson.fromJson(srcReader, JsonObject.class);
            JsonElement targetTasks = jsonObjectTarget.get("tasks");
            JsonElement sourceTasks = jsonObjectSource.get("tasks");
            if(targetTasks == null){   
                // Add tasks array to object
                jsonObjectTarget.add("tasks", sourceTasks);
            }else{
                // Add tasks to existing tasks array
                JsonArray targetArray = targetTasks.getAsJsonArray();
                JsonArray sourceArray = sourceTasks.getAsJsonArray();
                for (JsonElement sourceElement : sourceArray) {
                    boolean exists = false;
                    // Loop through targetArray to check if the element already exists
                    for (int i = 0; i < targetArray.size(); i++) {
                        JsonElement targetElement = targetArray.get(i);
                        if (targetElement.getAsJsonObject().get("label").getAsString()
                            .equals(sourceElement.getAsJsonObject().get("label").getAsString())) {
                            // If it exists, replace the target element with the source element
                            targetArray.set(i, sourceElement);
                            exists = true;
                            break;
                        }
                    }
                    // Only add the source element if it doesnt already exist
                    if (!exists) {
                        targetArray.add(sourceElement);
                    }
                }
            }
            saveJsonToFile(jsonObjectTarget, targetPath);
    
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

    private static void copyFile(Path sourcePath, Path targetPath){
        File vscodeFolder = targetPath.getParent().toFile();
        if(!vscodeFolder.exists()){
            vscodeFolder.mkdir();
        }
        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error copying tasks.json\n" + e.getMessage(), e);
            System.exit(1);
        }
    }
}