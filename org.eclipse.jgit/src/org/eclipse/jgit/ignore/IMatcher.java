/*
 * Copyright (C) 2014, 2020 Andrey Loskutov <loskutov@gmx.de> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.ignore;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.ignore.internal.PathMatcher;

/**
 * Generic path matcher.
 *
 * @since 5.7
 */
public interface IMatcher {

	/**
	 * Matcher that does not match any pattern.
	 */
	public static final IMatcher NO_MATCH = new IMatcher() {

		@Override
		public boolean matches(String path, boolean assumeDirectory,
				boolean pathMatch) {
			return false;
		}

		@Override
		public boolean matches(String segment, int startIncl, int endExcl) {
			return false;
		}
	};

	/**
	 * Creates a path matcher for the given pattern. A pattern may contain the
	 * wildcards "?", "*", and "**". The directory separator is '/'.
	 *
	 * @param pattern
	 *            to match
	 * @param dirOnly
	 *            whether to match only directories
	 * @return a matcher for the given pattern
	 * @throws InvalidPatternException
	 *             if the pattern is invalid
	 */
	@NonNull
	public static IMatcher createPathMatcher(@NonNull String pattern,
			boolean dirOnly) throws InvalidPatternException {
		return PathMatcher.createPathMatcher(pattern,
				Character.valueOf(FastIgnoreRule.PATH_SEPARATOR), dirOnly);
	}

	/**
	 * Matches entire given string
	 *
	 * @param path
	 *            string which is not null, but might be empty
	 * @param assumeDirectory
	 *            true to assume this path as directory (even if it doesn't end
	 *            with a slash)
	 * @param pathMatch
	 *            {@code true} if the match is for the full path: prefix-only
	 *            matches are not allowed
	 * @return true if this matcher pattern matches given string
	 */
	boolean matches(String path, boolean assumeDirectory, boolean pathMatch);

	/**
	 * Matches only part of given string
	 *
	 * @param segment
	 *            string which is not null, but might be empty
	 * @param startIncl
	 *            start index, inclusive
	 * @param endExcl
	 *            end index, exclusive
	 * @return true if this matcher pattern matches given string
	 */
	boolean matches(String segment, int startIncl, int endExcl);
}
