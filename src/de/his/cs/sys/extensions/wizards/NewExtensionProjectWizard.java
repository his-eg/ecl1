package de.his.cs.sys.extensions.wizards;


import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.wizards.utils.ProjectSupport;
import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * Wizard für das Anlegen eines neuen Modulprojekts
 *
 * @author keunecke
 */
public class NewExtensionProjectWizard extends Wizard implements INewWizard {

	private WizardNewProjectCreationPage firstPage;

	/**
	 * creates a NewModuleProjectWizard
	 */
	public NewExtensionProjectWizard() {
		setWindowTitle("HISinOne Module Project Wizard");
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
		IProject project = ProjectSupport.createProject(projectName, location);
		try {
			new ResourceSupport(project).createFiles();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		firstPage = new WizardNewProjectCreationPage("New HISinOne Module Project");
		firstPage.setDescription("Unterstützung bei der Erstellung eines neuen HISinOne Modul Projekts");
		addPage(firstPage);
	}

}
