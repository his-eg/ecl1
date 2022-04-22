/*
 * Copyright (C) 2009-2022, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;

/**
 * Default resolver serving from the local filesystem.
 *
 * @param <C>
 *            type of connection
 */
public class FileResolver<C> implements RepositoryResolver<C> {
	private volatile boolean exportAll;

	private final Map<String, Repository> exports;

	private final Collection<File> exportBase;

	/**
	 * Initialize an empty file based resolver.
	 */
	public FileResolver() {
		exports = new ConcurrentHashMap<>();
		exportBase = new CopyOnWriteArrayList<>();
	}

	/**
	 * Create a new resolver for the given path.
	 *
	 * @param basePath
	 *            the base path all repositories are rooted under.
	 * @param exportAll
	 *            if true, exports all repositories, ignoring the check for the
	 *            {@code git-daemon-export-ok} files.
	 */
	public FileResolver(File basePath, boolean exportAll) {
		this();
		exportDirectory(basePath);
		setExportAll(exportAll);
	}

	/** {@inheritDoc} */
	@Override
	public Repository open(C req, String name)
			throws RepositoryNotFoundException, ServiceNotEnabledException {
		if (isUnreasonableName(name))
			throw new RepositoryNotFoundException(name);

		Repository db = exports.get(StringUtils.nameWithDotGit(name));
		if (db != null) {
			db.incrementOpen();
			return db;
		}

		for (File base : exportBase) {
			File dir = FileKey.resolve(new File(base, name), FS.DETECTED);
			if (dir == null)
				continue;

			try {
				FileKey key = FileKey.exact(dir, FS.DETECTED);
				db = RepositoryCache.open(key, true);
			} catch (IOException e) {
				throw new RepositoryNotFoundException(name, e);
			}

			try {
				if (isExportOk(req, name, db)) {
					// We have to leak the open count to the caller, they
					// are responsible for closing the repository if we
					// complete successfully.
					return db;
				}
				throw new ServiceNotEnabledException();

			} catch (RuntimeException | IOException e) {
				db.close();
				throw new RepositoryNotFoundException(name, e);

			} catch (ServiceNotEnabledException e) {
				db.close();
				throw e;
			}
		}

		if (exportBase.size() == 1) {
			File dir = new File(exportBase.iterator().next(), name);
			throw new RepositoryNotFoundException(name,
					new RepositoryNotFoundException(dir));
		}

		throw new RepositoryNotFoundException(name);
	}

	/**
	 * Whether <code>git-daemon-export-ok</code> is required to export a
	 * repository
	 *
	 * @return false if <code>git-daemon-export-ok</code> is required to export
	 *         a repository; true if <code>git-daemon-export-ok</code> is
	 *         ignored.
	 * @see #setExportAll(boolean)
	 */
	public boolean isExportAll() {
		return exportAll;
	}

	/**
	 * Set whether or not to export all repositories.
	 * <p>
	 * If false (the default), repositories must have a
	 * <code>git-daemon-export-ok</code> file to be accessed through this
	 * daemon.
	 * <p>
	 * If true, all repositories are available through the daemon, whether or
	 * not <code>git-daemon-export-ok</code> exists.
	 *
	 * @param export a boolean.
	 */
	public void setExportAll(boolean export) {
		exportAll = export;
	}

	/**
	 * Add a single repository to the set that is exported by this daemon.
	 * <p>
	 * The existence (or lack-thereof) of <code>git-daemon-export-ok</code> is
	 * ignored by this method. The repository is always published.
	 *
	 * @param name
	 *            name the repository will be published under.
	 * @param db
	 *            the repository instance.
	 */
	public void exportRepository(String name, Repository db) {
		exports.put(StringUtils.nameWithDotGit(name), db);
	}

	/**
	 * Recursively export all Git repositories within a directory.
	 *
	 * @param dir
	 *            the directory to export. This directory must not itself be a
	 *            git repository, but any directory below it which has a file
	 *            named <code>git-daemon-export-ok</code> will be published.
	 */
	public void exportDirectory(File dir) {
		exportBase.add(dir);
	}

	/**
	 * Check if this repository can be served.
	 * <p>
	 * The default implementation of this method returns true only if either
	 * {@link #isExportAll()} is true, or the {@code git-daemon-export-ok} file
	 * is present in the repository's directory.
	 *
	 * @param req
	 *            the current HTTP request.
	 * @param repositoryName
	 *            name of the repository, as present in the URL.
	 * @param db
	 *            the opened repository instance.
	 * @return true if the repository is accessible; false if not.
	 * @throws java.io.IOException
	 *             the repository could not be accessed, the caller will claim
	 *             the repository does not exist.
	 */
	protected boolean isExportOk(C req, String repositoryName, Repository db)
			throws IOException {
		if (isExportAll())
			return true;
		else if (db.getDirectory() != null)
			return new File(db.getDirectory(), "git-daemon-export-ok").exists(); //$NON-NLS-1$
		else
			return false;
	}

	private static boolean isUnreasonableName(String name) {
		if (name.length() == 0)
			return true; // no empty paths

		if (name.indexOf('\\') >= 0)
			return true; // no windows/dos style paths
		if (new File(name).isAbsolute())
			return true; // no absolute paths

		if (name.startsWith("../")) //$NON-NLS-1$
			return true; // no "l../etc/passwd"
		if (name.contains("/../")) //$NON-NLS-1$
			return true; // no "foo/../etc/passwd"
		if (name.contains("/./")) //$NON-NLS-1$
			return true; // "foo/./foo" is insane to ask
		if (name.contains("//")) //$NON-NLS-1$
			return true; // double slashes is sloppy, don't use it

		return false; // is a reasonable name
	}
}
