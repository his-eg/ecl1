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
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.wizards.utils.HISConstants;
import de.his.cs.sys.extensions.wizards.utils.WorkspaceSupport;

/**
 * Extended New Project Wizard Page asking for additional information on extensions
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class NewExtensionWizardPage extends WizardNewProjectCreationPage {

	private List projectList;

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
		Composite projectChoice = new Composite(control, NONE);
		GridLayout gl = new GridLayout(2, false);
		projectChoice.setLayout(gl);
		Label projectChoiceLabel = new Label(projectChoice, NONE);
		projectChoiceLabel.setText("Referenced Projects");
		projectList = new List(projectChoice, SWT.MULTI);
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
	
	public Collection<String> getProjectsToReference() {
		Collection<String> result = new ArrayList<String>();
		String[] selection = projectList.getSelection();
		for (String project : selection) {
			result.add(project);
		}
		return result;
	}

}
