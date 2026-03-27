package net.sf.ecl1.git.mr;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Information about a local Git repository.
 * Uses JGit to read the repository configuration, current branch, LFS objects, and remote URLs.
 */
public class LocalRepository {

    private final Git git;
    private final Repository repository;
    private final File repositoryRoot;
    private final String repo;
    private final boolean lfs;
    private final String server;
    private final String repoPath;
    private final String branch;
    private final Set<String> knownRemotes;

    /**
     * Constructs a LocalRepository by opening the git repository at or above the given directory.
     *
     * @param startDirectory the directory to start searching from
     * @throws IOException if the repository cannot be found or read
     */
    public LocalRepository(File startDirectory) throws IOException {
        this.git = Git.open(startDirectory);
        this.repository = git.getRepository();
        this.repositoryRoot = repository.getWorkTree();
        this.repo = repositoryRoot.getName();
        this.lfs = detectLFSObjects();
        this.knownRemotes = new HashSet<>();

        String[] originInfo = readOriginUrl();
        this.server = originInfo[0];
        this.repoPath = originInfo[1];
        this.branch = repository.getBranch();
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
     * Reads the origin remote URL from the JGit StoredConfig and collects known remotes.
     * @return String[] with [0] = server, [1] = repoPath
     */
    private String[] readOriginUrl() throws IOException {
        String originUrl = null;

        try {
            for (RemoteConfig remote : RemoteConfig.getAllRemoteConfigs(repository.getConfig())) {
                String name = remote.getName();
                if ("origin".equals(name)) {
                    if (!remote.getURIs().isEmpty()) {
                        originUrl = remote.getURIs().get(0).toString();
                    }
                } else {
                    knownRemotes.add(name);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read remote configs: " + e.getMessage(), e);
        }

        if (originUrl == null) {
            throw new IOException("No origin remote found in .git/config");
        }

        String serverName = extractServerName(originUrl);
        String path = extractRepoPath(originUrl, serverName);
        return new String[] { serverName, path };
    }

    /**
     * Extracts the server name from a git URL.
     * Handles:
     * - git@gitlab.his.de:h1/webapps
     * - ssh://git@gitlab.his.de/h1/webapps
     * - https://gitlab.his.de/h1/webapps
     */
    private String extractServerName(String url) throws IOException {
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
                throw new IOException("Unparseable git URL: " + url);
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

        throw new IOException("Unparseable git URL: " + url);
    }

    /**
     * Extracts the repo path (e.g. "group/reponame") from a git URL.
     */
    private String extractRepoPath(String url, String serverName) {
        String lower = url.toLowerCase();
        int pos = lower.indexOf(serverName) + serverName.length();
        String temp = url.substring(pos + 1).trim();
        if (temp.endsWith(".git")) {
            temp = temp.substring(0, temp.length() - 4);
        }
        return temp;
    }

    /**
     * Finds the target branch by examining the git log for branch references.
     * Uses JGit LogCommand and walks commits from HEAD, checking which refs
     * (branches) point to each commit — similar to {@code git log --pretty=format:%d}.
     * <p>
     * The ref names are formatted like {@code git log} decorations, e.g.
     * {@code origin/master}, {@code origin/RELEASE_2025_12}, so that the
     * branches regex from the configuration can match them.
     *
     * @param branchesPattern the regex pattern to match branch names
     * @return the matched target branch, or null if not found
     * @throws IOException on repository errors
     */
    public String findTargetBranch(String branchesPattern) throws IOException {
        Pattern pattern = Pattern.compile(branchesPattern);

        // Build a reverse map: commit ObjectId (hex) -> list of short ref names
        // Use getRefsByPrefix to get all refs under refs/heads/ and refs/remotes/
        java.util.HashMap<String, java.util.List<String>> commitRefMap = new java.util.HashMap<>();

        List<Ref> allRefs = new java.util.ArrayList<>();
        allRefs.addAll(repository.getRefDatabase().getRefsByPrefix("refs/heads/"));
        allRefs.addAll(repository.getRefDatabase().getRefsByPrefix("refs/remotes/"));

        for (Ref ref : allRefs) {
            Ref peeled = repository.getRefDatabase().peel(ref);
            org.eclipse.jgit.lib.ObjectId objectId = peeled.getPeeledObjectId();
            if (objectId == null) {
                objectId = ref.getObjectId();
            }
            if (objectId != null) {
                String id = objectId.getName();
                // Format the ref name like git log decorations:
                //   refs/heads/master        -> master
                //   refs/remotes/origin/master -> origin/master
                String refName = ref.getName();
                if (refName.startsWith("refs/heads/")) {
                    refName = refName.substring("refs/heads/".length());
                } else if (refName.startsWith("refs/remotes/")) {
                    refName = refName.substring("refs/remotes/".length());
                }
                commitRefMap.computeIfAbsent(id, k -> new java.util.ArrayList<>()).add(refName);
            }
        }

        // Walk commits from HEAD (same as git log) and check decorations
        try {
            LogCommand logCmd = git.log();
            Iterable<RevCommit> commits = logCmd.call();
            for (RevCommit commit : commits) {
                String commitId = commit.getId().getName();
                java.util.List<String> refNames = commitRefMap.get(commitId);
                if (refNames != null) {
                    for (String refName : refNames) {
                        Matcher m = pattern.matcher(refName);
                        if (m.find()) {
                            return m.group(1);
                        }
                    }
                }
            }
        } catch (GitAPIException e) {
            throw new IOException("Failed to read git log: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Returns the underlying JGit Git instance for use in commands.
     */
    public Git getGit() {
        return git;
    }

    /**
     * Returns the underlying JGit Repository.
     */
    public Repository getRepository() {
        return repository;
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

    /**
     * Returns the short message of the last commit on the current branch.
     *
     * @return the commit message, or null if it cannot be determined
     * @throws IOException 
     */
    public String getLastCommitMessage() throws IOException {
        try {
            for (RevCommit commit : git.log().setMaxCount(1).call()) {
                return commit.getShortMessage();
            }
        } catch (GitAPIException e) {
        	throw new IOException("Failed to read git log: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Checks whether the current branch has commits that are not reachable
     * from the given base ref. This is used to detect whether there is
     * anything to push / create a merge request for.
     * <p>
     * Resolves the base ref in the following order:
     * <ol>
     *   <li>{@code refs/remotes/origin/<baseRef>}</li>
     *   <li>{@code refs/heads/<baseRef>}</li>
     *   <li>{@code <baseRef>} literally</li>
     * </ol>
     *
     * @param baseRef the branch name to compare against (e.g. "master")
     * @return {@code true} if HEAD has commits not in baseRef
     * @throws IOException on repository errors
     */
    public boolean hasUnpushedCommits(String baseRef) throws IOException {
        org.eclipse.jgit.lib.ObjectId headId = repository.resolve("HEAD");
        if (headId == null) {
            return false;
        }

        // Resolve the base ref
        org.eclipse.jgit.lib.ObjectId baseId = null;
        if (baseRef != null) {
            baseId = repository.resolve("refs/remotes/origin/" + baseRef);
            if (baseId == null) {
                baseId = repository.resolve("refs/heads/" + baseRef);
            }
            if (baseId == null) {
                baseId = repository.resolve(baseRef);
            }
        }

        if (baseId == null) {
            // Cannot resolve base ref — assume there are unpushed commits
            return true;
        }

        if (headId.equals(baseId)) {
            return false;
        }

        // Count commits reachable from HEAD but not from the base ref
        try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            walk.markStart(walk.parseCommit(headId));
            walk.markUninteresting(walk.parseCommit(baseId));
            return walk.iterator().hasNext();
        }
    }
}
