package h1modules.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
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
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		super.addPages();
		firstPage = new WizardNewProjectCreationPage("HISinOne Module Project Wizard");
		firstPage.setDescription("Unterstützung bei der Erstellung eines neuen HISinOne Modul Projekts");
		addPage(firstPage);
	}
	
	

}
