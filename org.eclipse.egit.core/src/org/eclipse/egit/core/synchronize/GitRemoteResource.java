/*******************************************************************************
 * Copyright (C) 2011, 2021 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.UnsupportedEncodingException;

import org.eclipse.egit.core.info.GitInfo;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.CachedResourceVariant;

/**
 * Base class for EGit's remote resource variants.
 *
 * @since 3.0
 */
public abstract class GitRemoteResource extends CachedResourceVariant
		implements GitInfo {

	private final Repository repo;

	private final String path;

	private final RevCommit commitId;

	private final ObjectId objectId;


	GitRemoteResource(Repository repo, RevCommit commitId, ObjectId objectId,
			String path) {
		this.repo = repo;
		this.path = path;
		this.objectId = objectId;
		this.commitId = commitId;
	}

	@Override
	public String getName() {
		int lastSeparator = path.lastIndexOf("/"); //$NON-NLS-1$
		return path.substring(lastSeparator + 1, path.length());
	}

	@Override
	public String getContentIdentifier() {
		if (commitId == null)
			return ""; //$NON-NLS-1$

		StringBuilder s = new StringBuilder();
		s.append(Utils.getShortObjectId(commitId));
		s.append("..."); //$NON-NLS-1$

		PersonIdent authorIdent = commitId.getAuthorIdent();
		if (authorIdent != null) {
			s.append(" ("); //$NON-NLS-1$
			s.append(authorIdent.getName());
			s.append(")"); //$NON-NLS-1$
		}
		return s.toString();
	}

	@Override
	public byte[] asBytes() {
		try {
			return getObjectId().name().getBytes("UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Repository getRepository() {
		return repo;
	}

	@Override
	public final String getGitPath() {
		return path;
	}

	@Override
	protected String getCachePath() {
		return path;
	}

	@Override
	protected String getCacheId() {
		return "org.eclipse.egit"; //$NON-NLS-1$
	}

	boolean exists() {
		return commitId != null;
	}

	/**
	 * @return the commit Id for this resource variant.
	 */
	@Override
	public RevCommit getCommitId() {
		return commitId;
	}

	@Override
	public Source getSource() {
		return Source.COMMIT;
	}

	/**
	 * @return object id, or {code {@link RevCommit#zeroId()} if object doesn't
	 *         exist in repository
	 */
	ObjectId getObjectId() {
		return objectId != null ? objectId : zeroId();
	}

	/**
	 * @return path to the resource.
	 */
	public String getPath() {
		return path;
	}

}
