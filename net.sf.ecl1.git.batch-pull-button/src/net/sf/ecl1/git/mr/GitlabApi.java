package net.sf.ecl1.git.mr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Calls to the Gitlab API (GraphQL and REST).
 */
public class GitlabApi {

    private final String server;
    private final String token;

    public GitlabApi(GitlabConfig config) {
        this.server = config.getServer();
        this.token = config.getToken();
    }

    /**
     * Data class for fork details.
     */
    public static class ForkDetails {
        public final int ahead;
        public final int behind;
        public final boolean isSyncing;
        public final boolean hasConflicts;

        public ForkDetails(int ahead, int behind, boolean isSyncing, boolean hasConflicts) {
            this.ahead = ahead;
            this.behind = behind;
            this.isSyncing = isSyncing;
            this.hasConflicts = hasConflicts;
        }
    }

    /**
     * Get status information about a fork.
     *
     * @param namespace the fork namespace (username)
     * @param repo the repository name
     * @return fork details, or null if the fork does not exist
     * @throws IOException on network errors
     */
    public ForkDetails getForkDetails(String namespace, String repo) throws IOException {
        String query = "query getForkDetails($projectPath: ID!, $ref: String) {"
                + "project(fullPath: $projectPath) {"
                + "  id"
                + "  forkDetails(ref: $ref) {"
                + "    ahead"
                + "    behind"
                + "    isSyncing"
                + "    hasConflicts"
                + "    __typename"
                + "  }"
                + "  __typename"
                + "}"
                + "}";

        JsonObject variables = new JsonObject();
        variables.addProperty("projectPath", namespace + "/" + repo);
        variables.addProperty("ref", "master");

        JsonObject body = new JsonObject();
        body.addProperty("operationName", "getForkDetails");
        body.add("variables", variables);
        body.addProperty("query", query);

        String responseBody = executeGraphQL(body);
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

        JsonElement dataElement = json.get("data");
        if (dataElement == null || !dataElement.isJsonObject()) {
            return null;
        }
        JsonElement projectElement = dataElement.getAsJsonObject().get("project");
        if (projectElement == null || !projectElement.isJsonObject()) {
            return null;
        }
        JsonElement forkElement = projectElement.getAsJsonObject().get("forkDetails");
        if (forkElement == null || !forkElement.isJsonObject()) {
            return null;
        }

        JsonObject forkDetails = forkElement.getAsJsonObject();

        // Gitlab may return forkDetails with null values while computing;
        // treat this as "not ready yet" (same as no fork details)
        JsonElement aheadEl = forkDetails.get("ahead");
        JsonElement behindEl = forkDetails.get("behind");
        if (aheadEl == null || aheadEl.isJsonNull() || behindEl == null || behindEl.isJsonNull()) {
            return null;
        }

        JsonElement isSyncingEl = forkDetails.get("isSyncing");
        JsonElement hasConflictsEl = forkDetails.get("hasConflicts");

        return new ForkDetails(
                aheadEl.getAsInt(),
                behindEl.getAsInt(),
                isSyncingEl != null && !isSyncingEl.isJsonNull() && isSyncingEl.getAsBoolean(),
                hasConflictsEl != null && !hasConflictsEl.isJsonNull() && hasConflictsEl.getAsBoolean());
    }

    /**
     * Updates a fork with the commits from the main repository.
     * This call is asynchronous; use {@link #getForkDetails} to wait for completion.
     *
     * @param namespace the fork namespace
     * @param repo the repository name
     * @param branch the target branch to sync
     * @throws IOException on network errors or server errors
     */
    public void syncFork(String namespace, String repo, String branch) throws IOException {
        String query = "mutation syncFork($projectPath: ID!, $targetBranch: String!) {"
                + "  projectSyncFork(input: {projectPath: $projectPath, targetBranch: $targetBranch}) {"
                + "    details {"
                + "      ahead"
                + "      behind"
                + "      isSyncing"
                + "      hasConflicts"
                + "      __typename"
                + "    }"
                + "    errors"
                + "    __typename"
                + "  }"
                + "}";

        JsonObject variables = new JsonObject();
        variables.addProperty("projectPath", namespace + "/" + repo);
        variables.addProperty("targetBranch", branch);

        JsonObject body = new JsonObject();
        body.addProperty("operationName", "syncFork");
        body.add("variables", variables);
        body.addProperty("query", query);

        executeGraphQL(body);
    }

    /**
     * Creates a fork of a project.
     *
     * @param sourceProject the full path of the source project (e.g. "group/repo")
     * @param targetNamespace the namespace to fork into
     * @throws IOException on network errors or server errors
     */
    public void forkProject(String sourceProject, String targetNamespace) throws IOException {
        String url = "https://" + server + "/api/v4/projects/"
                + java.net.URLEncoder.encode(sourceProject, "UTF-8") + "/fork";

        JsonObject body = new JsonObject();
        body.addProperty("namespace", targetNamespace);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 300) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    throw new IOException("forkProject " + sourceProject + " to " + targetNamespace
                            + " failed with server response: " + status + " " + responseBody);
                }
            }
        }
    }

    /**
     * Searches for Gitlab users matching the given query string.
     *
     * @param query the search term (matched against username and name)
     * @return list of usernames, empty list if no matches or on error
     */
    public List<String> searchUsers(String query) {
        List<String> usernames = new ArrayList<>();
        try {
            String url = "https://" + server + "/api/v4/users?search="
                    + java.net.URLEncoder.encode(query, "UTF-8") + "&per_page=10";

            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(url);
                get.setHeader("Authorization", "Bearer " + token);

                try (CloseableHttpResponse response = client.execute(get)) {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        JsonArray users = JsonParser.parseString(body).getAsJsonArray();
                        for (JsonElement element : users) {
                            JsonObject user = element.getAsJsonObject();
                            String username = user.get("username").getAsString();
                            usernames.add(username);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // non-fatal, return empty list
        }
        return usernames;
    }

    /**
     * Executes a GraphQL request and returns the response body as a string.
     */
    private String executeGraphQL(JsonObject body) throws IOException {
        String url = "https://" + server + "/api/graphql";

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Bearer " + token);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new IOException("Gitlab API request failed with status: " + status + " " + responseBody);
                }
                return responseBody;
            }
        }
    }
}
