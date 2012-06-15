package de.his.cs.sys.extensions.wizards;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * creates a new EnrollChecker
 *
 * @author brummermann
 */
public class NewEnrollCheckerWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	private NewEnrollCheckerWizardPage	 newFileWizardPage;
	private IWorkbench workbench;

	/**
	 * creates a wizard for new EnrollChecks
	 */
	public NewEnrollCheckerWizard() {
		setWindowTitle("New EnrollCheck");
	}

	@Override
	public void addPages() {
	    newFileWizardPage = new NewEnrollCheckerWizardPage(selection);
	    addPage(newFileWizardPage);
	}

	@Override
	public boolean performFinish() {

	    IFile file = newFileWizardPage.createNewFile();
	    if (file != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	    this.workbench = workbench;
	    this.selection = selection;
	}

	static class NewEnrollCheckerWizardPage extends WizardNewFileCreationPage {

	    public NewEnrollCheckerWizardPage (IStructuredSelection selection) {
	        super("NewConfigFileWizardPage", selection);
	        setTitle("New EnrollChecker");
	        setDescription("Creates a new EnrollChecker ");
	        setFileExtension("java");
	    }

	    @Override
	    protected InputStream getInitialContents() {
	    	// TODO add entry to extension.beans.spring.xml
	    	// TODO replace variables
	    	return ResourceSupport.class.getResourceAsStream("templates/src/java/EnrollChecker.java.template");
	    }
	}
}