package net.sf.ecl1.changeset.exporter;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Sets;

/**
 * The activator class controls the plug-in life cycle
 */
public class ChangeSetExportWizardPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "net.sf.ecl1.changeset.exporter"; //$NON-NLS-1$

    // The shared instance
    private static ChangeSetExportWizardPlugin plugin;

    /**
     * The constructor
     */
    public ChangeSetExportWizardPlugin() {
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
    public static ChangeSetExportWizardPlugin getDefault() {
        return plugin;
    }

    public Collection<ChangeSet> getChangeSets() {
        Collection<ChangeSet> changeSets = Sets.newHashSet();
        ActiveChangeSetManager manager = CVSUIPlugin.getPlugin().getChangeSetManager();
        changeSets.add(manager.getDefaultSet());
        changeSets.addAll(Arrays.asList(manager.getSets()));
        return changeSets;
    }

}
