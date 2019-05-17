package de.his.cs.sys.extensions.extensionpointhandlers;

import static de.his.cs.sys.extensions.steps.DeclaredExtensionPointIds.CS_SYS_EXTENSIONS_PROJECT_SETUP_STEPS;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import de.his.cs.sys.extensions.Activator;
import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;

/**
 * @company HIS eG
 * @author keunecke
 */
public class ForEachProjectSetupStepHandler {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ForEachProjectSetupStepHandler.class.getSimpleName());

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
	    		logger.error2(e.getMessage(), e);
			}
		}
	}
	
	private void evaluate(Object object) {
		ProjectSetupStep step = (ProjectSetupStep) object;
		step.performStep(this.project, this.initialChoice);
	}
}
