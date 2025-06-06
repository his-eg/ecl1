package net.sf.ecl1.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

	@Override
	public Object execute(ExecutionEvent event) {
		
		logger.info("Starting ecl1GitBatchPull");

		// standalone
		if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
			GitUtil.setupStandaloneSsh();
			IStatus multiStatus = gitBatchPullJob(new NullProgressMonitor());
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
				IStatus multiStatus = gitBatchPullJob(monitor);
				return displayResultStatus(multiStatus);
			}
		};
		
		//Registering the job enables the activator to properly shutdown the job when eclipse shuts down
		Activator.getDefault().setGitBatchPullJob(job);
		job.schedule();
		return null;
	}

	private IStatus gitBatchPullJob(IProgressMonitor monitor) {
		/** Stores the result of this job */
		MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, 0, "Problems occured during \"Batch Git Pull Command\"");
		Status status;

		List<IProject> projects = Arrays.asList(WorkspaceFactory.getWorkspace().getRoot().getProjects());
		//Unnecessary after finishing #267325. The problem definition for #250882 was flawed and therefore sort was 
		//never really needed by the user. Even though it is pointless, it stays in, because it doesn't do any harm...
		Collections.sort(projects, projectComparator);
		String[] projectNames = new String[projects.size()];
		for (int i = 0; i < projectNames.length; i++) {
			projectNames[i] = projects.get(i).getName();
		}
		logger.info("Found " + projectNames.length + " projects in Workspace: " + Arrays.toString(projectNames));
		monitor.beginTask("Batch Git Pull", projects.size());
		for (IProject p : projects) {
			//Check, if user has requested a cancel
			if(monitor.isCanceled()) {
				multiStatus.add(Status.CANCEL_STATUS);
				return displayResultStatus(multiStatus);
			}
			
			String name = p.getName();
			monitor.subTask("Pulling " + name);
			File projectLocationFile = p.getLocation().append(".git").toFile();
			logger.info("Processing " + name + " with location " + projectLocationFile.getAbsolutePath());
			
			if(projectLocationFile.isFile()) {
				status = new Status(IStatus.INFO, Activator.PLUGIN_ID, name + " is managed by git, but you are currently in a linked work tree. Git Batch Pull will not work in a linked work tree. Skipping...");
				multiStatus.add(status);
				monitor.worked(1);
				continue;
			}

			try (Git git = Git.open(projectLocationFile)){
				try {
					PullCommand pull = git.pull();
					PullResult pullResult = pull.call();
					parsePullResult(name, pullResult, multiStatus);
				} catch (GitAPIException | JGitInternalException e) {
					status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Failed to pull " + name + ": " + e.getMessage() + ". Skipping and proceeding.");
					multiStatus.add(status);
				}
			} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
				// ignore
				logger.info(name + " is not managed via Git: " + rnfe.getMessage());
			} catch (IOException e) {
				status = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Failed to pull " + name + ": " + e.getMessage());
				multiStatus.add(status);
			}
			logger.info("Finished " + name);
			monitor.worked(1);
		}				
		monitor.done();
		return multiStatus;
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
	
	/**
	 * 
	 * Sorts the projects according to the following order: 
	 *  
	 * 1. *.api
     * 2. webapps
     * 3. cs.* ohne *.dbschema.*, ohne *.frontend
     * 4. cm.*, fs.*, rm.* jeweils ohne *.frontend
     * 5. *.frontend
     * 6. *.dbschema.*
     * 7. rest
	 * 
	 * Sorting the projects in this order speeds up the build-process. 
	 * 
	 */
	Comparator<IProject> projectComparator = new Comparator<IProject>() {
		
		private int getPriority(IProject p) {
			final String api = ".api";
			final String webapps = "webapps";
			final String cs = "cs.";
			final String frontend = ".frontend";
			final String cm = "cm.";
			final String fs = "fs.";
			final String rm = "rm.";
			final String dbschema = ".dbschema.";
			
			String pName = p.getName();
			if(pName.endsWith(api)) {
				return 0;
			}
			
			if(pName.matches(webapps)) {
				return 1;
			}
			
			if(pName.startsWith(cs) && !(pName.contains(dbschema) || pName.endsWith(frontend))) {
				return 2;
			}
			
			if( (pName.startsWith(cm) || pName.startsWith(fs) || pName.startsWith(rm)) && !pName.endsWith(frontend)) {
				return 3;
			}
			
			if(pName.endsWith(frontend)) {
				return 4;
			}  
			
			if(pName.contains(dbschema)) {
				return 5;
			}
			
			return 6;
		}
		
		
		@Override
		public int compare(IProject p1, IProject p2) {
			return getPriority(p1) - getPriority(p2);
		}		
		
	};
	
}
