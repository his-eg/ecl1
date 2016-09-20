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

import java.util.Set;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Extension import configuration wizard, page 2 handling dependent extensions.
 * 
 * @author tneumann#his.de
 */
public class ExtensionImportWizardPage2_Confirmation extends WizardPage {
	
	private static final String PAGE_NAME = "page2";
	private static final String PAGE_DESCRIPTION = "Extension Import - Dependencies";

	// top-level container for this page
	private Composite container;

	private Label userSelectedInfo;
	private Table userSelectedTable;
	private TableColumn userSelectedTableColumn;
	
	private Label dependentInfo;
	private Table dependentTable;
	private TableColumn dependentTableColumn;
	
    // Extension Import Wizard data model
    private ExtensionImportWizardModel model;
    
    /**
     * Create second ExtensionImportWizardPage, containing the confirmation dialog.
     * @param model
     */
    protected ExtensionImportWizardPage2_Confirmation(ExtensionImportWizardModel model) {
        super(PAGE_NAME);
        this.setDescription(PAGE_DESCRIPTION);
        this.model = model;
    }
    
    @Override
    public void createControl(Composite parent) {
        System.out.println("create controls for page 2");
        
        container = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        container.setLayout(gl);

    	// show user-selected extensions
        userSelectedInfo = new Label(container, SWT.TOP);
        userSelectedInfo.setText("Selected extensions:");
        
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        userSelectedTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        userSelectedTable.setLinesVisible(true);
        userSelectedTable.setHeaderVisible(true);
        userSelectedTable.setLayoutData(layoutData);
        userSelectedTable.setSize(200, 600);
        userSelectedTableColumn = new TableColumn(userSelectedTable, SWT.NONE);
        userSelectedTableColumn.setText("Name");

        // Show required dependent extensions
        dependentInfo = new Label(container, SWT.TOP);
        dependentInfo.setText("Required dependent extensions:");

        dependentTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        dependentTable.setLinesVisible(true);
        dependentTable.setHeaderVisible(true);
        dependentTable.setLayoutData(layoutData);
        dependentTable.setSize(200, 600);
        dependentTableColumn = new TableColumn(dependentTable, SWT.NONE);
        dependentTableColumn.setText("Name");

        setControl(container);
        setPageComplete(false);
    }
    
    void onEnterPage() {
        System.out.println("create data for page 2");
        
        userSelectedTable.removeAll(); // avoid entries being added several times if the back-button is used
    	Set<String> userSelectedExtensions = model.getSelectedExtensions();
        for (String userSelectedExtension : userSelectedExtensions) {
        	TableItem tableItem = new TableItem(userSelectedTable, SWT.NONE);
        	tableItem.setText(0, userSelectedExtension); // first column has index 0
        }
        userSelectedTableColumn.pack();
        
        // Find extension dependencies recursively
        model.findDeepDependencyExtensions();

        dependentTable.removeAll();
        for (String dependentExtension : model.getDeepDependencyExtensions()) {
        	TableItem tableItem = new TableItem(dependentTable, SWT.NONE);
        	tableItem.setText(0, dependentExtension); // first column has index 0
        }
        dependentTableColumn.pack();
        
        setPageComplete(true);
    }

    @Override
    public IWizardPage getPreviousPage() {
		return ((ExtensionImportWizard) this.getWizard()).page1;
    }
}
