/*
 * Copyright (C) 2015 Obeo. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.hooks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * The <code>commit-msg</code> hook implementation. This hook is run before the
 * commit and can reject the commit. It passes one argument to the hook script,
 * which is the path to the COMMIT_MSG file, relative to the repository
 * workTree.
 *
 * @since 4.0
 */
public class CommitMsgHook extends GitHook<String> {

	/**
	 * Constant indicating the name of the commit-smg hook.
	 */
	public static final String NAME = "commit-msg"; //$NON-NLS-1$

	/**
	 * The commit message.
	 */
	private String commitMessage;

	/**
	 * Constructor for CommitMsgHook
	 * <p>
	 * This constructor will use the default error stream.
	 * </p>
	 *
	 * @param repo
	 *            The repository
	 * @param outputStream
	 *            The output stream the hook must use. {@code null} is allowed,
	 *            in which case the hook will use {@code System.out}.
	 */
	protected CommitMsgHook(Repository repo, PrintStream outputStream) {
		super(repo, outputStream);
	}

	/**
	 * Constructor for CommitMsgHook
	 *
	 * @param repo
	 *            The repository
	 * @param outputStream
	 *            The output stream the hook must use. {@code null} is allowed,
	 *            in which case the hook will use {@code System.out}.
	 * @param errorStream
	 *            The error stream the hook must use. {@code null} is allowed,
	 *            in which case the hook will use {@code System.err}.
	 * @since 5.6
	 */
	protected CommitMsgHook(Repository repo, PrintStream outputStream,
			PrintStream errorStream) {
		super(repo, outputStream, errorStream);
	}

	/** {@inheritDoc} */
	@Override
	public String call() throws IOException, AbortedByHookException {
		if (commitMessage == null) {
			throw new IllegalStateException();
		}
		if (canRun()) {
			getRepository().writeCommitEditMsg(commitMessage);
			doRun();
			commitMessage = getRepository().readCommitEditMsg();
		}
		return commitMessage;
	}

	/**
	 * @return {@code true} if and only if the path to the message commit file
	 *         is not null (which would happen in a bare repository) and the
	 *         commit message is also not null.
	 */
	private boolean canRun() {
		return getCommitEditMessageFilePath() != null && commitMessage != null;
	}

	/** {@inheritDoc} */
	@Override
	public String getHookName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This hook receives one parameter, which is the path to the file holding
	 * the current commit-msg, relative to the repository's work tree.
	 */
	@Override
	protected String[] getParameters() {
		return new String[] { getCommitEditMessageFilePath() };
	}

	/**
	 * @return The path to the commit edit message file relative to the
	 *         repository's work tree, or null if the repository is bare.
	 */
	private String getCommitEditMessageFilePath() {
		File gitDir = getRepository().getDirectory();
		if (gitDir == null) {
			return null;
		}
		return Repository.stripWorkDir(getRepository().getWorkTree(), new File(
				gitDir, Constants.COMMIT_EDITMSG));
	}

	/**
	 * It is mandatory to call this method with a non-null value before actually
	 * calling the hook.
	 *
	 * @param commitMessage
	 *            The commit message before the hook has run.
	 * @return {@code this} for convenience.
	 */
	public CommitMsgHook setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
		return this;
	}

}
