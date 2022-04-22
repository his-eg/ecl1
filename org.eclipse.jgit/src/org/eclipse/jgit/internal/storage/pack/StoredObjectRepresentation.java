/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.pack;

import org.eclipse.jgit.lib.ObjectId;

/**
 * An object representation
 * {@link org.eclipse.jgit.internal.storage.pack.PackWriter} can consider for
 * packing.
 */
public class StoredObjectRepresentation {
	/** Special unknown value for {@link #getWeight()}. */
	public static final int WEIGHT_UNKNOWN = Integer.MAX_VALUE;

	/** Stored in pack format, as a delta to another object. */
	public static final int PACK_DELTA = 0;

	/** Stored in pack format, without delta. */
	public static final int PACK_WHOLE = 1;

	/** Only available after inflating to canonical format. */
	public static final int FORMAT_OTHER = 2;

	/**
	 * Get relative size of this object's packed form.
	 *
	 * @return relative size of this object's packed form. The special value
	 *         {@link #WEIGHT_UNKNOWN} can be returned to indicate the
	 *         implementation doesn't know, or cannot supply the weight up
	 *         front.
	 */
	public int getWeight() {
		return WEIGHT_UNKNOWN;
	}

	/**
	 * Get the storage format type
	 *
	 * @return the storage format type, which must be one of
	 *         {@link #PACK_DELTA}, {@link #PACK_WHOLE}, or
	 *         {@link #FORMAT_OTHER}.
	 */
	public int getFormat() {
		return FORMAT_OTHER;
	}

	/**
	 * Get identity of the object this delta applies to in order to recover the
	 * original object content.
	 *
	 * @return identity of the object this delta applies to in order to recover
	 *         the original object content. This method should only be called if
	 *         {@link #getFormat()} returned {@link #PACK_DELTA}.
	 */
	public ObjectId getDeltaBase() {
		return null;
	}

	/**
	 * Whether the current representation of the object has had delta
	 * compression attempted on it.
	 *
	 * @return whether the current representation of the object has had delta
	 *         compression attempted on it.
	 */
	public boolean wasDeltaAttempted() {
		int fmt = getFormat();
		return fmt == PACK_DELTA || fmt == PACK_WHOLE;
	}
}
