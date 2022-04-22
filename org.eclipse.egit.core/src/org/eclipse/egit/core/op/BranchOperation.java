/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FileUtils;

/**
 * This class implements checkouts of a specific revision. A check is made that
 * this can be done without data loss.
 */
public class BranchOperation implements IEGitOperation {

	private final String target;

	private Repository[] repositories;

	private @NonNull Map<Repository, CheckoutResult> results = new HashMap<>();

	private boolean delete;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 */
	public BranchOperation(Repository repository, String target) {
		this(repository, target, true);
	}

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param delete
	 *            true to delete missing projects on new branch, false to close
	 *            them
	 */
	public BranchOperation(Repository repository, String target, boolean delete) {
		this(new Repository[] { repository }, target, delete);
	}

	/**
	 *
	 * @param repositories
	 * @param target
	 * @param delete
	 */
	public BranchOperation(Repository[] repositories, String target,
			boolean delete) {
		this.repositories = repositories;
		this.target = target;
		this.delete = delete;
	}

	@Override
	public void execute(IProgressMonitor m) throws CoreException {
		IWorkspaceRunnable action = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				try {
					pm.setTaskName(MessageFormat.format(
							CoreText.BranchOperation_performingBranch, target));
					int numberOfRepositories = repositories.length;
					SubMonitor progress = SubMonitor.convert(pm,
							numberOfRepositories * 2);
					for (Repository repository : repositories) {
						CheckoutResult result;
						if (pm.isCanceled()) {
							// don't break from the loop, the result map must be
							// filled
							result = CheckoutResult.NOT_TRIED_RESULT;
						}
						else {
							result = checkoutRepository(repository,
									progress.newChild(1),
									numberOfRepositories > 1);
							if (result.getStatus() == Status.NONDELETED) {
								retryDelete(repository,
										result.getUndeletedList());
							}
						}
						results.put(repository, result);
					}
					refreshAffectedProjects(
							progress.newChild(numberOfRepositories));
				} finally {
					pm.done();
				}
			}

			public CheckoutResult checkoutRepository(Repository repo,
					IProgressMonitor monitor, boolean logErrors)
					throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor, 2);
				closeProjectsMissingAfterCheckout(repo, progress.newChild(1));
				try (Git git = new Git(repo)) {
					CheckoutCommand co = git.checkout().setProgressMonitor(
							new EclipseGitProgressTransformer(
									progress.newChild(1)));
					co.setName(target);
					try {
						co.call();
					} catch (CheckoutConflictException e) {
						// Covered by the status return below.
					} catch (JGitInternalException | GitAPIException e) {
						String msg = MessageFormat.format(
								CoreText.BranchOperation_checkoutError,
								target, repo.getDirectory());
						if (logErrors) {
							Activator.logError(msg, e);
						} else {
							throw new CoreException(Activator.error(msg, e));
						}
					}
					return co.getResult();
				}
			}

			private void closeProjectsMissingAfterCheckout(Repository repo,
					IProgressMonitor monitor) throws CoreException {
				IProject[] missing = getMissingProjects(repo, target, monitor);

				if (missing.length > 0) {
					SubMonitor closeMonitor = SubMonitor.convert(monitor,
							missing.length);
					closeMonitor.setWorkRemaining(missing.length);
					for (IProject project : missing) {
						if (closeMonitor.isCanceled()) {
							break;
						}
						closeMonitor.subTask(MessageFormat.format(
								CoreText.BranchOperation_closingMissingProject,
								project.getName()));
						project.close(closeMonitor.newChild(1));
					}
				}
			}

			private void refreshAffectedProjects(IProgressMonitor monitor)
					throws CoreException {
				IProject[] refreshProjects = results.entrySet().stream()
						.map(this::getAffectedProjects)
						.flatMap(Stream::of).distinct()
						.toArray(IProject[]::new);
				ProjectUtil.refreshValidProjects(refreshProjects, delete,
						monitor);
			}

			private IProject[] getAffectedProjects(
					Entry<Repository, CheckoutResult> entry) {
				CheckoutResult result = entry.getValue();

				if (result.getStatus() != Status.OK
						&& result.getStatus() != Status.NONDELETED) {
					// the checkout did not succeed
					return new IProject[0];
				}

				Repository repo = entry.getKey();
				List<String> pathsToHandle = new ArrayList<>();
				pathsToHandle.addAll(result.getModifiedList());
				pathsToHandle.addAll(result.getRemovedList());
				pathsToHandle.addAll(result.getConflictList());
				IProject[] refreshProjects = ProjectUtil
						.getProjectsContaining(repo, pathsToHandle);
				return refreshProjects;
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, m);
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRuleForRepositories(Arrays.asList(repositories));
	}

	/**
	 * @return the result of the operation
	 */
	@NonNull
	public Map<Repository, CheckoutResult> getResults() {
		return results;
	}

	/**
	 * @param repo
	 * @return return the result specific to a repository
	 */
	public CheckoutResult getResult(Repository repo) {
		CheckoutResult result = results.get(repo);
		if (result == null) {
			return CheckoutResult.NOT_TRIED_RESULT;
		}
		return result;
	}

	void retryDelete(Repository repo, List<String> pathList) {
		// try to delete, but for a short time only
		long startTime = System.currentTimeMillis();
		for (String path : pathList) {
			if (System.currentTimeMillis() - startTime > 1000) {
				break;
			}
			File fileToDelete = new File(repo.getWorkTree(), path);
			if (fileToDelete.exists()) {
				try {
					// Only files should be passed here, thus
					// we ignore attempt to delete submodules when
					// we switch to a branch without a submodule
					if (!fileToDelete.isFile()) {
						FileUtils.delete(fileToDelete, FileUtils.RETRY);
					}
				} catch (IOException e) {
					// ignore here
				}
			}
		}
	}

	/**
	 * Compute the current projects that will be missing after the given branch
	 * is checked out
	 *
	 * @param repository
	 * @param branch
	 * @param monitor
	 * @return non-null but possibly empty array of missing projects
	 * @throws CoreException
	 */
	private IProject[] getMissingProjects(Repository repository,
			String branch, IProgressMonitor monitor) throws CoreException {
		IProject[] openProjects = ProjectUtil.getValidOpenProjects(repository);
		if (delete || openProjects.length == 0) {
			return new IProject[0];
		}
		ObjectId targetTreeId;
		ObjectId currentTreeId;
		try {
			targetTreeId = repository.resolve(branch + "^{tree}"); //$NON-NLS-1$
			currentTreeId = repository.resolve(Constants.HEAD + "^{tree}"); //$NON-NLS-1$
		} catch (IOException e) {
			return new IProject[0];
		}
		if (targetTreeId == null || currentTreeId == null) {
			return new IProject[0];
		}
		Map<File, IProject> locations = new HashMap<>();
		for (IProject project : openProjects) {
			if (monitor.isCanceled()) {
				break;
			}
			IPath location = project.getLocation();
			if (location == null) {
				continue;
			}
			location = location
					.append(IProjectDescription.DESCRIPTION_FILE_NAME);
			locations.put(location.toFile(), project);
		}

		List<IProject> toBeClosed = new ArrayList<>();
		File root = repository.getWorkTree();
		try (TreeWalk walk = new TreeWalk(repository)) {
			walk.addTree(targetTreeId);
			walk.addTree(currentTreeId);
			walk.addTree(new FileTreeIterator(repository));
			walk.setRecursive(true);
			walk.setFilter(AndTreeFilter.create(PathSuffixFilter
					.create(IProjectDescription.DESCRIPTION_FILE_NAME),
					TreeFilter.ANY_DIFF));
			while (walk.next()) {
				if (monitor.isCanceled()) {
					break;
				}
				AbstractTreeIterator targetIter = walk.getTree(0,
						AbstractTreeIterator.class);
				if (targetIter != null) {
					continue;
				}
				AbstractTreeIterator currentIter = walk.getTree(1,
						AbstractTreeIterator.class);
				AbstractTreeIterator workingIter = walk.getTree(2,
						AbstractTreeIterator.class);
				if (currentIter == null || workingIter == null) {
					continue;
				}
				IProject project = locations.get(new File(root, walk
						.getPathString()));
				if (project != null) {
					toBeClosed.add(project);
				}
			}
		} catch (IOException e) {
			return new IProject[0];
		}
		return toBeClosed.toArray(new IProject[0]);
	}
}
