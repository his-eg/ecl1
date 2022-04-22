/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2021, Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the current version in the workspace. */
public class WorkspaceFileRevision extends GitFileRevision {

	/**
	 * Creates a new {@link WorkspaceFileRevision} for a given {@link IResource}
	 * in a {@link Repository}.
	 *
	 * @param repository
	 *            the resource is in
	 * @param resource
	 *            to create the revision for
	 * @return the {@link WorkspaceFileRevision}
	 */
	public static WorkspaceFileRevision forFile(Repository repository,
			IResource resource) {
		IPath path = ResourceUtil
				.getRepositoryRelativePath(resource.getLocation(), repository);
		String repoRelativePath = path == null ? null : path.toString();
		return new WorkspaceFileRevision(repository, repoRelativePath,
				resource);
	}

	private final IResource rsrc;

	/**
	 * @param repository
	 * @param path
	 * @param resource
	 */
	public WorkspaceFileRevision(Repository repository, String path,
			IResource resource) {
		super(repository, path);
		rsrc = resource;
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return rsrc instanceof IStorage ? (IStorage) rsrc : null;
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public String getAuthor() {
		return "";  //$NON-NLS-1$
	}

	@Override
	public long getTimestamp() {
		return -1;
	}

	@Override
	public String getComment() {
		return "";  //$NON-NLS-1$
	}

	@Override
	public String getContentIdentifier() {
		return WORKSPACE;
	}

	@Override
	public Source getSource() {
		return Source.WORKING_TREE;
	}
}
