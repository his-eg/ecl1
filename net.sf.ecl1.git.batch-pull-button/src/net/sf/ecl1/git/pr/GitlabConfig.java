package net.sf.ecl1.git.pr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Reads configuration from ~/.config/gitlab.json
 *
 * <pre>
 * {
 *     "gitlab.his.de": {
 *         "username": "myusername",
 *         "token": "glpat-...",
 *         "branches": "(?:[^/]|^)(master|RELEASE_\\d\\d\\d\\d_\\d\\d)\\b"
 *     }
 * }
 * </pre>
 */
public class GitlabConfig {

    private JsonObject allConfig;
    private String branches;
    private String server;
    private String username;
    private String token;

    /**
     * Reads the configuration file from the user's home directory.
     *
     * @throws IOException if the configuration file cannot be read or parsed
     */
    public GitlabConfig() throws IOException {
        String userHome = System.getProperty("user.home");
        File configFile = new File(userHome, ".config/gitlab.json");

        if (!configFile.exists()) {
            throw new IOException("Configuration file does not exist: " + configFile.getAbsolutePath()
                    + "\n\n" + getConfigExample(configFile.getAbsolutePath()));
        }

        String fileContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
        // Remove single-line and multi-line comments
        String withoutComments = fileContent.replaceAll("//.*", "").replaceAll("/\\*[\\s\\S]*?\\*/", "");

        allConfig = JsonParser.parseString(withoutComments).getAsJsonObject();
    }

    /**
     * Activates a specific server section from the configuration.
     *
     * @param section the server name section to activate
     * @throws IllegalArgumentException if the section is missing or incomplete
     */
    public void activateSection(String section) {
        JsonElement sectionElement = allConfig.get(section);
        if (sectionElement == null || !sectionElement.isJsonObject()) {
            throw new IllegalArgumentException("No section '" + section + "' in configuration file.\n\n"
                    + getConfigExample(System.getProperty("user.home") + "/.config/gitlab.json"));
        }

        JsonObject sectionObj = sectionElement.getAsJsonObject();

        this.branches = getStringOrNull(sectionObj, "branches");
        this.server = getStringOrNull(sectionObj, "server");
        if (this.server == null) {
            this.server = section;
        }
        this.username = getStringOrNull(sectionObj, "username");
        this.token = getStringOrNull(sectionObj, "token");

        if (this.branches == null || this.username == null || this.token == null) {
            throw new IllegalArgumentException(
                    "At least one of the parameters \"branches\", \"username\" or \"token\" "
                            + "is missing in the configuration file in section \"" + section + "\".\n\n"
                            + getConfigExample(System.getProperty("user.home") + "/.config/gitlab.json"));
        }
    }

    private String getStringOrNull(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }

    private String getConfigExample(String path) {
        return "Please create or check the configuration file " + path + "\n\n"
                + "{\n"
                + "    // Servername of the Gitlab server\n"
                + "    \"gitlab.his.de\": {\n"
                + "\n"
                + "        // Your Gitlab username.\n"
                + "        \"username\": \"myusername\",\n"
                + "\n"
                + "        // In Gitlab: Avatar -> Settings -> Access Token\n"
                + "        // Enable the scope \"API\".\n"
                + "        \"token\": \"glpat-...\",\n"
                + "\n"
                + "        // RegEx to match destination branches\n"
                + "        \"branches\": \"(?:[^/]|^)(master|RELEASE_\\\\d\\\\d\\\\d\\\\d_\\\\d\\\\d)\\\\b\"\n"
                + "    }\n"
                + "}";
    }

    public String getBranches() {
        return branches;
    }

    public String getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }
}
