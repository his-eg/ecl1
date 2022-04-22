/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;

/**
 * Provides a way to populate a menu with a list of repositories.
 */
public final class RepositoryMenuUtil {

	private RepositoryMenuUtil() {
		// Utility class shall not be instantiated
	}

	/**
	 * Populates the given {@link IMenuManager} with a list of repositories.
	 * Each currently known configured repository is shown with its repository
	 * name and the path to the .git directory as tooltip; when a menu item is
	 * selected, the given {@code action} is invoked. Bare repositories can be
	 * excluded from the list. Menu items are sorted by repository name and .git
	 * directory paths.
	 *
	 * @param menuManager
	 *            to populate with the list of repositories
	 * @param includeBare
	 *            {@code true} if bare repositories should be included in the
	 *            list, {@code false} otherwise
	 * @param currentRepoDir
	 *            git directory of a repository that is to be marked as
	 *            "current"; may be {@code null}.
	 * @param action
	 *            to perform on the chosen repository
	 */
	public static void fillRepositories(@NonNull IMenuManager menuManager,
			boolean includeBare, @Nullable File currentRepoDir,
			@NonNull Consumer<Repository> action) {
		for (IAction item : getRepositoryActions(includeBare, currentRepoDir,
				action)) {
			menuManager.add(item);
		}
	}

	/**
	 * Creates for each configured repository an {@link IAction} that will
	 * perform the given {@code action} when invoked.
	 *
	 * @param includeBare
	 *            {@code true} if bare repositories should be included in the
	 *            list, {@code false} otherwise
	 * @param currentRepoDir
	 *            git directory of a repository that is to be marked as
	 *            "current"; may be {@code null}.
	 * @param action
	 *            to perform on the chosen repository
	 * @return the (possibly empty) list of actions
	 */
	@NonNull
	public static Collection<IAction> getRepositoryActions(boolean includeBare,
			@Nullable File currentRepoDir,
			@NonNull Consumer<Repository> action) {
		Set<String> repositories = RepositoryUtil.INSTANCE.getRepositories();
		Map<String, Set<File>> repos = new HashMap<>();
		for (String repo : repositories) {
			File gitDir = new File(repo);
			String name = null;
			try {
				Repository r = RepositoryCache.INSTANCE
						.lookupRepository(gitDir);
				if (!includeBare && r.isBare()) {
					continue;
				}
				name = RepositoryUtil.INSTANCE.getRepositoryName(r);
			} catch (IOException e) {
				continue;
			}
			repos.computeIfAbsent(name, key -> new HashSet<>()).add(gitDir);
		}
		String[] repoNames = repos.keySet().toArray(new String[0]);
		Arrays.sort(repoNames, CommonUtils.STRING_ASCENDING_COMPARATOR);
		List<IAction> result = new ArrayList<>();
		for (String repoName : repoNames) {
			Set<File> files = repos.get(repoName);
			File[] gitDirs = files.toArray(new File[0]);
			Arrays.sort(gitDirs);
			for (File f : gitDirs) {
				IAction menuItem = new Action(repoName,
						IAction.AS_RADIO_BUTTON) {
					@Override
					public void run() {
						try {
							Repository r = RepositoryCache.INSTANCE
									.lookupRepository(f);
							action.accept(r);
						} catch (IOException e) {
							Activator.showError(e.getLocalizedMessage(), e);
						}
					}
				};
				menuItem.setToolTipText(f.getPath());
				if (f.equals(currentRepoDir)) {
					menuItem.setChecked(true);
				}
				result.add(menuItem);
			}
		}
		return result;
	}

	/**
	 * Utility class facilitating creating toolbar actions that show a drop-down
	 * menu of all registered repositories, performing a given action on a
	 * selected repository.
	 */
	public static class RepositoryToolbarAction extends DropDownMenuAction {

		private final IEclipsePreferences preferences = RepositoryUtil.INSTANCE
				.getPreferences();

		private final IPreferenceChangeListener listener;

		private final @NonNull Consumer<Repository> action;

		private final @NonNull Supplier<Repository> currentRepo;

		private final boolean includeBare;

		/**
		 * Creates a new {@link RepositoryToolbarAction} with the given
		 * {@code action} and default text, image, and tooltip.
		 *
		 * @param includeBare
		 *            {@code true} if bare repositories shall be included,
		 *            {@code false} otherwise
		 * @param currentRepo
		 *            supplying the "current" repository, if any, or
		 *            {@code null} otherwise
		 * @param action
		 *            to run when a repository is selected from the drop-down
		 *            menu
		 */
		public RepositoryToolbarAction(boolean includeBare,
				@NonNull Supplier<Repository> currentRepo,
				@NonNull Consumer<Repository> action) {
			this(UIText.RepositoryToolbarAction_label, UIIcons.REPOSITORY,
					UIText.RepositoryToolbarAction_tooltip, includeBare,
					currentRepo, action);
		}

		/**
		 * Creates a new {@link RepositoryToolbarAction} with the given text and
		 * the given {@code action}.
		 *
		 * @param text
		 *            for the action
		 * @param image
		 *            for the action
		 * @param tooltip
		 *            for the action
		 * @param includeBare
		 *            {@code true} if bare repositories shall be included,
		 *            {@code false} otherwise
		 * @param currentRepo
		 *            supplying the "current" repository, if any, or
		 *            {@code null} otherwise
		 * @param action
		 *            to run when a repository is selected from the drop-down
		 *            menu
		 */
		public RepositoryToolbarAction(String text,
				@Nullable ImageDescriptor image, @Nullable String tooltip,
				boolean includeBare, @NonNull Supplier<Repository> currentRepo,
				@NonNull Consumer<Repository> action) {
			super(text);
			setImageDescriptor(image);
			setToolTipText(tooltip == null ? text : tooltip);
			this.includeBare = includeBare;
			this.currentRepo = currentRepo;
			this.action = action;
			this.listener = event -> {
				if (RepositoryUtil.PREFS_DIRECTORIES_REL
						.equals(event.getKey())) {
					setEnabled(!RepositoryUtil.INSTANCE.getRepositories()
							.isEmpty());
				}
			};
			setEnabled(!RepositoryUtil.INSTANCE.getRepositories().isEmpty());
			preferences.addPreferenceChangeListener(listener);
		}


		@Override
		public Collection<IContributionItem> getActions() {
			Repository current = currentRepo.get();
			File gitDir = current == null ? null : current.getDirectory();
			return RepositoryMenuUtil.getRepositoryActions(includeBare, gitDir,
					action).stream().map(ActionContributionItem::new)
					.collect(Collectors.toList());
		}

		@Override
		public void dispose() {
			preferences.removePreferenceChangeListener(listener);
			super.dispose();
		}
	}
}
