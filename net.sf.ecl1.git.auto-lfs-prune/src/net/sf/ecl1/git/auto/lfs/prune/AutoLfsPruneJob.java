package net.sf.ecl1.git.auto.lfs.prune;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.ExecutionResult;

import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

public class AutoLfsPruneJob extends Job {
	
    private static final ICommonLogger logger = LoggerFactory.getLogger(AutoLfsPruneJob.class.getSimpleName(), AutoLfsPruneActivator.PLUGIN_ID, AutoLfsPruneActivator.getDefault());
    	
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

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<IProject> projects = Arrays.asList(WorkspaceFactory.getWorkspace().getRoot().getProjects());
		String[] projectNames = new String[projects.size()];
		for (int i = 0; i < projectNames.length; i++) {
			projectNames[i] = projects.get(i).getName();
		}
		logger.info("Found " + projectNames.length + " projects in Workspace: " + Arrays.toString(projectNames));
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
			File gitFile = p.getLocation().append(".git").toFile();
			File projectRoot = p.getLocation().toFile();
			
			if (gitFile.isDirectory()) {
				logger.info(name + " with location " + projectRoot.getAbsolutePath());
			}else {
				logger.info(name + "(worktree) with location " + projectRoot.getAbsolutePath());
			}
			
			/*
			 * Try to open git-repo of this project
			 */
		    try {
		    	Repository repository = new FileRepositoryBuilder()
		            .setWorkTree(projectRoot)
		            .readEnvironment()
		            .findGitDir(projectRoot)
		            .build();
		    	
				try {
					ExecutionResult er = runCommandInRepo("git lfs prune", repository);
					
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
			} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
				// ignore
				logger.info(name + " is not managed via Git: " + rnfe.getMessage());
				monitor.worked(1);	
			} catch (IOException e) {
				logger.error2("Error pruning " + name + ": " + e.getMessage(), e);
				monitor.worked(1);	
			}
		}

		monitor.done();
		return Status.OK_STATUS;
	}
}
