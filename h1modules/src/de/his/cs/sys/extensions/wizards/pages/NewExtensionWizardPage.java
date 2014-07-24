/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 21.06.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.pages;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.wizards.pages.packages.HISinOneStrategy;
import de.his.cs.sys.extensions.wizards.pages.packages.PackageStructureStrategy;
import de.his.cs.sys.extensions.wizards.utils.HISConstants;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import de.his.cs.sys.extensions.wizards.utils.WorkspaceSupport;

/**
 * Extended New Project Wizard Page asking for additional information on extensions
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class NewExtensionWizardPage extends WizardNewProjectCreationPage {

	/**
	 * @author keunecke
	 * @version $Revision$ 
	 */
	private final class StrategySetter implements SelectionListener {
		
		private final PackageStructureStrategy strategyToSet;
		
		/**
		 * @param strategyToSet
		 */
		public StrategySetter(PackageStructureStrategy strategyToSet) {
			this.strategyToSet = strategyToSet;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			strategy = strategyToSet; 
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// 
		}
	}

	private List projectList;
	private Text versionInputTextField;
	private PackageStructureStrategy strategy = new HISinOneStrategy();
	
	/**
	 * @param pageName
	 */
	public NewExtensionWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		
		Composite versionChoice = new Composite(control, SWT.LEFT);
		GridLayout versionGrid = new GridLayout(2, false);
		versionChoice.setLayout(versionGrid);
		Label versionInputLabel = new Label(versionChoice, SWT.LEFT);
		versionInputLabel.setText("Initial Extension Version");
		versionInputTextField = new Text(versionChoice, SWT.LEFT);
		versionInputTextField.setText("0.0.1");
		
		Composite projectChoice = new Composite(control, SWT.BORDER | SWT.TOP);
		GridLayout gl = new GridLayout(2, false);
		projectChoice.setLayout(gl);
		Label projectChoiceLabel = new Label(projectChoice, SWT.TOP);
		projectChoiceLabel.setText("Referenced Projects");
		projectList = new List(projectChoice, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		java.util.List<String> references = new WorkspaceSupport().getPossibleProjectsToReference();
		int index = 0;
		for (String ref : references) {
			projectList.add(ref);
			if(HISConstants.WEBAPPS.equals(ref)) {
				index = references.indexOf(ref);
			}
		}
		projectList.select(index);
		Group buttonGroup = new Group(control, SWT.BORDER);
		buttonGroup.setLayout(new RowLayout());
		buttonGroup.setText("Package Layout");
		Button defaultButton = new Button(buttonGroup, SWT.RADIO);
		defaultButton.addSelectionListener(new StrategySetter(new HISinOneStrategy()));
		defaultButton.setText("HISinOne Layout");
		defaultButton.setToolTipText("HISinOne Package Layout with packages for persistence etc.");
		defaultButton.setSelection(true);
	}
	
	/**
	 * Get the initial version of the new extension
	 * 
	 * @return initial version string
	 */
	private String getInitialVersion(){
		return versionInputTextField.getText();
	}
	
	/**
	 * @return collection of project names to reference by the new extension project
	 */
	private Collection<String> getProjectsToReference() {
		Collection<String> result = new ArrayList<String>();
		String[] selection = projectList.getSelection();
		for (String project : selection) {
			result.add(project);
		}
		return result;
	}
	
	/**
	 * @return the package structure strategy
	 */
	public PackageStructureStrategy getStrategy() {
		return this.strategy;
	}
	
	/**
	 * @return get the initial choices done by the user
	 */
	public InitialProjectConfigurationChoices getInitialConfiguration() {
		return new InitialProjectConfigurationChoices(getProjectsToReference(), getProjectName(), getInitialVersion());
	}

}
