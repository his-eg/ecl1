/**
 *
 */
package net.sf.ecl1.hotfix.assistant;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IWorkbench;

/**
 * @author keunecke
 *
 */
public class CreateHotfixSynchronizeWizard extends Wizard implements IConfigurationWizard {

    /* (non-Javadoc)
     * @see org.eclipse.team.ui.IConfigurationWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.core.resources.IProject)
     */
    @Override
    public void init(IWorkbench workbench, IProject project) {
        // nop
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        CreateHotfixParticipant participant = new CreateHotfixParticipant();
        ISynchronizeManager manager = TeamUI.getSynchronizeManager();
        manager.addSynchronizeParticipants(new ISynchronizeParticipant[] { participant });
        ISynchronizeView view = manager.showSynchronizeViewInActivePage();
        view.display(participant);
        return false;
    }

}
