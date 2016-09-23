package h1modules.utilities.utils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;


/**
 * Activator
 *
 * @author keunecke
 */
public class Activator extends AbstractUIPlugin {

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
     * @return pluginId required for example by Status constructors.
     */
    public static String getPluginId() {
    	return Activator.getDefault().getBundle().getSymbolicName();
    }
    
    /**
     * @return long version identifier of the HisInOne branch declared on the preferences page,
     * like "HEAD" or "HISinOne_VERSION_07_RELEASE_01".
     */
    public static String getHISinOneBranch() {
        return getPreferences().getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE);
    }
}
