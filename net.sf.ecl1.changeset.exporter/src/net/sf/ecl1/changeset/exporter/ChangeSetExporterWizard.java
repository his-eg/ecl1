package net.sf.ecl1.changeset.exporter;

import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
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
        Collection<ChangeSet> changes = ChangeSetExportWizardPlugin.getDefault().getChangeSets();
        for (ChangeSet changeSet : changes) {
            if (changeSet != null) {
                System.out.println("Change Set: " + changeSet);
                System.out.println("Change Set Name: " + changeSet.getName());
                System.out.println("Change Set Comment: " + changeSet.getComment());
                System.out.println("Change Set Resources Begin");
                IResource[] resources = changeSet.getResources();
                for (IResource resource : resources) {
                    if (resource != null) {
                        System.out.println("Resource Name: " + resource.getName());
                        System.out.println("Resource Type: " + resource.getType());
                    }
                }
            }
        }
        return true;
    }

}
