/*
 * Copyright (C) 2017, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.reftable;

class ReftableConstants {
	static final byte[] FILE_HEADER_MAGIC = { 'R', 'E', 'F', 'T' };
	static final byte VERSION_1 = (byte) 1;

	static final int FILE_HEADER_LEN = 24;
	static final int FILE_FOOTER_LEN = 68;

	static final byte FILE_BLOCK_TYPE = 'R';
	static final byte REF_BLOCK_TYPE = 'r';
	static final byte OBJ_BLOCK_TYPE = 'o';
	static final byte LOG_BLOCK_TYPE = 'g';
	static final byte INDEX_BLOCK_TYPE = 'i';

	static final int VALUE_NONE = 0x0;
	static final int VALUE_1ID = 0x1;
	static final int VALUE_2ID = 0x2;
	static final int VALUE_SYMREF = 0x3;
	static final int VALUE_TYPE_MASK = 0x7;

	static final int LOG_NONE = 0x0;
	static final int LOG_DATA = 0x1;

	static final int MAX_BLOCK_SIZE = (1 << 24) - 1;
	static final int MAX_RESTARTS = 65535;

	static boolean isFileHeaderMagic(byte[] buf, int o, int n) {
		return (n - o) >= FILE_HEADER_MAGIC.length
				&& buf[o + 0] == FILE_HEADER_MAGIC[0]
				&& buf[o + 1] == FILE_HEADER_MAGIC[1]
				&& buf[o + 2] == FILE_HEADER_MAGIC[2]
				&& buf[o + 3] == FILE_HEADER_MAGIC[3];
	}

	static long reverseUpdateIndex(long time) {
		return 0xffffffffffffffffL - time;
	}

	private ReftableConstants() {
	}
}
