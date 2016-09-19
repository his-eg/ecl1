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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
//import org.jdom.Document;
//import org.jdom.Element;
//import org.jdom.JDOMException;
//import org.jdom.input.SAXBuilder;

import de.his.cs.sys.extensions.wizards.utils.JsonUtil;
import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;
import de.his.cs.sys.extensions.wizards.utils.RestUtil;
import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

/**
 * Extension import configuration wizard, page 2 handling dependent extensions.
 * 
 * @author tneumann#his.de
 */
public class ExtensionImportWizardPage2 extends ExtensionImportWizardPage {
	
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
	
    // Data used throughout the Extension Import Wizard
    private ExtensionImportWizardDataStore data;
    
    /**
     * Create second ExtensionImportWizardPage, containing the confirmation dialog.
     * @param data
     */
    protected ExtensionImportWizardPage2(ExtensionImportWizardDataStore data) {
        super(PAGE_NAME);
        this.setDescription(PAGE_DESCRIPTION);
        this.data = data;
    }
    
    @Override
    public void createControl(Composite parent) {
        System.out.println("createControls() for page 2");
        
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
    	Set<String> userSelectedExtensions = data.getSelectedExtensions();
        for (String userSelectedExtension : userSelectedExtensions) {
        	TableItem tableItem = new TableItem(userSelectedTable, SWT.NONE);
        	tableItem.setText(0, userSelectedExtension); // first column has index 0
        }
        userSelectedTableColumn.pack();
        
        // Find extension dependencies recursively
        data.initAllDependencyExtensions();

        dependentTable.removeAll();
        for (String dependentExtension : data.getAllDependencyExtensions()) {
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
    
    @Override
    public IWizardPage getNextPage() {
    	return null;
    }
}
