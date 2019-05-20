package de.his.cs.sys.extensions.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import de.his.cs.sys.extensions.wizards.pages.NewEnrollCheckerWizardPage;

/**
 * creates a new EnrollChecker
 *
 * @author brummermann
 */
public class NewEnrollCheckerWizard extends Wizard implements INewWizard {
	
	private IStructuredSelection selection;
	private NewEnrollCheckerWizardPage newFileWizardPage;

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
		this.selection = selection;
	}
}
