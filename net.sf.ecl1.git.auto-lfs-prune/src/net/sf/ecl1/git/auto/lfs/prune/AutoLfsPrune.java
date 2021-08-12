package net.sf.ecl1.git.auto.lfs.prune;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

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
import org.eclipse.jgit.util.FS.FSFactory;
import org.eclipse.jgit.util.FS_Win32;
import org.eclipse.ui.IStartup;
import net.sf.ecl1.utilities.general.ConsoleLogger;

public class AutoLfsPrune implements IStartup, Runnable {
    
	private static final ConsoleLogger logger = new ConsoleLogger(AutoLfsPruneActivator.getDefault().getLog(), AutoLfsPruneActivator.PLUGIN_ID, AutoLfsPrune.class.getSimpleName());
	
//	private static int RUN_DELAY = 60000; // = 1min
	private static int RUN_DELAY = 86400000; //86400000ms = 1day
	
	/** git lfs version 2.13.3 fixed a bug (see: https://github.com/git-lfs/git-lfs/issues/4401) that prohibited
	 *  running the command "git lfs prune" when stashes were present on windows machines...
	 */
	private static final int LFS_VERSION_WITH_BUGFIX = 2133;
    
	public static ExecutionResult runCommandInRepo(String command, Repository repo) throws IOException, InterruptedException {
		FS fs = repo.getFS();
		
		ProcessBuilder pb = fs.runInShell(command, new String[0]);
		pb.directory(repo.getWorkTree());
		pb.environment().put(Constants.GIT_DIR_KEY, repo.getDirectory().getAbsolutePath());
	
		return fs.execute(pb, null);
				
	}
    
    static public int parseGitLfsVersion(InputStream stream) throws ParseException {
    	//Pattern matches a semantic version number (for example: 2.13.3)
    	Pattern p = Pattern.compile("\\d*\\.\\d*\\.\\d*");
    	try(Scanner scanner = new Scanner(stream);) {
        	//We assume that the version of git lfs is in the first line and is the first version number in this line
        	String versionAsString = scanner.findInLine(p);
        	if (versionAsString == null) {
        		throw new ParseException("Exception occured while trying to parse the version of git lfs. One possible reason: git lfs is not installed on the machine.",0);
        	}
        	//Convert string to int
        	versionAsString = versionAsString.replace(".", "");
        	return Integer.parseInt(versionAsString);
    	}
    }
	
	@Override
	public void earlyStartup() {
    	/*
    	 * We start a separate thread for this plugin, because we want to schedule 
    	 * running "auto lfs prune" once every day. Scheduling is achieved by putting the "auto lfs prune" thread
    	 * to sleep for a day. By creating a separate thread, we won't put the thread to sleep 
    	 * that is responsible for executing all other early startup-plugins. 
    	 */
    	new Thread(this,"Auto lfs prune thread").start();
	}

	private void printFailureMessage(String command) {
		logger.info("Command: \"" + command + "\" returned an error code or an exception occured! Aborting pruning in ALL projects in the workspace.");
		logger.info("Most likely cause for the error code: \"git\" command is not available in your console.");
		logger.info("This can be fixed by installing git in your console.");
	}
	
	@Override
	public void run() {
		Job job = new Job("ecl1GitLfsPrune") {			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				//Detect filesystem (windows or POSIX)
				FS fs = FS.detect();
				boolean detectedLFSVersion = false;
				int LFSVersion = 0;
				
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
					 * Detect if this work tree is a linked work tree --> Gracefully abort if a linked work tree is detected
					 */
					if(projectLocationFile.isFile()) {
						logger.info(name + " is managed by git, but you are currently in a linked work tree. Auto lfs pruning will not work in a linked work tree. Skipping...");
						monitor.worked(1);
						continue;
					}
					
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
					 * Perform necessary checks for windows machines
					 * 
					 */
					if(fs instanceof FS_Win32) {
						
						/*
						 * Detect version of git lfs. 
						 * 
						 * We need the version number of git lfs, because if stashes are present on windows machines, we can only run "gif lfs prune", if "git lfs version" is bigger or equal
						 * to version 2.13.3 because of this bug: https://github.com/git-lfs/git-lfs/issues/4401 that was fixed in 2.13.3
						 * 
						 */
						if(detectedLFSVersion == false) {
							
							try {
								ExecutionResult er = runCommandInRepo("git lfs version",repo);
								
								if (er.getRc() != 0) {
									printFailureMessage("git lfs version");
									return Status.OK_STATUS;
								}
								
								
								try {
									LFSVersion = parseGitLfsVersion(er.getStdout().openInputStream());
								} catch (ParseException e) {
									printFailureMessage("git lfs version");
									return Status.OK_STATUS;
								}
								detectedLFSVersion = true;

							
							} catch (IOException | InterruptedException e) {
								logger.error2("Failed to run \"git lfs version\" command in the following project: "+ name);
								logger.error2("Error message: " + e.getMessage(),e);
								return Status.OK_STATUS;
							} 
							
						}
						

						
						/*
						 * Run "git stash list" in the git-repo
						 */
						if(LFSVersion < LFS_VERSION_WITH_BUGFIX) {
							try {
								ExecutionResult er = runCommandInRepo("git stash list", repo);
								
								if (er.getRc() != 0) {
									printFailureMessage("git stash list");
									return Status.OK_STATUS;
								}

								if (er.getStdout().length() != 0) {
									// If "git stash list" has output --> stashes present in this repo --> We cannot prune
									logger.info("Found stashes in project: " + name + ". Cannot run \"git lfs prune\" when stashes are present and git lfs version is < 2.13.3! Aborting..."
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
						}
					}
					
					
					
					
					/*
					 * If we reached this part, we can prune!
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
					
					
					
					
					logger.info("Successfully finished pruning: " + name);
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
		 * This thread will never terminate. Isn't there a better way to schedule an indefinitely repeating job?
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
