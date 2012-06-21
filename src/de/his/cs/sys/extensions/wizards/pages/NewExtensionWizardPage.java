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

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.wizards.utils.WorkspaceSupport;

/**
 * Extended New Project Wizard Page asking for additional information on extensions
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class NewExtensionWizardPage extends WizardNewProjectCreationPage {

	private List projectCombo;

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
		projectCombo = new List(projectChoice, NONE);
		Collection<String> references = new WorkspaceSupport().getPossibleProjectsToReference();
		for (String ref : references) {
			projectCombo.add(ref);
		}
	}

}
