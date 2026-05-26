package net.sf.ecl1.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
    private static final Status dummyStatus = new Status(IStatus.OK, Activator.PLUGIN_ID, "Dummy Status for inner Jobs that share a Multistatus");
    
    
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
		
		
		
		
		
		/*
		es kommt zu filelock wenn ich webapps und angualbase gemeinsame pulle macht webaops dichtf
		es kommt zu filelock wenn ich webapps und angualbase gemeinsame pulle macht webaops dichtf
		es kommt zu filelock wenn ich webapps und angualbase gemeinsame pulle macht webaops dichtf
		
		
		es kommt zu filelock wenn ich webapps und angualbase gemeinsame pulle macht webaops dichtf
		
		
		schon behoen? -scheint so
		*/
		
		
		
		

		
		
		//Registering the job enables the activator to properly shutdown the job when eclipse shuts down
		Activator.getDefault().setGitBatchPullJob(job);
		job.schedule();
		return null;
	}
	
	private MultiStatus schedulePullJobs(IProgressMonitor batchMonitor) {
		/** Stores the result of all scheduled jobs */
		MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, 0, "Problems occured during \"Batch Git Pull Command\"");
		
		List<IProject> projects = Arrays.asList(WorkspaceFactory.getWorkspace().getRoot().getProjects());
		List<Job> jobList = new ArrayList<>();

		String[] projectNames = new String[projects.size()];
		for (int i = 0; i < projectNames.length; i++) {
			projectNames[i] = projects.get(i).getName();
		}
		
		logger.info("Found " + projectNames.length + " projects in Workspace: " + Arrays.toString(projectNames));
		batchMonitor.beginTask("Batch Git Pull", projects.size());
		
		for (IProject p : projects) {
			//Check, if user has requested a cancel
			if(batchMonitor.isCanceled()) {
				multiStatus.add(Status.CANCEL_STATUS);
				batchMonitor.done();
				for (Job job: jobList) {
					if ( job != null ) {
						job.cancel();
						try {
							job.join();
						} catch (InterruptedException e) {
							logger.error("Interrupted while waiting for git batch pull job to cancel");
						}
					}
				}
				return multiStatus;
			}
			
			String name = p.getName();
			File projectLocationFile = p.getLocation().append(".git").toFile();
			//TODO worktree
			if(projectLocationFile.isFile()) {
				multiStatus.add(new Status(IStatus.INFO, Activator.PLUGIN_ID, name + " is managed by git, but you are currently in a linked work tree. Git Batch Pull will not work in a linked work tree. Skipping..."));
				batchMonitor.worked(1);
				continue;
			}

			Job pullJob = new WorkspaceJob("ecl1: Executing \"git pull\" for "+ name) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor innerMonitor) {
					logger.info("Processing " + name + " with location " + projectLocationFile.getAbsolutePath());
					innerMonitor.beginTask("Pulling " + name, IProgressMonitor.UNKNOWN);
					gitPull(innerMonitor, multiStatus, projectLocationFile, name);
					innerMonitor.done();
					logger.info("Finished Processing " + name);
					batchMonitor.worked(1);
					return dummyStatus;
				}
			};
			jobList.add(pullJob);
			Activator.getDefault().appendPullJob(pullJob);
			pullJob.schedule();
		}
		
		for(Job job : jobList) {
			try {
				job.join();
			} catch (InterruptedException e) {
				logger.error("Interrupted while running git batch pull job");
			}
		}
		batchMonitor.done();
		return multiStatus;
	}
	
	
	private void gitPull(IProgressMonitor monitor, MultiStatus multiStatus, File projectLocationFile, String name) {
		if(monitor.isCanceled()) {
			multiStatus.add(Status.CANCEL_STATUS);
			return;
		}
		try (Git git = Git.open(projectLocationFile)){
			try {
				PullCommand pull = git.pull();
				PullResult pullResult = pull.call();
				parsePullResult(name, pullResult, multiStatus);
			} catch (GitAPIException | JGitInternalException e) {
				multiStatus.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Failed to pull " + name + ": " + e.getMessage() + ". Skipping and proceeding."));
			}
		} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
			// ignore
			logger.info(name + " is not managed via Git: " + rnfe.getMessage());
		} catch (IOException e) {
			logger.info(name + " failed: " + e.getMessage());
			multiStatus.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Failed to pull " + name + ": " + e.getMessage()));
		}
	}


	/**
	 * Parse the pullResult and write the result into the multiStatus
	 * 
	 * @param projectName
	 * @param pullResult
	 * @param multiStatus
	 */
	private void parsePullResult(String projectName, PullResult pullResult, MultiStatus multiStatus) {
		Status status;
		if(!pullResult.isSuccessful()) {
			if (pullResult.getMergeResult() != null && !pullResult.getMergeResult().getMergeStatus().isSuccessful()) {
				status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Pull from " + projectName + " was not successful, because the merge failed.");
				multiStatus.add(status);
			}
			if(pullResult.getRebaseResult() != null && !pullResult.getRebaseResult().getStatus().isSuccessful()) {
				status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Pull from " + projectName + " was not successful, because the rebase failed.");
				multiStatus.add(status);
			}
		}
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
