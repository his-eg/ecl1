/*
 * Copyright (C) 2010, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

/**
 * A {@link org.eclipse.jgit.lib.Ref} that points directly at an
 * {@link org.eclipse.jgit.lib.ObjectId}.
 */
public abstract class ObjectIdRef implements Ref {
	/** Any reference whose peeled value is not yet known. */
	public static class Unpeeled extends ObjectIdRef {
		/**
		 * Create a new ref pairing.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref. May be {@code null} to indicate
		 *            a ref that does not exist yet.
		 */
		public Unpeeled(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id) {
			super(st, name, id, UNDEFINED_UPDATE_INDEX);
		}

		/**
		 * Create a new ref pairing with update index.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref. May be {@code null} to indicate
		 *            a ref that does not exist yet.
		 * @param updateIndex
		 *            number increasing with each update to the reference.
		 * @since 5.3
		 */
		public Unpeeled(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id, long updateIndex) {
			super(st, name, id, updateIndex);
		}

		@Override
		@Nullable
		public ObjectId getPeeledObjectId() {
			return null;
		}

		@Override
		public boolean isPeeled() {
			return false;
		}
	}

	/** An annotated tag whose peeled object has been cached. */
	public static class PeeledTag extends ObjectIdRef {
		private final ObjectId peeledObjectId;

		/**
		 * Create a new ref pairing.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref.
		 * @param p
		 *            the first non-tag object that tag {@code id} points to.
		 */
		public PeeledTag(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id, @NonNull ObjectId p) {
			super(st, name, id, UNDEFINED_UPDATE_INDEX);
			peeledObjectId = p;
		}

		/**
		 * Create a new ref pairing with update index.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref. May be {@code null} to indicate
		 *            a ref that does not exist yet.
		 * @param p
		 *            the first non-tag object that tag {@code id} points to.
		 * @param updateIndex
		 *            number increasing with each update to the reference.
		 * @since 5.3
		 */
		public PeeledTag(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id, @NonNull ObjectId p, long updateIndex) {
			super(st, name, id, updateIndex);
			peeledObjectId = p;
		}

		@Override
		@NonNull
		public ObjectId getPeeledObjectId() {
			return peeledObjectId;
		}

		@Override
		public boolean isPeeled() {
			return true;
		}
	}

	/** A reference to a non-tag object coming from a cached source. */
	public static class PeeledNonTag extends ObjectIdRef {
		/**
		 * Create a new ref pairing.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref. May be {@code null} to indicate
		 *            a ref that does not exist yet.
		 */
		public PeeledNonTag(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id) {
			super(st, name, id, UNDEFINED_UPDATE_INDEX);
		}

		/**
		 * Create a new ref pairing with update index.
		 *
		 * @param st
		 *            method used to store this ref.
		 * @param name
		 *            name of this ref.
		 * @param id
		 *            current value of the ref. May be {@code null} to indicate
		 *            a ref that does not exist yet.
		 * @param updateIndex
		 *            number increasing with each update to the reference.
		 * @since 5.3
		 */
		public PeeledNonTag(@NonNull Storage st, @NonNull String name,
				@Nullable ObjectId id, long updateIndex) {
			super(st, name, id, updateIndex);
		}

		@Override
		@Nullable
		public ObjectId getPeeledObjectId() {
			return null;
		}

		@Override
		public boolean isPeeled() {
			return true;
		}
	}

	private final String name;

	private final Storage storage;

	private final ObjectId objectId;

	private final long updateIndex;

	/**
	 * Create a new ref pairing.
	 *
	 * @param st
	 *            method used to store this ref.
	 * @param name
	 *            name of this ref.
	 * @param id
	 *            current value of the ref. May be {@code null} to indicate a
	 *            ref that does not exist yet.
	 * @param updateIndex
	 *            number that increases with each ref update. Set to -1 if the
	 *            storage doesn't support versioning.
	 * @since 5.3
	 */
	protected ObjectIdRef(@NonNull Storage st, @NonNull String name,
			@Nullable ObjectId id, long updateIndex) {
		this.name = name;
		this.storage = st;
		this.objectId = id;
		this.updateIndex = updateIndex;
	}

	/** {@inheritDoc} */
	@Override
	@NonNull
	public String getName() {
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isSymbolic() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	@NonNull
	public Ref getLeaf() {
		return this;
	}

	/** {@inheritDoc} */
	@Override
	@NonNull
	public Ref getTarget() {
		return this;
	}

	/** {@inheritDoc} */
	@Override
	@Nullable
	public ObjectId getObjectId() {
		return objectId;
	}

	/** {@inheritDoc} */
	@Override
	@NonNull
	public Storage getStorage() {
		return storage;
	}

	/**
	 * {@inheritDoc}
	 * @since 5.3
	 */
	@Override
	public long getUpdateIndex() {
		if (updateIndex == UNDEFINED_UPDATE_INDEX) {
			throw new UnsupportedOperationException();
		}
		return updateIndex;
	}

	/** {@inheritDoc} */
	@NonNull
	@Override
	public String toString() {
		StringBuilder r = new StringBuilder();
		r.append("Ref["); //$NON-NLS-1$
		r.append(getName());
		r.append('=');
		r.append(ObjectId.toString(getObjectId()));
		r.append('(');
		r.append(updateIndex); // Print value, even if -1
		r.append(")]"); //$NON-NLS-1$
		return r.toString();
	}
}
