/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2014, Obeo
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.storage.GitBlobStorage;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Blob Storage related to a file in the index. Method <code>getFullPath</code>
 * returns a path of format <repository name>/<file path> index
 *
 * @see CommitBlobStorage
 *
 */
public class IndexBlobStorage extends GitBlobStorage {

	IndexBlobStorage(Repository repository, String fileName, boolean isGitlink,
			ObjectId blob, CheckoutMetadata metadata) {
		super(repository, fileName, blob, metadata, isGitlink);
	}

	@Override
	public Source getSource() {
		return Source.INDEX;
	}

	@Override
	public IPath getFullPath() {
		IPath repoPath = new Path(
				RepositoryUtil.INSTANCE.getRepositoryName(db));
		String pathString = super.getFullPath().toPortableString() + " index"; //$NON-NLS-1$
		return repoPath.append(Path.fromPortableString(pathString));
	}

}
