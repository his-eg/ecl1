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
import org.eclipse.jgit.util.FS_Win32;
import org.eclipse.jgit.util.FS.ExecutionResult;

import net.sf.ecl1.utilities.general.ConsoleLogger;

public class AutoLfsPruneJob extends Job {
	
	private final static ConsoleLogger logger = new ConsoleLogger(AutoLfsPruneActivator.getDefault().getLog(), AutoLfsPruneActivator.PLUGIN_ID, AutoLfsPruneJob.class.getSimpleName());
	
	/** git lfs version 2.13.3 fixed a bug (see: https://github.com/git-lfs/git-lfs/issues/4401) that prohibited
	 *  running the command "git lfs prune" when stashes were present on windows machines...
	 */
	private static final String LFS_VERSION_WITH_BUGFIX = "2.13.3";
	
	
	public AutoLfsPruneJob() {
		super("ecl1: Pruning all git versioned projects in the workspace");
	}
	
	private ExecutionResult runCommandInRepo(String command, Repository repo) throws IOException, InterruptedException {
		FS fs = repo.getFS();
		
		ProcessBuilder pb = fs.runInShell(command, new String[0]);
		pb.directory(repo.getWorkTree());
		pb.environment().put(Constants.GIT_DIR_KEY, repo.getDirectory().getAbsolutePath());
	
		return fs.execute(pb, null);
				
	}
    
	/**
	 * Taken from here: https://www.baeldung.com/java-comparing-versions
	 * 
	 * @param version1
	 * @param version2
	 * @return
	 */
	private int compareVersions(String version1, String version2) {
	    int comparisonResult = 0;
	    
	    String[] version1Splits = version1.split("\\.");
	    String[] version2Splits = version2.split("\\.");
	    int maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length);

	    for (int i = 0; i < maxLengthOfVersionSplits; i++){
	        Integer v1 = i < version1Splits.length ? Integer.parseInt(version1Splits[i]) : 0;
	        Integer v2 = i < version2Splits.length ? Integer.parseInt(version2Splits[i]) : 0;
	        int compare = v1.compareTo(v2);
	        if (compare != 0) {
	            comparisonResult = compare;
	            break;
	        }
	    }
	    return comparisonResult;
	}
	
    private String parseGitLfsVersion(InputStream stream) throws ParseException {
    	//Pattern matches a semantic version number (for example: 2.13.3)
    	Pattern p = Pattern.compile("\\d*\\.\\d*\\.\\d*");
    	try(Scanner scanner = new Scanner(stream);) {
        	//We assume that the version of git lfs is in the first line and is the first version number in this line
        	String version = scanner.findInLine(p);
        	if (version == null) {
        		throw new ParseException("Exception occured while trying to parse the version of git lfs. One possible reason: git lfs is not installed on the machine.",0);
        	}
        	return version;
    	}
    }
    
	private void printFailureMessage(String command) {
		logger.info("Command: \"" + command + "\" returned an error code or an exception occured! Aborting pruning in ALL projects in the workspace.");
		logger.info("Most likely cause for the error code: \"git\" command is not available in your console.");
		logger.info("This can be fixed by installing git in your console.");
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		//Detect filesystem (windows or POSIX)
		FS fs = FS.detect();
		boolean detectedLFSVersion = false;
		String LFSVersion = "0";
		
		List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		logger.info("Found projects in Workspace: " + projects);
		monitor.beginTask("Pruning", projects.size());
		
		for (IProject p : projects) {
			/*
			 * Check if the user has requested canceling of pruning
			 */
			if(monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			
			String name = p.getName();
			monitor.subTask(name);
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
				if( compareVersions(LFSVersion, LFS_VERSION_WITH_BUGFIX) <= 0 ) {
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

}
