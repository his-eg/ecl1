package net.sf.ecl1.changeset.exporter;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ChangeSetExporterWizard extends Wizard implements IExportWizard {

    private ChangeSetExporterWizardPage page;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Export Change Sets");
        this.page = new ChangeSetExporterWizardPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        HotfixInformation hf = page.getHotfixDefinition();
        IFile releaseFile = page.getSelectedReleaseFile();
        //TODO replace contents and return true if successful
        return true;
    }

}
