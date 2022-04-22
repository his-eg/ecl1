/*
 * Copyright (c) 2020 Julian Ruppel <julian.ruppel@sap.com>
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Config.ConfigEnum;
import org.eclipse.jgit.lib.Config.SectionParser;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * The standard "commit" configuration parameters.
 *
 * @since 5.13
 */
public class CommitConfig {

	/**
	 * Key for {@link Config#get(SectionParser)}.
	 */
	public static final Config.SectionParser<CommitConfig> KEY = CommitConfig::new;

	private static final String CUT = " ------------------------ >8 ------------------------\n"; //$NON-NLS-1$

	/**
	 * How to clean up commit messages when committing.
	 *
	 * @since 6.1
	 */
	public enum CleanupMode implements ConfigEnum {

		/**
		 * {@link #WHITESPACE}, additionally remove comment lines.
		 */
		STRIP,

		/**
		 * Remove trailing whitespace and leading and trailing empty lines;
		 * collapse multiple empty lines to a single one.
		 */
		WHITESPACE,

		/**
		 * Make no changes.
		 */
		VERBATIM,

		/**
		 * Omit everything from the first "scissor" line on, then apply
		 * {@link #WHITESPACE}.
		 */
		SCISSORS,

		/**
		 * Use {@link #STRIP} for user-edited messages, otherwise
		 * {@link #WHITESPACE}, unless overridden by a git config setting other
		 * than DEFAULT.
		 */
		DEFAULT;

		@Override
		public String toConfigValue() {
			return name().toLowerCase(Locale.ROOT);
		}

		@Override
		public boolean matchConfigValue(String in) {
			return toConfigValue().equals(in);
		}
	}

	private final static Charset DEFAULT_COMMIT_MESSAGE_ENCODING = StandardCharsets.UTF_8;

	private String i18nCommitEncoding;

	private String commitTemplatePath;

	private CleanupMode cleanupMode;

	private CommitConfig(Config rc) {
		commitTemplatePath = rc.getString(ConfigConstants.CONFIG_COMMIT_SECTION,
				null, ConfigConstants.CONFIG_KEY_COMMIT_TEMPLATE);
		i18nCommitEncoding = rc.getString(ConfigConstants.CONFIG_SECTION_I18N,
				null, ConfigConstants.CONFIG_KEY_COMMIT_ENCODING);
		cleanupMode = rc.getEnum(ConfigConstants.CONFIG_COMMIT_SECTION, null,
				ConfigConstants.CONFIG_KEY_CLEANUP, CleanupMode.DEFAULT);
	}

	/**
	 * Get the path to the commit template as defined in the git
	 * {@code commit.template} property.
	 *
	 * @return the path to commit template or {@code null} if not present.
	 */
	@Nullable
	public String getCommitTemplatePath() {
		return commitTemplatePath;
	}

	/**
	 * Get the encoding of the commit as defined in the git
	 * {@code i18n.commitEncoding} property.
	 *
	 * @return the encoding or {@code null} if not present.
	 */
	@Nullable
	public String getCommitEncoding() {
		return i18nCommitEncoding;
	}

	/**
	 * Retrieves the {@link CleanupMode} as given by git config
	 * {@code commit.cleanup}.
	 *
	 * @return the {@link CleanupMode}; {@link CleanupMode#DEFAULT} if the git
	 *         config is not set
	 * @since 6.1
	 */
	@NonNull
	public CleanupMode getCleanupMode() {
		return cleanupMode;
	}

	/**
	 * Computes a non-default {@link CleanupMode} from the given mode and the
	 * git config.
	 *
	 * @param mode
	 *            {@link CleanupMode} to resolve
	 * @param defaultStrip
	 *            if {@code true} return {@link CleanupMode#STRIP} if the git
	 *            config is also "default", otherwise return
	 *            {@link CleanupMode#WHITESPACE}
	 * @return the {@code mode}, if it is not {@link CleanupMode#DEFAULT},
	 *         otherwise the resolved mode, which is never
	 *         {@link CleanupMode#DEFAULT}
	 * @since 6.1
	 */
	@NonNull
	public CleanupMode resolve(@NonNull CleanupMode mode,
			boolean defaultStrip) {
		if (CleanupMode.DEFAULT == mode) {
			CleanupMode defaultMode = getCleanupMode();
			if (CleanupMode.DEFAULT == defaultMode) {
				return defaultStrip ? CleanupMode.STRIP
						: CleanupMode.WHITESPACE;
			}
			return defaultMode;
		}
		return mode;
	}

	/**
	 * Get the content to the commit template as defined in
	 * {@code commit.template}. If no {@code i18n.commitEncoding} is specified,
	 * UTF-8 fallback is used.
	 *
	 * @param repository
	 *            to resolve relative path in local git repo config
	 *
	 * @return content of the commit template or {@code null} if not present.
	 * @throws IOException
	 *             if the template file can not be read
	 * @throws FileNotFoundException
	 *             if the template file does not exists
	 * @throws ConfigInvalidException
	 *             if a {@code commitEncoding} is specified and is invalid
	 * @since 6.0
	 */
	@Nullable
	public String getCommitTemplateContent(@NonNull Repository repository)
			throws FileNotFoundException, IOException, ConfigInvalidException {

		if (commitTemplatePath == null) {
			return null;
		}

		File commitTemplateFile;
		FS fileSystem = repository.getFS();
		if (commitTemplatePath.startsWith("~/")) { //$NON-NLS-1$
			commitTemplateFile = fileSystem.resolve(fileSystem.userHome(),
					commitTemplatePath.substring(2));
		} else {
			commitTemplateFile = fileSystem.resolve(null, commitTemplatePath);
		}
		if (!commitTemplateFile.isAbsolute()) {
			commitTemplateFile = fileSystem.resolve(
					repository.getWorkTree().getAbsoluteFile(),
					commitTemplatePath);
		}

		Charset commitMessageEncoding = getEncoding();
		return RawParseUtils.decode(commitMessageEncoding,
				IO.readFully(commitTemplateFile));

	}

	private Charset getEncoding() throws ConfigInvalidException {
		Charset commitMessageEncoding = DEFAULT_COMMIT_MESSAGE_ENCODING;

		if (i18nCommitEncoding == null) {
			return null;
		}

		try {
			commitMessageEncoding = Charset.forName(i18nCommitEncoding);
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			throw new ConfigInvalidException(MessageFormat.format(
					JGitText.get().invalidEncoding, i18nCommitEncoding), e);
		}

		return commitMessageEncoding;
	}

	/**
	 * Processes a text according to the given {@link CleanupMode}.
	 *
	 * @param text
	 *            text to process
	 * @param mode
	 *            {@link CleanupMode} to use
	 * @param commentChar
	 *            comment character (normally {@code #}) to use if {@code mode}
	 *            is {@link CleanupMode#STRIP} or {@link CleanupMode#SCISSORS}
	 * @return the processed text
	 * @throws IllegalArgumentException
	 *             if {@code mode} is {@link CleanupMode#DEFAULT} (use
	 *             {@link #resolve(CleanupMode, boolean)} first)
	 * @since 6.1
	 */
	public static String cleanText(@NonNull String text,
			@NonNull CleanupMode mode, char commentChar) {
		String toProcess = text;
		boolean strip = false;
		switch (mode) {
		case VERBATIM:
			return text;
		case SCISSORS:
			String cut = commentChar + CUT;
			if (text.startsWith(cut)) {
				return ""; //$NON-NLS-1$
			}
			int cutPos = text.indexOf('\n' + cut);
			if (cutPos >= 0) {
				toProcess = text.substring(0, cutPos + 1);
			}
			break;
		case STRIP:
			strip = true;
			break;
		case WHITESPACE:
			break;
		case DEFAULT:
		default:
			// Internal error; no translation
			throw new IllegalArgumentException("Invalid clean-up mode " + mode); //$NON-NLS-1$
		}
		// WHITESPACE
		StringBuilder result = new StringBuilder();
		boolean lastWasEmpty = true;
		for (String line : toProcess.split("\n")) { //$NON-NLS-1$
			line = line.stripTrailing();
			if (line.isEmpty()) {
				if (!lastWasEmpty) {
					result.append('\n');
					lastWasEmpty = true;
				}
			} else if (!strip || !isComment(line, commentChar)) {
				lastWasEmpty = false;
				result.append(line).append('\n');
			}
		}
		int bufferSize = result.length();
		if (lastWasEmpty && bufferSize > 0) {
			bufferSize--;
			result.setLength(bufferSize);
		}
		if (bufferSize > 0 && !toProcess.endsWith("\n")) { //$NON-NLS-1$
			if (result.charAt(bufferSize - 1) == '\n') {
				result.setLength(bufferSize - 1);
			}
		}
		return result.toString();
	}

	private static boolean isComment(String text, char commentChar) {
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char ch = text.charAt(i);
			if (!Character.isWhitespace(ch)) {
				return ch == commentChar;
			}
		}
		return false;
	}
}