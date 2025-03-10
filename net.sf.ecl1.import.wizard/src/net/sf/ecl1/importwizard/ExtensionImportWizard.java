package net.sf.ecl1.importwizard;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Wizard for import of extension projects listed on a jenkins.
 *
 * @author keunecke
 */
public class ExtensionImportWizard extends Wizard implements IImportWizard {

	private static final String WINDOW_TITLE = "Extension Import Wizard";

    /** Data used throughout the extension import wizard. */
    ExtensionImportWizardModel model;
    
    ExtensionImportWizardPage1_Selection page1;
    ExtensionImportWizardPage2_Confirmation page2;
    
    public ExtensionImportWizard() {
        super();
        model = new ExtensionImportWizardModel();
        setWindowTitle(WINDOW_TITLE);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setNeedsProgressMonitor(true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();
        page1 = new ExtensionImportWizardPage1_Selection(model);
        addPage(page1);
        page2 = new ExtensionImportWizardPage2_Confirmation(model);
        addPage(page2);
    }

    /**
     * Enable the "finish"-button only if page 2 is the current wizard page.
     * @return true if the "finish"-button shall be enabled
     */
    // Implementation note: This is a better solution than using the super implementation and
    // calling setPageComplete() to control the "finish"-button.
    @Override
    public boolean canFinish() {
    	IWizardPage currentPage = this.getContainer().getCurrentPage();
    	boolean canFinish = (currentPage instanceof ExtensionImportWizardPage2_Confirmation);
    	return canFinish;
    }
    
    /**
     * Finish the extension import.
     * This is implemented such that progress monitoring is available.
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
    	// variables to be used in the Job class implementation must be final
        Collection<String> extensionsToImport = new HashSet<>(model.getSelectedExtensions()); // copy
        extensionsToImport.addAll(model.getDependenciesOfSelectedExtensions());

        if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
            ExtensionImportJob importJob = new ExtensionImportJob(extensionsToImport, false, false); 
            IProgressMonitor dummyMonitor = new NullProgressMonitor();
            importJob.run(dummyMonitor);
            // Exit wizard
            return true;
        }

        boolean openProjectsAfterImport = page2.openProjectsAfterImport();
        boolean deleteFolders = page2.deleteFolders();
        ExtensionImportJob importJob = new ExtensionImportJob(extensionsToImport, openProjectsAfterImport, deleteFolders); 

        Activator.getDefault().setJob(importJob);
        //Acquiring this rule prevents auto builds
        importJob.setRule(WorkspaceFactory.getWorkspace().getRuleFactory().buildRule());
        importJob.schedule();

        return true;
    }
    
}
