package net.sf.ecl1.commit.exporter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Sets;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommitExportWizardPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "net.sf.ecl1.commit.exporter"; //$NON-NLS-1$

    // The shared instance
    private static CommitExportWizardPlugin plugin;

    /**
     * The constructor
     */
    public CommitExportWizardPlugin() {
        // nop
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static CommitExportWizardPlugin getDefault() {
        return plugin;
    }

    
    
    /**
     * Retrieve all present change sets
     *
     * @return all user defined change sets
     */
    public Collection<ChangeSet> getChangeSets() {
        Collection<ChangeSet> changeSets = Sets.newHashSet();
        ActiveChangeSetManager manager = CVSUIPlugin.getPlugin().getChangeSetManager();
        List<ChangeSet> changeSetsFromManager = Arrays.asList(manager.getSets());
        changeSets.addAll(changeSetsFromManager);
        return changeSets;
    }

}
