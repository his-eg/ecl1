package net.sf.ecl1.commit.exporter;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class CommitExporterWizard extends Wizard implements IExportWizard {

    private CommitExporterWizardPage page;
    
    public CommitExporterWizard() {
        super();
        setWindowTitle("Export Commits");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {  	    	
        //do nothing yet
    }

    @Override
    public boolean performFinish() {
        page.setValidationRequired();
        page.createHotfix();
        return (page.getErrorMessage() == null);
    }

    @Override
    public void addPages(){
        super.addPages();
        this.page = new CommitExporterWizardPage();
        addPage(page);
    }
}
