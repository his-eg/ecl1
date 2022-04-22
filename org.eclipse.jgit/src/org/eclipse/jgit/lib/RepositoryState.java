/*
 * Copyright (C) 2008, Mike Ralphson <mike@abacus.co.uk>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg.lists@dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import org.eclipse.jgit.internal.JGitText;
/**
 * Important state of the repository that affects what can and cannot bed
 * done. This is things like unhandled conflicted merges and unfinished rebase.
 *
 * The granularity and set of states are somewhat arbitrary. The methods
 * on the state are the only supported means of deciding what to do.
 */
public enum RepositoryState {
	/** Has no work tree and cannot be used for normal editing. */
	BARE {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return false; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() {
			return JGitText.get().repositoryState_bare;
		}
	},

	/**
	 * A safe state for working normally
	 * */
	SAFE {
		@Override
		public boolean canCheckout() { return true; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_normal; }
	},

	/** An unfinished merge. Must resolve or reset before continuing normally
	 */
	MERGING {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return false; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_conflicts; }
	},

	/**
	 * An merge where all conflicts have been resolved. The index does not
	 * contain any unmerged paths.
	 */
	MERGING_RESOLVED {
		@Override
		public boolean canCheckout() { return true; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_merged; }
	},

	/** An unfinished cherry-pick. Must resolve or reset before continuing normally
	 */
	CHERRY_PICKING {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return false; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_conflicts; }
	},

	/**
	 * A cherry-pick where all conflicts have been resolved. The index does not
	 * contain any unmerged paths.
	 */
	CHERRY_PICKING_RESOLVED {
		@Override
		public boolean canCheckout() { return true; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_merged; }
	},

	/** An unfinished revert. Must resolve or reset before continuing normally
	 */
	REVERTING {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return false; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_conflicts; }
	},

	/**
	 * A revert where all conflicts have been resolved. The index does not
	 * contain any unmerged paths.
	 */
	REVERTING_RESOLVED {
		@Override
		public boolean canCheckout() { return true; }

		@Override
		public boolean canResetHead() { return true; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_merged; }
	},

	/**
	 * An unfinished rebase or am. Must resolve, skip or abort before normal work can take place
	 */
	REBASING {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return true; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_rebaseOrApplyMailbox; }
	},

	/**
	 * An unfinished rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_REBASING {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return true; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_rebase; }
	},

	/**
	 * An unfinished apply. Must resolve, skip or abort before normal work can take place
	 */
	APPLY {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_applyMailbox; }
	},

	/**
	 * An unfinished rebase with merge. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_MERGE {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return true; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_rebaseWithMerge; }
	},

	/**
	 * An unfinished interactive rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_INTERACTIVE {
		@Override
		public boolean canCheckout() { return false; }

		@Override
		public boolean canResetHead() { return false; }

		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return true; }

		@Override
		public boolean isRebasing() { return true; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_rebaseInteractive; }
	},

	/**
	 * Bisecting being done. Normal work may continue but is discouraged
	 */
	BISECTING {
		/* Changing head is a normal operation when bisecting */
		@Override
		public boolean canCheckout() { return true; }

		/* Do not reset, checkout instead */
		@Override
		public boolean canResetHead() { return false; }

		/* Commit during bisect is useful */
		@Override
		public boolean canCommit() { return true; }

		@Override
		public boolean canAmend() { return false; }

		@Override
		public boolean isRebasing() { return false; }

		@Override
		public String getDescription() { return JGitText.get().repositoryState_bisecting; }
	};

	/**
	 * Whether checkout can be done.
	 *
	 * @return whether checkout can be done.
	 */
	public abstract boolean canCheckout();

	/**
	 * @return true if we can commit
	 */
	public abstract boolean canCommit();

	/**
	 * @return true if reset to another HEAD is considered SAFE
	 */
	public abstract boolean canResetHead();

	/**
	 * @return true if amending is considered SAFE
	 */
	public abstract boolean canAmend();

	/**
	 * @return true if the repository is currently in a rebase
	 * @since 3.0
	 */
	public abstract boolean isRebasing();

	/**
	 * @return a human readable description of the state.
	 */
	public abstract String getDescription();
}
