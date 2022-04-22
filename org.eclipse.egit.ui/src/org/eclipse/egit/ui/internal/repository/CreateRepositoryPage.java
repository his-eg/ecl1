/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.util.RepositoryPathChecker;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Asks for a directory and whether to create a bare repository
 */
public class CreateRepositoryPage extends WizardPage {
	private final boolean hideBare;

	private Text directoryText;

	private Text defaultBranchText;

	private Button bareButton;

	/**
	 * Constructs this page
	 *
	 * @param hideBareOption
	 */
	public CreateRepositoryPage(boolean hideBareOption) {
		super(CreateRepositoryPage.class.getName());
		this.hideBare = hideBareOption;
		setTitle(UIText.CreateRepositoryPage_PageTitle);
		setMessage(UIText.CreateRepositoryPage_PageMessage);
		// we must at least enter the directory
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		Label directoryLabel = new Label(main, SWT.NONE);
		directoryLabel.setText(UIText.CreateRepositoryPage_DirectoryLabel);
		directoryText = new Text(main, SWT.BORDER);
		String initialDirectory = RepositoryUtil.getDefaultRepositoryDir();
		int cursorPosition = initialDirectory.length();
		if (!initialDirectory.isEmpty()) {
			initialDirectory = RepositoryUtil.getDefaultRepositoryDir()
					+ File.separatorChar
					+ UIText.CreateRepositoryPage_DefaultRepositoryName;
			int repoCounter = 2;
			while (Paths.get(initialDirectory).toFile().exists()) {
				initialDirectory = RepositoryUtil.getDefaultRepositoryDir()
						+ File.separatorChar
						+ UIText.CreateRepositoryPage_DefaultRepositoryName + repoCounter++;
			}
			cursorPosition++;
		}
		directoryText.setText(initialDirectory);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(directoryText);
		Button browseButton = new Button(main, SWT.PUSH);
		browseButton.setText(UIText.CreateRepositoryPage_BrowseButton);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String previous = directoryText.getText();
				File previousFile = new File(previous);
				String result;
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage(UIText.CreateRepositoryPage_PageMessage);
				if (previousFile.exists() && previousFile.isDirectory()) {
					dialog.setFilterPath(previousFile.getPath());
				}
				result = dialog.open();
				if (result != null)
					directoryText.setText(result);
			}
		});

		Label defaultBranchLabel = new Label(main, SWT.NONE);
		defaultBranchLabel
				.setText(UIText.CreateRepositoryPage_DefaultBranchLabel);
		defaultBranchText = new Text(main, SWT.BORDER);
		String defaultBranch;
		try {
			defaultBranch = RepositoryUtil.getDefaultBranchName();
			if (StringUtils.isEmptyOrNull(defaultBranch)) {
				defaultBranch = Constants.MASTER;
			}
		} catch (ConfigInvalidException | IOException e) {
			defaultBranch = Constants.MASTER;
			Activator.handleError(
					UIText.CreateRepositoryPage_ReadDefaultBranchFailed, e,
					true);
		}
		defaultBranchText.setText(defaultBranch);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(defaultBranchText);

		defaultBranchText.addModifyListener(event -> checkPage());

		if (!hideBare) {
			bareButton = new Button(main, SWT.CHECK);
			bareButton.setText(UIText.CreateRepositoryPage_BareCheckbox);
			GridDataFactory.fillDefaults().indent(10, 0).span(3, 1)
					.applyTo(bareButton);
			bareButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					checkPage();
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					checkPage();
				}
			});
		}

		directoryText.addModifyListener(event -> checkPage());

		setControl(main);
		directoryText.setFocus();
		directoryText.setSelection(cursorPosition,
				directoryText.getText().length());
		if (!directoryText.getText().isEmpty()) {
			// enforce validation if a default repository directory is set.
			// Otherwise the wizards initial validation state would be different
			// than when entering the same directory text manually
			checkPage();
		}
	}

	/**
	 * @return the directory where to create the Repository (with operating
	 *         system specific path separators)
	 */
	public String getDirectory() {
		IPath path = new Path(directoryText.getText().trim());
		return path.toOSString();
	}

	/**
	 * Get the default branch of the new repository.
	 *
	 * @return the default branch of the new repository
	 */
	public String getDefaultBranch() {
		return defaultBranchText.getText().trim();
	}

	/**
	 * @return <code>true</code> if a bare Repository is to be created
	 */
	public boolean getBare() {
		return bareButton != null && bareButton.getSelection();
	}

	void checkPage() {
		setErrorMessage(null);
		try {
			String dir = directoryText.getText().trim();

			if (dir.length() == 0) {
				setErrorMessage(UIText.CreateRepositoryPage_PleaseSelectDirectoryMessage);
				return;
			}

			RepositoryPathChecker checker = new RepositoryPathChecker();
			if (!checker.check(dir)) {
				setErrorMessage(checker.getErrorMessage());
				return;
			}
			if (checker.hasContent()) {
				if (getBare()) {
					setErrorMessage(NLS.bind(
							UIText.CreateRepositoryPage_NotEmptyMessage, dir));
					return;
				} else {
					setMessage(NLS.bind(
							UIText.CreateRepositoryPage_NotEmptyMessage, dir),
							IMessageProvider.INFORMATION);
				}
			} else {
				setMessage(UIText.CreateRepositoryPage_PageMessage);
			}

			String defaultBranch = getDefaultBranch();
			if (!Repository.isValidRefName(Constants.R_HEADS + defaultBranch)) {
				setErrorMessage(MessageFormat.format(
						UIText.CreateRepositoryPage_InvalidBranchName,
						defaultBranch));
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}
}
