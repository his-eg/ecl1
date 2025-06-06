package net.sf.ecl1.utilities;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator
 *
 * @author keunecke
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "net.sf.ecl1.utilities";

    private static BundleContext context;
    
    // The shared instance
    private static Activator plugin;

    static BundleContext getContext() {
        return context;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        Activator.context = null;
        plugin = null;
    }

    public static Activator getDefault() {
        return plugin;
    }
    
    public static IPreferenceStore getPreferences() {
        return Activator.getDefault().getPreferenceStore();
    }

    /**
     * Check if application is running inside Eclipse
     * @return true if running in Eclipse
     */
    public static boolean isRunningInEclipse() {
        try {
            // If workspace is accessible, it's running inside Eclipse
            ResourcesPlugin.getWorkspace();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
