/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.sf.ecl1.importwizard;

import h1modules.utilities.utils.Activator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.his.cs.sys.extensions.wizards.utils.HISConstants;
import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;


public class ExtensionImportWizardPage extends WizardPage {

    // All extensions existing on repo server
    private final Set<String> remoteExtensions = new TreeSet<String>();

    // All extensions existing in workspace
    private final Set<String> extensionsInWorkspace = new TreeSet<String>();

    private Table projectTable;

    private Button openAfterImport;

    private Button deleteExistingFolders;

    protected ExtensionImportWizardPage(String pageName) {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent) {
        initRemoteExtensions();
        initExtensionsInWorkspace();
        GridLayout gl = new GridLayout(1, false);
        GridLayout gl2 = new GridLayout(2, false);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

        parent.setLayout(gl);

        Label branchInfo = new Label(parent, SWT.BORDER | SWT.LEFT);
        branchInfo.setText("Branch: " + getBranch());
        Composite openAfterImportComposite = new Composite(parent, SWT.BORDER | SWT.TOP);
        openAfterImportComposite.setLayout(gl2);
        Label question = new Label(openAfterImportComposite, SWT.TOP);
        question.setText("Open extensions after import?");
        openAfterImport = new Button(openAfterImportComposite, SWT.CHECK);
        openAfterImport.setSelection(true);

        Composite projectChoice = new Composite(parent, SWT.BORDER | SWT.TOP);
        projectChoice.setLayout(gl);
        projectChoice.setLayoutData(layoutData);

        Label projectChoiceLabel = new Label(projectChoice, SWT.TOP);
        projectChoiceLabel.setText("Importable Projects");

        final Button selectAllButton = new Button(projectChoice, SWT.CHECK);
        selectAllButton.setText("Select all");
        selectAllButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSelect(selectAllButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleSelect(selectAllButton.getSelection());
            }

            private void handleSelect(boolean selection) {
                for (TableItem item : projectTable.getItems()) {
                    item.setChecked(selection);
                }
            }
        });

        deleteExistingFolders = new Button(projectChoice, SWT.CHECK);
        deleteExistingFolders.setText("Delete folders?");
        deleteExistingFolders.setToolTipText("Should wizard delete existing folders named like extensions for import?");

        projectTable = new Table(projectChoice, SWT.MULTI | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        projectTable.setLinesVisible(true);
        projectTable.setHeaderVisible(true);
        projectTable.setLayoutData(layoutData);
        projectTable.setSize(200, 600);

        String[] headers = { "Import?", "Name" };
        for (String header : headers) {
            TableColumn c = new TableColumn(projectTable, SWT.NONE);
            c.setText(header);
        }

        for (String remoteExtensionName : remoteExtensions) {
            if (!extensionsInWorkspace.contains(remoteExtensionName) && !remoteExtensionName.contains(HISConstants.WEBAPPS)) {
                TableItem tableItem = new TableItem(projectTable, SWT.NONE);
                tableItem.setChecked(false);
                tableItem.setText(1, remoteExtensionName);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            projectTable.getColumn(i).pack();
        }

        setControl(parent);
    }

    private void initRemoteExtensions() {
        Collection<String> remoteProjectsIncludingBranch = new RemoteProjectSearchSupport().getProjects();
        String branch = getBranch();
        for (String remoteProjectIncludingBranch : remoteProjectsIncludingBranch) {
            String remoteProject = remoteProjectIncludingBranch.replace("_" + branch, "");
            System.out.println("Replacing original '" + remoteProjectIncludingBranch + "' with '" + remoteProject + "'");
            remoteExtensions.add(remoteProject);
        }
    }

    private void initExtensionsInWorkspace() {
        Collection<String> result = new ArrayList<String>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            String name = iProject.getName();
            result.add(name);
        }
        extensionsInWorkspace.addAll(result);
    }

    private String getBranch() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        return store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE);
    }

    /**
     * Get all user selected extensions for import
     *
     * @return set of extension names
     */
    public Set<String> getSelectedExtensions() {
        Set<String> result = new TreeSet<String>();
        TableItem[] items = projectTable.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                String text = item.getText(1);
                result.add(text);
            }
        }
        return result;
    }

    /**
     * User selection if extensions should be opend after import
     *
     * @return true iff user wants extensions to be opened after import
     */
    public boolean openProjectsAfterImport() {
        return openAfterImport.getSelection();
    }

    /**
     * User selection if folders with names like extensions for import should be deleted directly
     *
     * @return true iff user wants to have folders deleted
     */
    public boolean deleteFolders() {
        return deleteExistingFolders.getSelection();
    }

}
