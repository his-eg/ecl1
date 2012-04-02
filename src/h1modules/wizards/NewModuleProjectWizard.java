package h1modules.wizards;

import h1modules.wizards.utils.ProjectSupport;
import h1modules.wizards.utils.ResourceSupport;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
/**
 * Wizard für das Anlegen eines neuen Modulprojekts
 * 
 * @author keunecke
 */
public class NewModuleProjectWizard extends Wizard implements INewWizard {
	
	private WizardNewProjectCreationPage firstPage;

	public NewModuleProjectWizard() {
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
			ResourceSupport.createModuleBeanFile(project);
		} catch (CoreException e) {
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
