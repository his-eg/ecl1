package de.his.cs.sys.extensions.wizards;


import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.his.cs.sys.extensions.extensionpointhandlers.ForEachProjectSetupStepHandler;
import de.his.cs.sys.extensions.wizards.pages.NewExtensionWizardPage;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import de.his.cs.sys.extensions.wizards.utils.ProjectSupport;

/**
 * Wizard für das Anlegen eines neuen Modulprojekts
 *
 * @author keunecke
 */
public class NewExtensionProjectWizard extends Wizard implements INewWizard {

	private NewExtensionWizardPage firstPage;

	/**
	 * creates a NewModuleProjectWizard
	 */
	public NewExtensionProjectWizard() {
		setWindowTitle("HISinOne Extension Project Wizard");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Do nothing yet
	}

	@Override
	public boolean performFinish() {
		URI location = null;
		if(!firstPage.useDefaults()) {
			location = firstPage.getLocationURI();
		}
		InitialProjectConfigurationChoices initialChoice = firstPage.getInitialConfiguration();
		IProject project = new ProjectSupport(firstPage.getStrategy().packagesToCreate(initialChoice.getName())).createProject(initialChoice, location);
		new ForEachProjectSetupStepHandler(project, initialChoice).contribute();
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		firstPage = new NewExtensionWizardPage("New HISinOne Module Project");
		firstPage.setDescription("Unterstützung bei der Erstellung eines neuen HISinOne Extension Projekts");
		addPage(firstPage);
	}

}
