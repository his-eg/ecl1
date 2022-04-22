/*
 * Copyright (C) 2009, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.util;

import java.util.Arrays;

/**
 * A more efficient List&lt;Long&gt; using a primitive long array.
 */
public class LongList {
	private long[] entries;

	private int count;

	/**
	 * Create an empty list with a default capacity.
	 */
	public LongList() {
		this(10);
	}

	/**
	 * Create an empty list with the specified capacity.
	 *
	 * @param capacity
	 *            number of entries the list can initially hold.
	 */
	public LongList(int capacity) {
		entries = new long[capacity];
	}

	/**
	 * Get number of entries in this list
	 *
	 * @return number of entries in this list
	 */
	public int size() {
		return count;
	}

	/**
	 * Get the value at the specified index
	 *
	 * @param i
	 *            index to read, must be in the range [0, {@link #size()}).
	 * @return the number at the specified index
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *             the index outside the valid range
	 */
	public long get(int i) {
		if (count <= i)
			throw new ArrayIndexOutOfBoundsException(i);
		return entries[i];
	}

	/**
	 * Determine if an entry appears in this collection.
	 *
	 * @param value
	 *            the value to search for.
	 * @return true of {@code value} appears in this list.
	 */
	public boolean contains(long value) {
		for (int i = 0; i < count; i++)
			if (entries[i] == value)
				return true;
		return false;
	}

	/**
	 * Clear this list
	 */
	public void clear() {
		count = 0;
	}

	/**
	 * Add an entry to the end of the list.
	 *
	 * @param n
	 *            the number to add.
	 */
	public void add(long n) {
		if (count == entries.length)
			grow();
		entries[count++] = n;
	}

	/**
	 * Assign an entry in the list.
	 *
	 * @param index
	 *            index to set, must be in the range [0, {@link #size()}).
	 * @param n
	 *            value to store at the position.
	 */
	public void set(int index, long n) {
		if (count < index)
			throw new ArrayIndexOutOfBoundsException(index);
		else if (count == index)
			add(n);
		else
			entries[index] = n;
	}

	/**
	 * Pad the list with entries.
	 *
	 * @param toIndex
	 *            index position to stop filling at. 0 inserts no filler. 1
	 *            ensures the list has a size of 1, adding <code>val</code> if
	 *            the list is currently empty.
	 * @param val
	 *            value to insert into padded positions.
	 */
	public void fillTo(int toIndex, long val) {
		while (count < toIndex)
			add(val);
	}

	/**
	 * Sort the list of longs according to their natural ordering.
	 */
	public void sort() {
		Arrays.sort(entries, 0, count);
	}

	private void grow() {
		final long[] n = new long[(entries.length + 16) * 3 / 2];
		System.arraycopy(entries, 0, n, 0, count);
		entries = n;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		final StringBuilder r = new StringBuilder();
		r.append('[');
		for (int i = 0; i < count; i++) {
			if (i > 0)
				r.append(", "); //$NON-NLS-1$
			r.append(entries[i]);
		}
		r.append(']');
		return r.toString();
	}
}
