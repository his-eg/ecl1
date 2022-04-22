/*
 * Copyright (C) 2012, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.BitmapCommit;
import org.eclipse.jgit.internal.storage.pack.ObjectToPack;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BitmapIndex.Bitmap;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdOwnerMap;
import org.eclipse.jgit.util.BlockList;

import com.googlecode.javaewah.EWAHCompressedBitmap;

/**
 * Helper for constructing
 * {@link org.eclipse.jgit.internal.storage.file.PackBitmapIndex}es.
 */
public class PackBitmapIndexBuilder extends BasePackBitmapIndex {
	private static final int MAX_XOR_OFFSET_SEARCH = 10;

	private final EWAHCompressedBitmap commits;
	private final EWAHCompressedBitmap trees;
	private final EWAHCompressedBitmap blobs;
	private final EWAHCompressedBitmap tags;
	private final BlockList<PositionEntry> byOffset;

	private final LinkedList<StoredBitmap>
			bitmapsToWriteXorBuffer = new LinkedList<>();

	private List<StoredEntry> bitmapsToWrite = new ArrayList<>();

	final ObjectIdOwnerMap<PositionEntry>
			positionEntries = new ObjectIdOwnerMap<>();

	/**
	 * Creates a PackBitmapIndex used for building the contents of an index
	 * file.
	 *
	 * @param objects
	 *            objects sorted by name. The list must be initially sorted by
	 *            ObjectId (name); it will be resorted in place.
	 */
	public PackBitmapIndexBuilder(List<ObjectToPack> objects) {
		super(new ObjectIdOwnerMap<StoredBitmap>());
		byOffset = new BlockList<>(objects.size());
		sortByOffsetAndIndex(byOffset, positionEntries, objects);

		// 64 objects fit in a single long word (64 bits).
		// On average a repository is 30% commits, 30% trees, 30% blobs.
		// Initialize bitmap capacity for worst case to minimize growing.
		int sizeInWords = Math.max(4, byOffset.size() / 64 / 3);
		commits = new EWAHCompressedBitmap(sizeInWords);
		trees = new EWAHCompressedBitmap(sizeInWords);
		blobs = new EWAHCompressedBitmap(sizeInWords);
		tags = new EWAHCompressedBitmap(sizeInWords);
		for (int i = 0; i < objects.size(); i++) {
			int type = objects.get(i).getType();
			switch (type) {
			case Constants.OBJ_COMMIT:
				commits.set(i);
				break;
			case Constants.OBJ_TREE:
				trees.set(i);
				break;
			case Constants.OBJ_BLOB:
				blobs.set(i);
				break;
			case Constants.OBJ_TAG:
				tags.set(i);
				break;
			default:
				throw new IllegalArgumentException(MessageFormat.format(
						JGitText.get().badObjectType, String.valueOf(type)));
			}
		}
		commits.trim();
		trees.trim();
		blobs.trim();
		tags.trim();
	}

	private static void sortByOffsetAndIndex(BlockList<PositionEntry> byOffset,
			ObjectIdOwnerMap<PositionEntry> positionEntries,
			List<ObjectToPack> entries) {
		for (int i = 0; i < entries.size(); i++) {
			positionEntries.add(new PositionEntry(entries.get(i), i));
		}
		Collections.sort(entries, (ObjectToPack a, ObjectToPack b) -> Long
				.signum(a.getOffset() - b.getOffset()));
		for (int i = 0; i < entries.size(); i++) {
			PositionEntry e = positionEntries.get(entries.get(i));
			e.offsetPosition = i;
			byOffset.add(e);
		}
	}

	/**
	 * Get set of objects included in the pack.
	 *
	 * @return set of objects included in the pack.
	 */
	public ObjectIdOwnerMap<ObjectIdOwnerMap.Entry> getObjectSet() {
		ObjectIdOwnerMap<ObjectIdOwnerMap.Entry> r = new ObjectIdOwnerMap<>();
		for (PositionEntry e : byOffset) {
			r.add(new ObjectIdOwnerMap.Entry(e) {
				// A new entry that copies the ObjectId
			});
		}
		return r;
	}

	/**
	 * Stores the bitmap for the objectId.
	 *
	 * @param objectId
	 *            the object id key for the bitmap.
	 * @param bitmap
	 *            the bitmap
	 * @param flags
	 *            the flags to be stored with the bitmap
	 */
	public void addBitmap(AnyObjectId objectId, Bitmap bitmap, int flags) {
		addBitmap(objectId, bitmap.retrieveCompressed(), flags);
	}

	/**
	 * Processes a commit and prepares its bitmap to write to the bitmap index
	 * file.
	 *
	 * @param c
	 *            the commit corresponds to the bitmap.
	 * @param bitmap
	 *            the bitmap to be written.
	 * @param flags
	 *            the flags of the commit.
	 */
	public void processBitmapForWrite(BitmapCommit c, Bitmap bitmap,
			int flags) {
		EWAHCompressedBitmap compressed = bitmap.retrieveCompressed();
		compressed.trim();
		StoredBitmap newest = new StoredBitmap(c, compressed, null, flags);

		bitmapsToWriteXorBuffer.add(newest);
		if (bitmapsToWriteXorBuffer.size() > MAX_XOR_OFFSET_SEARCH) {
			bitmapsToWrite.add(
					generateStoredEntry(bitmapsToWriteXorBuffer.pollFirst()));
		}

		if (c.isAddToIndex()) {
			// The Bitmap map in the base class is used to make revwalks
			// efficient, so only add bitmaps that keep it efficient without
			// bloating memory.
			addBitmap(c, bitmap, flags);
		}
	}

	private StoredEntry generateStoredEntry(StoredBitmap bitmapToWrite) {
		int bestXorOffset = 0;
		EWAHCompressedBitmap bestBitmap = bitmapToWrite.getBitmap();

		int offset = 1;
		for (StoredBitmap curr : bitmapsToWriteXorBuffer) {
			EWAHCompressedBitmap bitmap = curr.getBitmap()
					.xor(bitmapToWrite.getBitmap());
			if (bitmap.sizeInBytes() < bestBitmap.sizeInBytes()) {
				bestBitmap = bitmap;
				bestXorOffset = offset;
			}
			offset++;
		}

		PositionEntry entry = positionEntries.get(bitmapToWrite);
		if (entry == null) {
			throw new IllegalStateException();
		}
		bestBitmap.trim();
		StoredEntry result = new StoredEntry(entry.namePosition, bestBitmap,
				bestXorOffset, bitmapToWrite.getFlags());

		return result;
	}

	/**
	 * Stores the bitmap for the objectId.
	 *
	 * @param objectId
	 *            the object id key for the bitmap.
	 * @param bitmap
	 *            the bitmap
	 * @param flags
	 *            the flags to be stored with the bitmap
	 */
	public void addBitmap(
			AnyObjectId objectId, EWAHCompressedBitmap bitmap, int flags) {
		bitmap.trim();
		StoredBitmap result = new StoredBitmap(objectId, bitmap, null, flags);
		getBitmaps().add(result);
	}

	/** {@inheritDoc} */
	@Override
	public EWAHCompressedBitmap ofObjectType(
			EWAHCompressedBitmap bitmap, int type) {
		switch (type) {
		case Constants.OBJ_BLOB:
			return getBlobs().and(bitmap);
		case Constants.OBJ_TREE:
			return getTrees().and(bitmap);
		case Constants.OBJ_COMMIT:
			return getCommits().and(bitmap);
		case Constants.OBJ_TAG:
			return getTags().and(bitmap);
		}
		throw new IllegalArgumentException();
	}

	/** {@inheritDoc} */
	@Override
	public int findPosition(AnyObjectId objectId) {
		PositionEntry entry = positionEntries.get(objectId);
		if (entry == null)
			return -1;
		return entry.offsetPosition;
	}

	/** {@inheritDoc} */
	@Override
	public ObjectId getObject(int position) throws IllegalArgumentException {
		ObjectId objectId = byOffset.get(position);
		if (objectId == null)
			throw new IllegalArgumentException();
		return objectId;
	}

	/**
	 * Get the commit object bitmap.
	 *
	 * @return the commit object bitmap.
	 */
	public EWAHCompressedBitmap getCommits() {
		return commits;
	}

	/**
	 * Get the tree object bitmap.
	 *
	 * @return the tree object bitmap.
	 */
	public EWAHCompressedBitmap getTrees() {
		return trees;
	}

	/**
	 * Get the blob object bitmap.
	 *
	 * @return the blob object bitmap.
	 */
	public EWAHCompressedBitmap getBlobs() {
		return blobs;
	}

	/**
	 * Get the tag object bitmap.
	 *
	 * @return the tag object bitmap.
	 */
	public EWAHCompressedBitmap getTags() {
		return tags;
	}

	/**
	 * Get the index storage options.
	 *
	 * @return the index storage options.
	 */
	public int getOptions() {
		return PackBitmapIndexV1.OPT_FULL;
	}

	/** {@inheritDoc} */
	@Override
	public int getBitmapCount() {
		return bitmapsToWriteXorBuffer.size() + bitmapsToWrite.size();
	}

	/**
	 * Remove all the bitmaps entries added.
	 *
	 * @param size
	 *            the expected number of bitmap entries to be written.
	 */
	public void resetBitmaps(int size) {
		getBitmaps().clear();
		bitmapsToWrite = new ArrayList<>(size);
	}

	/** {@inheritDoc} */
	@Override
	public int getObjectCount() {
		return byOffset.size();
	}

	/**
	 * Get list of xor compressed entries that need to be written.
	 *
	 * @return a list of the xor compressed entries.
	 */
	public List<StoredEntry> getCompressedBitmaps() {
		while (!bitmapsToWriteXorBuffer.isEmpty()) {
			bitmapsToWrite.add(
					generateStoredEntry(bitmapsToWriteXorBuffer.pollFirst()));
		}

		Collections.reverse(bitmapsToWrite);
		return bitmapsToWrite;
	}

	/** Data object for the on disk representation of a bitmap entry. */
	public static final class StoredEntry {
		private final long objectId;
		private final EWAHCompressedBitmap bitmap;
		private final int xorOffset;
		private final int flags;

		StoredEntry(long objectId, EWAHCompressedBitmap bitmap,
				int xorOffset, int flags) {
			this.objectId = objectId;
			this.bitmap = bitmap;
			this.xorOffset = xorOffset;
			this.flags = flags;
		}

		/** @return the bitmap */
		public EWAHCompressedBitmap getBitmap() {
			return bitmap;
		}

		/** @return the xorOffset */
		public int getXorOffset() {
			return xorOffset;
		}

		/** @return the flags */
		public int getFlags() {
			return flags;
		}

		/** @return the ObjectId */
		public long getObjectId() {
			return objectId;
		}
	}

	private static final class PositionEntry extends ObjectIdOwnerMap.Entry {
		final int namePosition;

		int offsetPosition;

		PositionEntry(AnyObjectId objectId, int namePosition) {
			super(objectId);
			this.namePosition = namePosition;
		}
	}
}
