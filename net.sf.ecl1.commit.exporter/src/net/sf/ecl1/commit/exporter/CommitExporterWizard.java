package net.sf.ecl1.commit.exporter;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class CommitExporterWizard extends Wizard implements IExportWizard {

    private CommitExporterWizardPage page;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Export Commits");
        this.page = new CommitExporterWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
    	page.setValidationRequired();
        page.createHotfix();
        return (page.getErrorMessage() == null);
    }
}
