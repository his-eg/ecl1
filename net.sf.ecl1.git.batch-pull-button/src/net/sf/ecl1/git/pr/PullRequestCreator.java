package net.sf.ecl1.git.pr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;

import net.sf.ecl1.git.Activator;

/**
 * Orchestrates the creation of a Gitlab pull/merge request.
 * <p>
 * This is a port of the functionality in pr.py (Node.js) to Java.
 * It creates a fork if needed, syncs LFS objects, and pushes with merge request options.
 * All git operations use JGit internally instead of spawning external git processes.
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

            monitor.beginTask("Creating Merge Request", 4);

            // Step 1: Create fork if it does not exist
            monitor.subTask("Checking fork...");
            createForkIfNeeded(username, repo);
            monitor.worked(1);

            // Step 2: Determine target branch
            monitor.subTask("Determining target branch...");
            String resolvedTargetBranch = targetBranch;
            if (resolvedTargetBranch == null || resolvedTargetBranch.isEmpty()) {
                resolvedTargetBranch = localRepo.findTargetBranch(config.getBranches());
            }
            monitor.worked(1);

            // Step 3: Sync fork if LFS or forced
            if (localRepo.hasLFS() || forceSync) {
                monitor.subTask("Syncing fork...");
                syncFork(username, repo, "master", forceSync);
                if (resolvedTargetBranch != null && !"master".equals(resolvedTargetBranch)) {
                    syncFork(username, repo, resolvedTargetBranch, forceSync);
                }
            }
            monitor.worked(1);

            // Step 4: Push and create merge request
            monitor.subTask("Pushing and creating merge request...");
            pushWithMergeRequest(username, resolvedTargetBranch);
            monitor.worked(1);

            monitor.done();

            return new Status(IStatus.OK, Activator.PLUGIN_ID, "Merge request created successfully.");

        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Error creating merge request: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Status(IStatus.CANCEL, Activator.PLUGIN_ID, "Operation was cancelled.");
        } catch (GitAPIException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Git operation failed: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                    "Invalid remote URI: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a fork if it doesn't exist and adds the remote using JGit.
     */
    private void createForkIfNeeded(String username, String repo) throws IOException, InterruptedException, GitAPIException, URISyntaxException {
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
            Git git = localRepo.getGit();
            git.remoteAdd()
                    .setName(username)
                    .setUri(new URIish("git@" + config.getServer() + ":" + username + "/" + repo))
                    .call();
        }
    }

    /**
     * Syncs a fork branch via the Gitlab API and fetches updates using JGit.
     */
    private void syncFork(String username, String repo, String syncBranch, boolean forced) throws IOException, InterruptedException, GitAPIException {
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

        // Fetch from fork using JGit
        Git git = localRepo.getGit();
        git.fetch()
                .setRemote(config.getUsername())
                .call();
    }

    /**
     * Pushes and creates the merge request using JGit push with push options.
     *
     * @throws GitAPIException on JGit errors
     * @throws IOException on other errors
     */
    private void pushWithMergeRequest(String username, String resolvedTargetBranch) throws GitAPIException, IOException {
        List<String> pushOptions = new ArrayList<>();
        pushOptions.add("merge_request.create");
        pushOptions.add("merge_request.remove_source_branch");

        if (assignTo != null && !assignTo.isEmpty()) {
            pushOptions.add("merge_request.assign=" + assignTo);
        }
        if (resolvedTargetBranch != null && !resolvedTargetBranch.isEmpty()) {
            pushOptions.add("merge_request.target=" + resolvedTargetBranch);
        }
        if (message != null && !message.isEmpty()) {
            pushOptions.add("merge_request.title=" + message);
        }

        Git git = localRepo.getGit();
        Iterable<PushResult> results = git.push()
                .setRemote(username)
                .setPushOptions(pushOptions)
                .call();

        // Check for errors in push results
        for (PushResult result : results) {
            Collection<RemoteRefUpdate> updates = result.getRemoteUpdates();
            for (RemoteRefUpdate update : updates) {
                RemoteRefUpdate.Status status = update.getStatus();
                if (status != RemoteRefUpdate.Status.OK
                        && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                    throw new IOException("Git push failed for ref " + update.getRemoteName()
                            + ": " + status + (update.getMessage() != null ? " - " + update.getMessage() : ""));
                }
            }
        }
    }
}