package net.sf.ecl1.git.lfs.prune;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * 
 * Executes a git lfs prune on all open projects 
 * 
 * @author sohrt@his.de
 */
public class GitLfsPruneHandler extends AbstractHandler {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID,
			GitLfsPruneHandler.class.getSimpleName());

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		logger.info("Starting ecl1GitLfsPrune");
		Job job = new Job("ecl1GitLfsPrune") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
				logger.info("Found projects in Workspace: " + projects);
				monitor.beginTask("Batch Git Lfs prune", projects.size());
				for (IProject p : projects) {
					String name = p.getName();
					monitor.subTask("Pruning " + name);
					File projectLocationFile = p.getLocation().append(".git").toFile();
					logger.info(name + " with location " + projectLocationFile.getAbsolutePath());

					try {
						Git git = Git.open(projectLocationFile);
						Repository repo = git.getRepository();
						FS fs = repo.getFS();

						// Tell the processBuilder which command to run; namely "git stash list"
						// The fs.runInShell makes sure that the command is run by sh.exe and _not_ by cmd.exe
						ProcessBuilder pb = fs.runInShell("git stash list", new String[0]);
						pb.directory(repo.getWorkTree());
						pb.environment().put(Constants.GIT_DIR_KEY, repo.getDirectory().getAbsolutePath());

						try {
							ExecutionResult er = fs.execute(pb, null);
							if (er.getRc() != 0) {
								logger.error2("Command: \"git stash list\" returned an error code! Aborting prune attempt.");
								logger.error2("Output of the error stream: " + new String(er.getStderr().toByteArray()));
								continue;
							}

							if (er.getStdout().length() != 0) {
								// If "git stash list" has output --> stashes present in this repo --> We cannot prune
								logger.error2("Found stashes in the repository. Cannot run \"git lfs prune\" when stashes are present! Aborting..."
												+ "\n You can run \"git stash clear\" to manually delete your stashes. ");
								continue;
							}

							// Tell the processBuilder which command to run; namely "git lfs prune"
							// The fs.runInShell makes sure that the command is run by sh.exe and _not_ by cmd.exe
							pb = fs.runInShell("git lfs prune", new String[0]);
							pb.directory(repo.getWorkTree());
							pb.environment().put(Constants.GIT_DIR_KEY, repo.getDirectory().getAbsolutePath());

							er = fs.execute(pb, null);

							if (er.getRc() != 0) {
								logger.error2("Command: \"git lfs prune \" returned an error code! Prune attempt was not successful...");
								logger.error2("Output of the error stream: " + new String(er.getStderr().toByteArray()));
								continue;
							}

							logger.info("Successfully pruned the repo");

						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
						// ignore
						logger.info(name + " is not managed via Git: " + rnfe.getMessage());
					} catch (IOException e) {
						logger.error2("Error pruning " + name + ": " + e.getMessage(), e);
					}

					logger.info("Finished: " + name);
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
}
