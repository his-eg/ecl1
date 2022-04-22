/*******************************************************************************
 * Copyright (C) 2013, 2016 Stephen Elsemore <selsemore@collab.net> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.decorators.IProblemDecoratable;
import org.eclipse.jgit.annotations.NonNull;

/**
 * A staged/unstaged folder entry in the tree
 */
public class StagingFolderEntry implements IAdaptable, IProblemDecoratable {
	private final IPath repoLocation;
	private final IPath repoRelativePath;

	private final String label;
	private final IContainer container;

	private StagingFolderEntry parent;
	private Object[] children;


	/**
	 * @param repoLocation
	 * @param repoRelativePath
	 * @param label
	 */
	public StagingFolderEntry(IPath repoLocation, IPath repoRelativePath,
			String label) {
		this.repoLocation = repoLocation;
		this.repoRelativePath = repoRelativePath;
		this.label = label;
		this.container = ResourceUtil.getContainerForLocation(getLocation(),
				false);
	}

	/**
	 * @return the container corresponding to the entry, if it exists in the
	 *         workspace, null otherwise.
	 */
	public IContainer getContainer() {
		return container;
	}

	@Override
	public int getProblemSeverity() {
		if (container == null)
			return SEVERITY_NONE;

		try {
			return container.findMaxProblemSeverity(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			return SEVERITY_NONE;
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IResource.class || adapter == IContainer.class)
			return adapter.cast(getContainer());
		else if (adapter == IPath.class)
			return adapter.cast(getLocation());
		return null;
	}

	/**
	 * @return the repo-relative path of this folder
	 */
	public IPath getPath() {
		return repoRelativePath;
	}

	/**
	 * @return the absolute path corresponding to the folder entry
	 */
	@SuppressWarnings("null")
	@NonNull
	public IPath getLocation() {
		return repoLocation.append(repoRelativePath);
	}

	/**
	 * @return the label of the node
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the parent folder entry
	 */
	public StagingFolderEntry getParent() {
		return parent;
	}

	/**
	 * @param parent
	 */
	public void setParent(StagingFolderEntry parent) {
		this.parent = parent;
	}

	/**
	 * @return child nodes (files or folders)
	 */
	public Object[] getChildren() {
		return children;
	}

	/**
	 * @param children
	 */
	public void setChildren(Object[] children) {
		this.children = children;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StagingFolderEntry)
			return ((StagingFolderEntry) obj).getLocation().equals(getLocation());
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return getLocation().hashCode();
	}

	@Override
	public String toString() {
		return "StagingFolderEntry[" + repoRelativePath + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}
}
