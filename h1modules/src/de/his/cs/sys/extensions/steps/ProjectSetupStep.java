package de.his.cs.sys.extensions.steps;

import org.eclipse.core.resources.IProject;

import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;

/**
 * Step for the setup of an extension project
 * 
 * @company HIS eG
 * @author keunecke
 */
public interface ProjectSetupStep {
	
	/**
	 * perform the actions of this step on the given project
	 * 
	 * @param project
	 * @param choices
	 */
	public void performStep(IProject project, InitialProjectConfigurationChoices choices);
}
