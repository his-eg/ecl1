package net.sf.ecl1.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		logger.info("Starting ecl1GitBatchPull");
		Job job = new Job("ecl1GitBatchPull") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
				logger.info("Found projects in Workspace: " + projects);
				monitor.beginTask("Batch Git Pull", projects.size());
				for (IProject p : projects) {
					String name = p.getName();
					monitor.subTask("Pulling " + name);
					File projectLocationFile = p.getLocation().append(".git").toFile();
					logger.info(name + " with location " + projectLocationFile.getAbsolutePath());

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
									logger.error("Error pulling from " + name + ": " + e.getMessage() + ". Skipping and proceeding.", e);
								}
							}
						}
					} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
						// ignore
						logger.info(name + " is not managed via Git: " + rnfe.getMessage());
					} catch (IOException e) {
						logger.error("Error pulling " + name + ": " + e.getMessage(), e);
					}
					logger.info("Finished " + name);
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
