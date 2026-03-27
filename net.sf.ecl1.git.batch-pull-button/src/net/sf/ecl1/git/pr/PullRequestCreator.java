package net.sf.ecl1.git.pr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sf.ecl1.git.Activator;

/**
 * Orchestrates the creation of a Gitlab pull/merge request.
 * <p>
 * This is a port of the functionality in pr.py (Node.js) to Java.
 * It creates a fork if needed, syncs LFS objects, and pushes with merge request options.
 */
public class PullRequestCreator {

    private final GitlabConfig config;
    private final GitlabApi api;
    private final LocalRepository localRepo;
    private final String message;
    private final String targetBranch;
    private final String assignTo;
    private final boolean forceSync;

    /**
     * Parameters for a pull request.
     */
    public static class Params {
        public String message;
        public String targetBranch;
        public String assignTo;
        public boolean forceSync;
    }

    /**
     * Constructs a PullRequestCreator.
     *
     * @param config the Gitlab configuration
     * @param localRepo the local repository
     * @param params the merge request parameters
     */
    public PullRequestCreator(GitlabConfig config, LocalRepository localRepo, Params params) {
        this.config = config;
        this.api = new GitlabApi(config);
        this.localRepo = localRepo;
        this.message = params.message;
        this.targetBranch = params.targetBranch;
        this.assignTo = params.assignTo;
        this.forceSync = params.forceSync;
    }

    /**
     * Executes the pull request creation workflow.
     *
     * @param monitor progress monitor
     * @return status of the operation
     */
    public IStatus execute(IProgressMonitor monitor) {
        try {
            String username = config.getUsername();
            String repo = localRepo.getRepo();

            monitor.beginTask("Creating Merge Request", 5);

            // Step 1: Validate branch
            monitor.subTask("Checking current branch...");
            String branch = localRepo.getBranch();
            if (branch == null || Pattern.compile(config.getBranches()).matcher(branch).find()) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Please create a dedicated branch for your merge request. Current branch is: " + branch);
            }
            monitor.worked(1);

            // Step 2: Create fork if it does not exist
            monitor.subTask("Checking fork...");
            createForkIfNeeded(username, repo);
            monitor.worked(1);

            // Step 3: Determine target branch
            monitor.subTask("Determining target branch...");
            String resolvedTargetBranch = targetBranch;
            if (resolvedTargetBranch == null || resolvedTargetBranch.isEmpty()) {
                resolvedTargetBranch = localRepo.findTargetBranch(config.getBranches());
            }
            monitor.worked(1);

            // Step 4: Sync fork if LFS or forced
            if (localRepo.hasLFS() || forceSync) {
                monitor.subTask("Syncing fork...");
                syncFork(username, repo, "master", forceSync);
                if (resolvedTargetBranch != null && !"master".equals(resolvedTargetBranch)) {
                    syncFork(username, repo, resolvedTargetBranch, forceSync);
                }
            }
            monitor.worked(1);

            // Step 5: Push and create merge request
            monitor.subTask("Pushing and creating merge request...");
            int exitCode = pushWithMergeRequest(username, resolvedTargetBranch);
            monitor.worked(1);

            monitor.done();

            if (exitCode != 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Git push failed with exit code " + exitCode);
            }

            return new Status(IStatus.OK, Activator.PLUGIN_ID, "Merge request created successfully.");

        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Error creating merge request: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, "Operation was cancelled.");
        }
    }

    /**
     * Creates a fork if it doesn't exist and adds the remote.
     */
    private void createForkIfNeeded(String username, String repo) throws IOException, InterruptedException {
        GitlabApi.ForkDetails details = api.getForkDetails(username, repo);

        if (details == null) {
            // Fork does not exist, create it
            api.forkProject(localRepo.getRepoPath(), username);

            // Wait for fork to be ready
            for (int i = 0; i < 120; i++) {
                details = api.getForkDetails(username, repo);
                if (details != null) {
                    break;
                }
                Thread.sleep(1000);
            }

            if (details == null) {
                throw new IOException("Timed out waiting for fork creation.");
            }
        }

        // Add remote if not known
        if (!localRepo.getKnownRemotes().contains(username)) {
            runGitCommand("remote", "add", username,
                    "git@" + config.getServer() + ":" + username + "/" + repo);
        }
    }

    /**
     * Syncs a fork branch and fetches updates.
     */
    private void syncFork(String username, String repo, String syncBranch, boolean forced) throws IOException, InterruptedException {
        api.syncFork(username, repo, syncBranch);

        // Wait for sync to complete
        for (int i = 0; i < 120; i++) {
            GitlabApi.ForkDetails details = api.getForkDetails(username, repo);
            if (details != null && !details.isSyncing) {
                if (details.hasConflicts) {
                    throw new IOException("Cannot refresh fork for branch " + syncBranch + " because of conflicts.");
                }
                break;
            }
            Thread.sleep(1000);
        }

        // Fetch from fork
        runGitCommand("fetch", config.getUsername());
    }

    /**
     * Pushes and creates the merge request using git push options.
     *
     * @return the exit code of the git push command
     */
    private int pushWithMergeRequest(String username, String resolvedTargetBranch) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("push");
        args.add(username);
        args.add("-o");
        args.add("merge_request.create");
        args.add("-o");
        args.add("merge_request.remove_source_branch");

        if (assignTo != null && !assignTo.isEmpty()) {
            args.add("-o");
            args.add("merge_request.assign=" + assignTo);
        }
        if (resolvedTargetBranch != null && !resolvedTargetBranch.isEmpty()) {
            args.add("-o");
            args.add("merge_request.target=" + resolvedTargetBranch);
        }
        if (message != null && !message.isEmpty()) {
            args.add("-o");
            args.add("merge_request.title=" + message);
        }

        return runGitCommand(args.toArray(new String[0]));
    }

    /**
     * Runs a git command in the repository root directory.
     *
     * @return exit code
     */
    private int runGitCommand(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(localRepo.getRepositoryRoot());
        pb.inheritIO();
        Process process = pb.start();
        return process.waitFor();
    }
}