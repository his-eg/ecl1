package de.his.cs.sys.extensions.wizards;


import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.extensionpointhandlers.ForEachProjectSetupStepHandler;
import de.his.cs.sys.extensions.wizards.pages.NewExtensionWizardPage;
import de.his.cs.sys.extensions.wizards.utils.ProjectSupport;
import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

import static de.his.cs.sys.extensions.steps.DeclaredExtensionPointIds.*;

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
		String projectName = firstPage.getProjectName();
		URI location = null;
		if(!firstPage.useDefaults()) {
			location = firstPage.getLocationURI();
		}
		IProject project = new ProjectSupport().createProject(projectName, location);
		new ProjectSupport().addProjectDependencies(project, firstPage.getProjectsToReference());
		new ForEachProjectSetupStepHandler(project).contribute();
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
