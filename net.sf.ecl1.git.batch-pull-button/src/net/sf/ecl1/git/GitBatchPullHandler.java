package net.sf.ecl1.git;

import static net.sf.ecl1.git.Activator.info;
import static net.sf.ecl1.git.Activator.error;

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

/**
 * Executes a pull command on all open projects using Git as SCM
 *  
 * @author keunecke
 */
public class GitBatchPullHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		info("Starting ecl1GitBatchPull");
		Job job = new Job("ecl1GitBatchPull") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<IProject> projects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
				info("Found projects in Workspace: " + projects);
				monitor.beginTask("Batch Git Pull", projects.size());
				for (IProject p : projects) {
					String name = p.getName();
					monitor.subTask("Pulling " + name);
					File projectLocationFile = p.getLocation().append(".git").toFile();
					info(name + " with location " + projectLocationFile.getAbsolutePath());

					try {
						Git git = Git.open(projectLocationFile);
						info("Getting remotes of " + name);
						Set<String> remotes = git.getRepository().getRemoteNames();
						if(remotes != null && !remotes.isEmpty()) {
							for (String remote : remotes) {
								try {
									PullCommand pull = git.pull();
									pull.setRemote(remote);
									info(name + " has remotes. Starting to pull remote '" + remote + "'.");
									pull.call();
								} catch (GitAPIException | JGitInternalException e) {
									error("Error pulling from " + name + ": " + e.getMessage() + ". Skipping and proceeding.");
									e.printStackTrace();
								}
							}
						}
					} catch (org.eclipse.jgit.errors.RepositoryNotFoundException rnfe) {
						// ignore
						info(name + " is not managed via Git: " + rnfe.getMessage());
					} catch (IOException e) {
						error("Error pulling " + name + ": " + e.getMessage(), e);
						e.printStackTrace();
					}
					info("Finished " + name);
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
