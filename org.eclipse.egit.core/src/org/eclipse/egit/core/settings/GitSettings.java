/*******************************************************************************
 * Copyright (C) 2020 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.settings;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.util.StringUtils;

/**
 * API to access some core EGit settings.
 *
 * @since 5.10
 */
public final class GitSettings {

	private GitSettings() {
		// No instantiation
	}

	/**
	 * Retrieves the configured connection timeout in seconds.
	 *
	 * @return the configured connection timeout in seconds; 60 seconds by
	 *         default.
	 */
	public static int getRemoteConnectionTimeout() {
		return Platform.getPreferencesService().getInt(Activator.PLUGIN_ID,
				GitCorePreferences.core_remoteConnectionTimeout, 60, null);
	}

	/**
	 * Retrieves the {@link Path} of the default directory where to create new
	 * clones.
	 *
	 * @return the {@link Path} to the directory
	 */
	public static Path getDefaultRepositoryDir() {
		return Paths.get(RepositoryUtil.getDefaultRepositoryDir());
	}

	/**
	 * Retrieves the {@link File} set in the EGit preferences for a GPG
	 * executable.
	 *
	 * @return the file, {@code null} if not set, invalid or not executable
	 * @since 5.11
	 */
	public static File getGpgExecutable() {
		String result = Platform.getPreferencesService().getString(
				Activator.PLUGIN_ID, GitCorePreferences.core_gpgExecutable,
				null, null);
		if (!StringUtils.isEmptyOrNull(result)) {
			try {
				// Use Paths.get; it validates more.
				File exe = Paths.get(result).toFile();
				if (exe.isFile() && exe.canExecute()) {
					return exe.getAbsoluteFile();
				} else {
					Activator.logError(MessageFormat.format(
							CoreText.GitSettings_gpgNotExecutable, result),
							null);
				}
			} catch (Exception e) {
				Activator.logError(MessageFormat.format(
						CoreText.GitSettings_gpgInvalidExecutable, result),
						e);
			}
		}
		return null;
	}

	/**
	 * Retrieves the set of absolute paths to all repositories configured in
	 * EGit.
	 * <p>
	 * Note that there is no guarantee that all the paths returned correspond to
	 * existing directories. Repositories can be deleted outside of EGit.
	 * </p>
	 *
	 * @return a collection of absolute path strings pointing to the git
	 *         directories of repositories configured in EGit.
	 */
	public static Collection<Path> getConfiguredRepositoryDirectories() {
		return RepositoryUtil.INSTANCE.getRepositories()
				.stream().map(Paths::get).collect(Collectors.toSet());
	}

	/**
	 * Adds a repository to the list of repositories configured in EGit.
	 *
	 * @param gitDir
	 *            to add; must not be {@code null}
	 * @throws IllegalArgumentException
	 *             if {@code gitDir} does not "look like" a git repository
	 *             directory
	 */
	public static void addConfiguredRepository(Path gitDir)
			throws IllegalArgumentException {
		RepositoryUtil.INSTANCE.addConfiguredRepository(
				Objects.requireNonNull(gitDir).toFile());
	}
}
