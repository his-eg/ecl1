package de.his.cs.sys.extensions.standalone;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

import h1modules.resource.setup.step.GitInitSetupStep;
import h1modules.resource.setup.step.ResourceSetupStep;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.ProjectSupport;

/**
 * Wizard for creating a new module project
 */
public class NewExtensionProjectWizardStandalone extends Wizard{

	private NewExtensionWizardPageStandalone firstPage;


	public NewExtensionProjectWizardStandalone() {
		setWindowTitle("HISinOne Extension Project Wizard");
	}

	@Override
	public boolean performFinish() {
		InitialProjectConfigurationChoices initialChoice = firstPage.getInitialConfiguration();
		
		IProject project = new ProjectSupport(firstPage.getStrategy().packagesToCreate(initialChoice.getName())).createProject(initialChoice, firstPage.getLocationURI());
		new GitInitSetupStep().performStep(project, initialChoice);
		new ResourceSetupStep().performStep(project, initialChoice);

		//Exit wizard
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		firstPage = new NewExtensionWizardPageStandalone("New HISinOne Module Project");
		firstPage.setDescription("Wizard for the creation of new HISinOne extension projects");
		addPage(firstPage);
	}

}
