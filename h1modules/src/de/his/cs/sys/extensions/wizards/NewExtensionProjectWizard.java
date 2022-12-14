package de.his.cs.sys.extensions.wizards;


import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.his.cs.sys.extensions.Activator;
import de.his.cs.sys.extensions.extensionpointhandlers.ForEachProjectSetupStepHandler;
import de.his.cs.sys.extensions.wizards.pages.NewExtensionWizardPage;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.ProjectSupport;

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
		final URI location;
		if(!firstPage.useDefaults()) {
			location = firstPage.getLocationURI();
		} else {
			location = null; 
		}	
		InitialProjectConfigurationChoices initialChoice = firstPage.getInitialConfiguration();
		
		/*
		 * We are wrapping the actual creation of the extension project into a workspace job, because
		 * workspace jobs are never interrupted by the eclipse build process (since they are considered somewhat atomic). 
		 * 
		 * This prevents the wizard dialog from freezing in case that a build job has begun between starting 
		 * the wizard and performing the finish. 
		 */
		Job createExtensionProjectJob = new WorkspaceJob("ecl1: Creating a new extension project") {
			
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IProject project = new ProjectSupport(firstPage.getStrategy().packagesToCreate(initialChoice.getName())).createProject(initialChoice, location);
				new ForEachProjectSetupStepHandler(project, initialChoice).contribute();


				
				//Exit job
				return Status.OK_STATUS;
			}
		};
		/*Register job with activator to shutdown the job properly in case eclipse is closed */
		Activator.getDefault().setJob(createExtensionProjectJob);
		createExtensionProjectJob.schedule();
		
		//Exit wizard
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		firstPage = new NewExtensionWizardPage("New HISinOne Module Project");
		firstPage.setDescription("Wizard for the creation of new HISinOne extension projects");
		addPage(firstPage);
	}

}
