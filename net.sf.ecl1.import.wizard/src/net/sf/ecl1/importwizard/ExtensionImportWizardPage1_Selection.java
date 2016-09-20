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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import de.his.cs.sys.extensions.wizards.utils.HISConstants;
import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;

/**
 * Extension import configuration wizard page.
 *
 * @author keunecke
 */
public class ExtensionImportWizardPage1_Selection extends ExtensionImportWizardPage {

	private static final String PAGE_NAME = "page1";
	private static final String PAGE_DESCRIPTION = "Extension Import - Selection";

	// top-level container for this page
	private Composite container;

    private Table projectTable;

    // TODO: Move to page 2?
    private Button openAfterImport;

    // TODO: Move to page 2?
    private Button deleteExistingFolders;
	
    // Extension Import Wizard data model
    private ExtensionImportWizardModel model;
    
    /**
     * Create first ExtensionImportWizardPage, containing the extension selection dialog.
     * @param model
     */
    protected ExtensionImportWizardPage1_Selection(ExtensionImportWizardModel model) {
        super(PAGE_NAME);
        this.setDescription(PAGE_DESCRIPTION);
        this.model = model;
    }

    @Override
    public void createControl(Composite parent) {
        System.out.println("createControls() for page 1");
        
        container = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        container.setLayout(gl);
        GridLayout gl2 = new GridLayout(2, false);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

        Label branchInfo = new Label(container, SWT.TOP);
        branchInfo.setText("Branch: " + model.getBranch());
        Composite openAfterImportComposite = new Composite(container, SWT.BORDER | SWT.TOP);
        openAfterImportComposite.setLayout(gl2);
        openAfterImport = new Button(openAfterImportComposite, SWT.CHECK);
        openAfterImport.setText("Open extensions after import?");
        openAfterImport.setToolTipText("Should wizard open extension projects after import?");
        openAfterImport.setSelection(true);

        deleteExistingFolders = new Button(openAfterImportComposite, SWT.CHECK);
        deleteExistingFolders.setText("Delete folders?");
        deleteExistingFolders.setToolTipText("Should wizard delete existing folders named like extensions for import?");

        Composite projectChoice = new Composite(container, SWT.BORDER | SWT.TOP);
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

        Set<String> extensionsInWorkspace = model.getExtensionsInWorkspace();
        for (String remoteExtensionName : model.getRemoteExtensions()) {
            if (!extensionsInWorkspace.contains(remoteExtensionName) && !remoteExtensionName.contains(HISConstants.WEBAPPS)) {
                TableItem tableItem = new TableItem(projectTable, SWT.NONE);
                tableItem.setChecked(false);
                tableItem.setText(1, remoteExtensionName);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            projectTable.getColumn(i).pack();
        }

        setControl(container);
        setPageComplete(true); // enable "next"-button
    }

	@Override
	void onEnterPage() {
		// nothing to do so far
	}

    @Override
    public IWizardPage getPreviousPage() {
    	return null;
    }
    
    /**
     * Dynamically prepare data for page 2 from results of this page, and return page 2.
     * @return complete page 2
     */
    // Implementation note: Overriding the getPreviousPage() and getNextPage() methods to permit data processing
    // between page transitions is simpler than overriding Wizard.getNextPage(), because we don't need case distinctions.
    @Override
    public IWizardPage getNextPage() {
    	this.initSelectedExtensions();
    	ExtensionImportWizardPage2_Confirmation page2 = ((ExtensionImportWizard) this.getWizard()).page2;
		page2.onEnterPage();
    	return page2;
    }

    /**
     * Get user selected extensions to import from page 1
     */
    private void initSelectedExtensions() {
        Set<String> selectedExtensions = new TreeSet<String>();
        TableItem[] items = projectTable.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                String text = item.getText(1);
                selectedExtensions.add(text);
            }
        }
        model.setSelectedExtensions(selectedExtensions);
    }
    
    /**
     * User selection if extensions should be opened after import
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

    /**
     * Make project table visible to page 2
     * @return project table
     */
    Table getProjectTable() {
    	return projectTable;
    }
}
