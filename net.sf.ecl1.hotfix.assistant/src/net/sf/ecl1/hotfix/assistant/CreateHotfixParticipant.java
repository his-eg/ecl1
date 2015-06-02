package net.sf.ecl1.hotfix.assistant;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Participant in Synchronization providing infrastructure for creation of hotfix snippets
 *
 * @author keunecke
 */
public class CreateHotfixParticipant extends AbstractSynchronizeParticipant {

    public static final String CONTEXT_MENU_CONTRIB_GROUP = "hotifx_context_group_1";

    /**
     *
     * @author keunecke
     */
    private class CreateHotfixActionContribution extends SynchronizePageActionGroup {
        @Override
        public void initialize(ISynchronizePageConfiguration configuration) {
            super.initialize(configuration);
            appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, CONTEXT_MENU_CONTRIB_GROUP, new SynchronizeModelAction("Create Hotfix Snippet", configuration) {
                @Override
                protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
                    return new CreateHotfixOperation(configuration, elements);
                }
            });
        }

    }

    @Override
    public IPageBookViewPage createPage(ISynchronizePageConfiguration configuration) {
        System.out.println("createPage");
        return null;
    }

    @Override
    public void run(IWorkbenchPart part) {
        System.out.println("run");
    }

    @Override
    public void dispose() {
        System.out.println("dispose");
    }

    @Override
    protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
        configuration.addMenuGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, CONTEXT_MENU_CONTRIB_GROUP);
        configuration.addActionContribution(new CreateHotfixActionContribution());
    }

}
