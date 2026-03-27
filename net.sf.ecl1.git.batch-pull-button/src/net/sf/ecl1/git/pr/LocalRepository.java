package net.sf.ecl1.git.pr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information about a local Git repository.
 * Reads the .git/config, detects the current branch, LFS objects, server, and repo path.
 */
public class LocalRepository {

    private final File repositoryRoot;
    private final String repo;
    private final boolean lfs;
    private final String server;
    private final String repoPath;
    private final String branch;
    private final Set<String> knownRemotes;

    /**
     * Constructs a LocalRepository by scanning upward from the given directory.
     *
     * @param startDirectory the directory to start searching from
     * @throws IOException if the repository cannot be found or read
     */
    public LocalRepository(File startDirectory) throws IOException {
        this.repositoryRoot = findRepositoryRootDirectory(startDirectory);
        this.repo = repositoryRoot.getName();
        this.lfs = detectLFSObjects();
        this.knownRemotes = new HashSet<>();

        String[] originInfo = readGitConfig();
        this.server = originInfo[0];
        this.repoPath = originInfo[1];
        this.branch = readCurrentBranch();
    }

    private File findRepositoryRootDirectory(File startDir) throws IOException {
        File current = startDir;
        while (current != null) {
            if (new File(current, ".git").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IOException("Directory is not inside a git repository: " + startDir.getAbsolutePath());
    }

    private boolean detectLFSObjects() {
        File lfsDir = new File(repositoryRoot, ".git/lfs/objects");
        if (lfsDir.isDirectory()) {
            String[] entries = lfsDir.list();
            return entries != null && entries.length > 0;
        }
        return false;
    }

    /**
     * Reads .git/config to extract origin URL info and known remotes.
     * @return String[] with [0] = server, [1] = repoPath
     */
    private String[] readGitConfig() throws IOException {
        File configFile = new File(repositoryRoot, ".git/config");
        String fileContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
        String[] lines = fileContent.split("\\r?\\n");

        boolean inOrigin = false;
        String originUrl = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.equals("[remote \"origin\"]")) {
                inOrigin = true;
            } else if (line.startsWith("[remote \"")) {
                // extract remote name
                Matcher m = Pattern.compile("\\[remote \"(.*)\"\\]").matcher(line);
                if (m.matches()) {
                    knownRemotes.add(m.group(1));
                }
                inOrigin = false;
            } else if (line.startsWith("[")) {
                inOrigin = false;
            } else if (inOrigin && line.startsWith("url")) {
                originUrl = line;
            }
        }

        if (originUrl == null) {
            throw new IOException("No origin remote found in .git/config");
        }

        String serverName = extractServerName(originUrl);
        String path = extractRepoPath(originUrl, serverName);
        return new String[] { serverName, path };
    }

    /**
     * Extracts the server name from a git URL line.
     * Handles:
     * - url = git@gitlab.his.de:h1/webapps
     * - url = ssh://git@gitlab.his.de/h1/webapps
     * - url = https://gitlab.his.de/h1/webapps
     */
    private String extractServerName(String urlLine) throws IOException {
        // find the actual URL part after '='
        int eqIndex = urlLine.indexOf('=');
        if (eqIndex < 0) {
            throw new IOException("Unparseable url entry in .git/config: " + urlLine);
        }
        String url = urlLine.substring(eqIndex + 1).trim();

        int atIndex = url.indexOf('@');
        if (atIndex >= 0) {
            String afterAt = url.substring(atIndex + 1);
            int colonIndex = afterAt.indexOf(':');
            int slashIndex = afterAt.indexOf('/');
            int endIndex;
            if (colonIndex >= 0 && (slashIndex < 0 || colonIndex < slashIndex)) {
                endIndex = colonIndex;
            } else if (slashIndex >= 0) {
                endIndex = slashIndex;
            } else {
                throw new IOException("Unparseable url entry in .git/config: " + urlLine);
            }
            return afterAt.substring(0, endIndex).toLowerCase();
        }

        // https:// URL
        int doubleSlash = url.indexOf("//");
        if (doubleSlash >= 0) {
            String afterSlash = url.substring(doubleSlash + 2);
            int slashIndex = afterSlash.indexOf('/');
            if (slashIndex > 0) {
                return afterSlash.substring(0, slashIndex).toLowerCase();
            }
        }

        throw new IOException("Unparseable url entry in .git/config: " + urlLine);
    }

    /**
     * Extracts the repo path (e.g. "group/reponame") from a git URL line.
     */
    private String extractRepoPath(String urlLine, String serverName) {
        String lower = urlLine.toLowerCase();
        int pos = lower.indexOf(serverName) + serverName.length();
        String temp = urlLine.substring(pos + 1).trim();
        if (temp.endsWith(".git")) {
            temp = temp.substring(0, temp.length() - 4);
        }
        return temp;
    }

    private String readCurrentBranch() throws IOException {
        File headFile = new File(repositoryRoot, ".git/HEAD");
        String content = new String(Files.readAllBytes(headFile.toPath()), StandardCharsets.UTF_8).trim();
        if (content.startsWith("ref: refs/heads/")) {
            return content.substring(16);
        }
        return null; // detached HEAD
    }

    /**
     * Finds the target branch by examining the git log for branch references.
     * Uses the "git log" command and matches against the branches pattern.
     *
     * @param branchesPattern the regex pattern to match branch names
     * @return the matched target branch, or null if not found
     * @throws IOException on process errors
     */
    public String findTargetBranch(String branchesPattern) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("git", "log", "--pretty=format:%d");
        pb.directory(repositoryRoot);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Pattern pattern = Pattern.compile(branchesPattern);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        process.destroy();
                        return m.group(1);
                    }
                }
            }
        }
        return null;
    }

    public File getRepositoryRoot() {
        return repositoryRoot;
    }

    public String getRepo() {
        return repo;
    }

    public boolean hasLFS() {
        return lfs;
    }

    public String getServer() {
        return server;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getBranch() {
        return branch;
    }

    public Set<String> getKnownRemotes() {
        return knownRemotes;
    }
}
