/*******************************************************************************
 * Copyright (C) 2015, 2021 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.info;

import org.eclipse.egit.core.info.GitItemState;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.IndexDiff.StageState;

/**
 * Base implementation of an {@link GitItemState}.
 */
public class GitItemStateImpl implements GitItemState {

	/**
	 * Flag indicating whether or not the resource is tracked
	 */
	private boolean tracked;

	/**
	 * Flag indicating whether or not the resource is ignored
	 */
	private boolean ignored;

	/**
	 * Flag indicating whether or not the resource has changes that are not
	 * staged
	 */
	private boolean dirty;

	/**
	 * Flag indicating whether or not the resource has been deleted locally
	 * (unstaged deletion).
	 */
	private boolean missing;

	/**
	 * Staged state of the resource
	 */
	@NonNull
	private StagingState staged = StagingState.NOT_STAGED;

	/**
	 * Flag indicating whether or not the resource has merge conflicts
	 */
	private boolean conflicts;

	/**
	 * For conflicting resources, the kind of conflict.
	 */
	private StageState conflictType;

	/**
	 * Flag indicating whether or not the resource is assumed unchanged
	 */
	private boolean assumeUnchanged;

	@Override
	public boolean isTracked() {
		return tracked;
	}

	@Override
	public boolean isIgnored() {
		return ignored;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isMissing() {
		return missing;
	}

	@Override
	public StagingState getStagingState() {
		return staged;
	}

	@Override
	public final boolean isStaged() {
		return staged != StagingState.NOT_STAGED;
	}

	@Override
	public boolean hasConflicts() {
		return conflicts;
	}

	@Override
	public StageState getConflictType() {
		return conflictType;
	}

	@Override
	public boolean isAssumeUnchanged() {
		return assumeUnchanged;
	}

	@Override
	public final boolean hasUnstagedChanges() {
		return !isIgnored()
				&& (!isTracked() || isDirty() || isMissing() || hasConflicts());
	}

	/**
	 * Sets the staged property.
	 *
	 * @param staged
	 *            value to set.
	 */
	protected void setStagingState(@NonNull StagingState staged) {
		this.staged = staged;
	}

	/**
	 * Sets the conflicts property.
	 *
	 * @param conflicts
	 *            value to set.
	 */
	protected void setConflicts(boolean conflicts) {
		this.conflicts = conflicts;
	}

	/**
	 * Sets the conflict type.
	 *
	 * @param conflictType
	 *            to set
	 */
	protected void setConflictType(StageState conflictType) {
		this.conflictType = conflictType;
	}

	/**
	 * Sets the tracked property.
	 *
	 * @param tracked
	 *            value to set.
	 */
	protected void setTracked(boolean tracked) {
		this.tracked = tracked;
	}

	/**
	 * Sets the ignored property.
	 *
	 * @param ignored
	 *            value to set.
	 */
	protected void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	/**
	 * Sets the dirty property.
	 *
	 * @param dirty
	 *            value to set.
	 */
	protected void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	/**
	 * Sets the missing property.
	 *
	 * @param missing
	 *            value to set.
	 */
	protected void setMissing(boolean missing) {
		this.missing = missing;
	}

	/**
	 * Sets the assumeUnchanged property.
	 *
	 * @param assumeUnchanged
	 *            value to set.
	 */
	protected void setAssumeUnchanged(boolean assumeUnchanged) {
		this.assumeUnchanged = assumeUnchanged;
	}

}
