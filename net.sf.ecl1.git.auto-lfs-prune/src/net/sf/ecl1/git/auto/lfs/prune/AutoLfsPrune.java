package net.sf.ecl1.git.auto.lfs.prune;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.ExecutionResult;
import org.eclipse.ui.IStartup;
import net.sf.ecl1.utilities.general.ConsoleLogger;

public class AutoLfsPrune implements IStartup {
    
	private static final ConsoleLogger logger = new ConsoleLogger(AutoLfsPruneActivator.getDefault().getLog(), AutoLfsPruneActivator.PLUGIN_ID, AutoLfsPrune.class.getSimpleName());
	
//	private static int RUN_DELAY = 60000; // =1min
	private static int RUN_DELAY = 86400000; //86400000ms = 1day
    
	public static ExecutionResult runCommandInRepo(String command, Repository repo) throws IOException, InterruptedException {
		FS fs = repo.getFS();
		
		// The fs.runInShell makes sure that the command is run by sh.exe and _not_ by cmd.exe
		ProcessBuilder pb = fs.runInShell(command, new String[0]);
		pb.directory(repo.getWorkTree());
		pb.environment().put(Constants.GIT_DIR_KEY, repo.getDirectory().getAbsolutePath());
	
		return fs.execute(pb, null);
				
	}
    
    @Override
	public void earlyStartup() {
		Job job = new Job("ecl1GitLfsPrune") {			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
				logger.info("Found projects in Workspace: " + projects);
				monitor.beginTask("Batch Git Lfs prune", projects.size());
				
				for (IProject p : projects) {
					/*
					 * Check if the user has requested canceling of pruning
					 */
					if(monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					
					
					String name = p.getName();
					monitor.subTask("Pruning " + name);
					File projectLocationFile = p.getLocation().append(".git").toFile();
					logger.info(name + " with location " + projectLocationFile.getAbsolutePath());
					
					/*
					 * Try to open git-repo of this eclipse-project
					 */
					Repository repo;
					try {
						Git git = Git.open(projectLocationFile);
						repo = git.getRepository();
					} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
						// ignore
						logger.info(name + " is not managed via Git: " + rnfe.getMessage());
						monitor.worked(1);	
						continue;
					} catch (IOException e) {
						logger.error2("Error pruning " + name + ": " + e.getMessage(), e);
						monitor.worked(1);	
						continue;
					}
					
					/*
					 * Run "git stash list" in the git-repo
					 */
					try {
						ExecutionResult er = runCommandInRepo("git stash list", repo);
						
						if (er.getRc() != 0) {
							logger.info("Command: \"git stash list\" returned an error code! Aborting pruning in ALL projects in the workspace.");
							logger.info("Most likely cause for the error code: \"git\" command is not available on your console.");
							logger.info("This can be fixed by installing git in your console.");
							return Status.OK_STATUS;
						}

						if (er.getStdout().length() != 0) {
							// If "git stash list" has output --> stashes present in this repo --> We cannot prune
							logger.info("Found stashes in project: " + name + ". Cannot run \"git lfs prune\" when stashes are present! Aborting..."
									+ "\nYou can run \"git stash clear\" to manually delete your stashes. ");
							monitor.worked(1);
							continue;
						}
						
					} catch (IOException | InterruptedException e) {
						logger.error2("Failed to run \"git stash list\" command in project: "+ name);
						logger.error2("Error message: " + e.getMessage(),e);
						monitor.worked(1);	
						continue;
					} 
					
					/*
					 * If we reached this part --> No stashes present and project hat a ./git-folder --> Run prune command
					 */
					try {
						ExecutionResult er = runCommandInRepo("git lfs prune", repo);
						
						if (er.getRc() != 0) {
							logger.error2("Command: \"git lfs prune\" returned an error code! Prune attempt was not successful...");
							logger.error2("Output of the error stream: " + new String(er.getStderr().toByteArray()));
							monitor.worked(1);	
							continue;
						}
						
					} catch (IOException | InterruptedException e) {
						logger.error2("Failed to run \"git lfs prune\" command in the following project: "+ name);
						logger.error2("Error message: " + e.getMessage(),e);
						monitor.worked(1);	
						continue;
					} 
					
					
					
					
					logger.info("Succesfully finished pruning: " + name);
					monitor.worked(1);	
				}
				
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		
		

		/*
		 * What does this do?
		 * Reschedule "git lfs prune" once a day. 
		 * 
		 * Why is this necessary?
		 * At least one user is leaving eclipse open for a very long time (multiple days). This user
		 * wants eclipse to run "git lfs prune" once a day automatically. 
		 * 
		 * This thread will never terminated. Isn't there a better way to schedule an indefinitely repeating job?
		 * The officially recommended way of implementing a repeating background task is described here: 
		 * https://wiki.eclipse.org/FAQ_How_do_I_create_a_repeating_background_task%3F
		 * Using this approach would also keep the thread alive indefinitely. Therefore there is no 
		 * difference resource-wise between the recommended approach and the approach we have chosen here. 
		 *  
		 * The recommended approach has one downside, however: 
		 * The progressbar for this job will not disappear and will instead indicate a sleeping thread to the user.  
		 * This is correct (since the thread is indeed sleeping), but some users (including me) did not like the indication
		 * of a sleeping thread that never goes away (actually I discovered, that it is no longer displayed, when closing and 
		 * re-opening the progress-view, but some users never close the progress-view and therefore the sleeping indication never goes away).
		 * By finishing the old job, the progress bar is completely discarded and no sleeping thread is indicated to the user.  
		 */
		while(true) {
			try {
				Thread.sleep(RUN_DELAY);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			job.schedule();
		}
			

	}
}
