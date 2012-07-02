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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

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

	private List projectList;
	private Text versionInputTextField;
	
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
		Composite versionChoice = new Composite(control, SWT.BORDER|SWT.TOP);
		Label versionInputLabel = new Label(versionChoice, SWT.LEFT);
		versionInputLabel.setText("Initial Extension Version");
		versionInputTextField = new Text(control, SWT.LEFT);
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
	 * @return get the initial choices done by the user
	 */
	public InitialProjectConfigurationChoices getInitialConfiguration() {
		return new InitialProjectConfigurationChoices(getProjectsToReference(), getProjectName(), getInitialVersion());
	}

}
