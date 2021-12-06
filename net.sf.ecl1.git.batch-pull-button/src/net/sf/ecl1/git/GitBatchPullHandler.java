package net.sf.ecl1.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * Executes a pull command on all open projects using Git as SCM
 *  
 * @author keunecke
 */
public class GitBatchPullHandler extends AbstractHandler {
		
	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, GitBatchPullHandler.class.getSimpleName());
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		logger.info("Starting ecl1GitBatchPull");
		Job job = new WorkspaceJob("ecl: Executing \"git pull\" for all git versioned projects in the workspace.") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
				//Fixes #250882
				Collections.sort(projects, projectComparator);
				logger.info("Found projects in Workspace: " + projects);
				monitor.beginTask("Batch Git Pull", projects.size());
				for (IProject p : projects) {
					//Check, if user has requested a cancel
					if(monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					
					String name = p.getName();
					monitor.subTask("Pulling " + name);
					File projectLocationFile = p.getLocation().append(".git").toFile();
					logger.info(name + " with location " + projectLocationFile.getAbsolutePath());
					
					if(projectLocationFile.isFile()) {
						logger.info(name + " is managed by git, but you are currently in a linked work tree. Git Batch Pull will not work in a linked work tree. Skipping...");
						monitor.worked(1);
						continue;
					}

					try {
						Git git = Git.open(projectLocationFile);
						logger.info("Getting remotes of " + name);
						Set<String> remotes = git.getRepository().getRemoteNames();
						if(remotes != null && !remotes.isEmpty()) {
							for (String remote : remotes) {
								try {
									PullCommand pull = git.pull();
									pull.setRemote(remote);
									logger.info(name + " has remotes. Starting to pull remote '" + remote + "'.");
									pull.call();
								} catch (GitAPIException | JGitInternalException e) {
									logger.error2("Error pulling from " + name + ": " + e.getMessage() + ". Skipping and proceeding.", e);
								}
							}
						}
					} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
						// ignore
						logger.info(name + " is not managed via Git: " + rnfe.getMessage());
					} catch (IOException e) {
						logger.error2("Error pulling " + name + ": " + e.getMessage(), e);
					}
					logger.info("Finished " + name);
					monitor.worked(1);
				}				
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		//Registering the job enables the activator to properly shutdown the job when eclipse shuts down
		Activator.getDefault().setGitBatchPullJob(job);
		job.schedule();
		return null;
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
