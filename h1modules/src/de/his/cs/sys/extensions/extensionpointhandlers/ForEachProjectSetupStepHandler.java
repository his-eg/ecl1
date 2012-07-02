/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 26.06.2012 by keunecke
 */
package de.his.cs.sys.extensions.extensionpointhandlers;

import static de.his.cs.sys.extensions.steps.DeclaredExtensionPointIds.CS_SYS_EXTENSIONS_PROJECT_SETUP_STEPS;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ForEachProjectSetupStepHandler {
	
	private final IProject project;
	private final InitialProjectConfigurationChoices initialChoice;
	
	/**
	 * @param project
	 * @param initialChoice 
	 */
	public ForEachProjectSetupStepHandler(IProject project, InitialProjectConfigurationChoices initialChoice) {
		this.project = project;
		this.initialChoice = initialChoice;
	}

	public void contribute() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(CS_SYS_EXTENSIONS_PROJECT_SETUP_STEPS);
		for (IConfigurationElement element : elements) {
			try {
				Object object = element.createExecutableExtension("class");
				evaluate(object);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void evaluate(Object object) {
		ProjectSetupStep step = (ProjectSetupStep) object;
		step.performStep(project);
	}
	
}
