package net.sf.ecl1.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.ui.PlatformUI;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;
import net.sf.ecl1.utilities.general.SwtUtil;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Executes a pull command on all open projects using Git as SCM
 *  
 * @author keunecke
 */
public class GitBatchPullHandler extends AbstractHandler {
		
    private static final ICommonLogger logger = LoggerFactory.getLogger(GitBatchPullHandler.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());
    private static final int MAX_PARALLEL_PULLS = 10;
    /**
     * Pull large projects first.
     * List order defines pull order.
    */
    private static final List<String> PRIORITY_PROJECTS = List.of("webapps", "cs.sys.dbschema.hisinone");
    
	@Override
	public Object execute(ExecutionEvent event) {
		
		logger.info("Starting ecl1GitBatchPull");

		// standalone
		if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
			GitUtil.setupStandaloneSsh();
			IStatus multiStatus = schedulePullJobs(new NullProgressMonitor());
			if (PreferenceWrapper.isDisplaySummaryOfGitPull()) {
				Display display = new Display();
				SwtUtil.bringShellToForeground(display);
				GitBatchPullSummaryErrorDialog errorDialog = new GitBatchPullSummaryErrorDialog(display.getActiveShell(), multiStatus);
				Image icon = new Image(null, GitBatchPullHandler.class.getResourceAsStream("/ecl1_icon.png"));
				GitBatchPullSummaryErrorDialog.setDefaultImage(icon);
				errorDialog.open();
			}
			return null;
		}

		Job job = new WorkspaceJob("ecl1: Executing \"git pull\" for all git versioned projects in the workspace.") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				IStatus multiStatus = schedulePullJobs(monitor);
				return displayResultStatus(multiStatus);
			}
		};
		
		//Registering the job enables the activator to properly shutdown the job when eclipse shuts down
		Activator.getDefault().setGitBatchPullJob(job);
		job.schedule();
		return null;
	}
	
	/**
	 * Returns the order in which the project should be pulled.
	 * Projects configured in {@link #PRIORITY_PROJECTS} are pulled first.
	 * @param project
	 * @return configured priority or the lowest priority for normal projects
	 */
	private int getPullOrder(IProject project) {
		int prioIndex = PRIORITY_PROJECTS.indexOf(project.getName());
		if(prioIndex >= 0) {
			return prioIndex;
		}
		// Normal projects without configured priority
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Returns accessible projects in pull order.
	 */
	private List<IProject> getProjectsForPull() {
		List<IProject> projects = new ArrayList<>();
		for (IProject project : WorkspaceFactory.getWorkspace().getRoot().getProjects()) {
			if (project.isAccessible() && project.getLocation() != null) {
				projects.add(project);
			}
		}
		// Pull priority projects first and sort remaining alphabetically
		projects.sort(Comparator.comparingInt(this::getPullOrder).thenComparing(IProject::getName));
		return projects;
	}
	
	/**
	 * Creates the throttled pull job group.
	 */
	private JobGroup createPullJobGroup(int jobCount) {
		return new JobGroup(
				"ecl1: Git Batch Pull",
				MAX_PARALLEL_PULLS,
				jobCount) {

			@Override
			protected boolean shouldCancel(
					IStatus lastCompletedJobResult,
					int numberOfFailedJobs,
					int numberOfCanceledJobs) {

				// A failed pull must not cancel the remaining pulls.
				return false;
			}
		};
	}
	
	
	/**
	 * Schedules and collects all pull jobs.
	 */
	private MultiStatus schedulePullJobs(IProgressMonitor batchMonitor) {
		MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, 0,
				"Problems occurred during \"Batch Git Pull Command\"");
	
		List<IProject> projects = getProjectsForPull();
		List<String> projectNames = new ArrayList<>();
	
		for (IProject project : projects) {
			projectNames.add(project.getName());
		}
	
		logger.info("Found " + projects.size() + " projects in Workspace: " + projectNames);
	
		batchMonitor.beginTask("Batch Git Pull", projects.size());
	
		if (projects.isEmpty()) {
			batchMonitor.done();
			return multiStatus;
		}
	
		if (batchMonitor.isCanceled()) {
			multiStatus.add(Status.CANCEL_STATUS);
			batchMonitor.done();
			return multiStatus;
		}
	
		JobGroup pullJobGroup = createPullJobGroup(projects.size());
	
		for (IProject project : projects) {
			Job pullJob = createPullJob(project);
			pullJob.setJobGroup(pullJobGroup);	
			Activator.getDefault().appendPullJob(pullJob);
			pullJob.schedule();
		}
	
		try {
			pullJobGroup.join(0, batchMonitor);
		} catch (OperationCanceledException e) {
			pullJobGroup.cancel();
			waitForPullJobs(pullJobGroup);
			multiStatus.add(Status.CANCEL_STATUS);
		} catch (InterruptedException e) {
			pullJobGroup.cancel();
			waitForPullJobs(pullJobGroup);
	
			multiStatus.add(new Status(IStatus.CANCEL, Activator.PLUGIN_ID,
					"Interrupted while running git batch pull jobs.", e));
	
			Thread.currentThread().interrupt();
		} finally {
			batchMonitor.done();
		}
	
		MultiStatus pullResult = pullJobGroup.getResult();
	
		if (pullResult != null) {
			multiStatus.merge(pullResult);
		}
	
		return multiStatus;
	}


	/**
	 * Creates a pull job for one project.
	 */
	private Job createPullJob(IProject project) {
		String name = project.getName();
		File projectRoot = project.getLocation().toFile();
	
		boolean isWorktree = project.getLocation().append(".git").toFile().isFile();
	
		return new WorkspaceJob(
				"ecl1: Executing \"git pull\" for " + name) {
	
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				if (isWorktree) {
					logger.info("Processing " + name + " (worktree) with location "
									+ projectRoot.getAbsolutePath());
				} else {
					logger.info("Processing " + name + " with location "
									+ projectRoot.getAbsolutePath());
				}
	
				monitor.beginTask("Pulling " + name, IProgressMonitor.UNKNOWN);
	
				try {
					return gitPull(monitor, projectRoot, name);
				} finally {
					monitor.done();
					logger.info("Finished Processing " + name);
				}
			}
		};
	}

	/**
	 * Waits until all canceled pull jobs have finished.
	 */
	private void waitForPullJobs(JobGroup pullJobGroup) {
		try {
			pullJobGroup.join(0, null);
		} catch (InterruptedException e) {
			logger.error("Interrupted while waiting for git pull jobs to finish");
			Thread.currentThread().interrupt();
		}
	}


	/**
	 * Pulls one Git repository.
	 */
	private IStatus gitPull(IProgressMonitor monitor,File projectRoot, String name) {

		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		try (Repository repository = new FileRepositoryBuilder()
						.setWorkTree(projectRoot)
						.readEnvironment()
						.findGitDir(projectRoot)
						.build();
				Git git = new Git(repository)) {

			PullResult pullResult = git.pull().call();
			return parsePullResult(name, pullResult);

		} catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
			logger.info(name + " is not managed via Git: " + e.getMessage());
			return Status.OK_STATUS;
		} catch (GitAPIException | JGitInternalException e) {
			return new Status(IStatus.WARNING, Activator.PLUGIN_ID,
					"Failed to pull " + name + ": " + e.getMessage() + ". Skipping and proceeding.", e);
		} catch (IOException e) {
			logger.info(name + " failed: " + e.getMessage());
			return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Failed to pull " + name + ": " + e.getMessage(), e);
		}
	}


	/**
	 * Creates the status for a pull result.
	 */
	private IStatus parsePullResult(
			String projectName,
			PullResult pullResult) {

		if (pullResult.isSuccessful()) {
			return Status.OK_STATUS;
		}

		MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, 0, "Pull from " + projectName + " was not successful.");

		boolean reasonFound = false;

		if (pullResult.getMergeResult() != null
				&& !pullResult.getMergeResult().getMergeStatus().isSuccessful()) {

			result.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Pull from " + projectName + " was not successful because the merge failed."));
			reasonFound = true;
		}

		if (pullResult.getRebaseResult() != null
				&& !pullResult.getRebaseResult()
						.getStatus()
						.isSuccessful()) {

			result.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Pull from " + projectName + " was not successful because the rebase failed."));
			reasonFound = true;
		}

		if (!reasonFound) {
			result.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID," Pull from " + projectName + " was not successful."));
		}

		return result;
	}
	
	/**
	 * Creates a dialog that summarizes the result of the git batch pull
	 * 
	 * @param result
	 * @return
	 */
	private IStatus displayResultStatus(IStatus result) {
		//Jobs are running outside of the UI thread and therefore cannot display anything to the user themselves.
		//--> Create the runnable to display the result
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (PreferenceWrapper.isDisplaySummaryOfGitPull()) {
					new GitBatchPullSummaryErrorDialog(result).open();
				}
			}
		});
		return result;
	}
}
