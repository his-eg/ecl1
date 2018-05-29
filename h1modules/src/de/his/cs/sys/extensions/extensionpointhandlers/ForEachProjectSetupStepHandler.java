/*
 * Copyright (c) 2012 HIS eG All Rights Reserved.
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

import de.his.cs.sys.extensions.Activator;
import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ForEachProjectSetupStepHandler {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

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

	/**
	 * Execute each project setup step
	 */
	public void contribute() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(CS_SYS_EXTENSIONS_PROJECT_SETUP_STEPS);
		for (IConfigurationElement element : elements) {
			try {
				Object object = element.createExecutableExtension("class");
				evaluate(object);
			} catch (CoreException e) {
	    		logger.error(e.getMessage(), e);
			}
		}
	}
	
	private void evaluate(Object object) {
		ProjectSetupStep step = (ProjectSetupStep) object;
		step.performStep(this.project, this.initialChoice);
	}
	
}
