package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;
import h1modules.utilities.utils.CvsTagUtil;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE, "ssh://git@git.his.de/");
        store.setDefault(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE, "http://build.his.de/build/");
        // initialize HISinOne version preference setting from webapps/CVS/Tag
        String branch = CvsTagUtil.getCvsTagVersionLongString();
        if (branch==null || branch.equals(CvsTagUtil.UNKNOWN_VERSION)) {
        	// there is no webapps project yet, use HEAD as default
        	branch = CvsTagUtil.HEAD_VERSION;
        }
        store.setDefault(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, branch);
        store.setDefault(ExtensionToolsPreferenceConstants.TEMPLATE_ROOT_URLS, "http://devtools.his.de/ecl1/templates,http://ecl1.sourceforge.net/templates");
		store.setDefault(ExtensionToolsPreferenceConstants.LOGGING_PREFERENCE, false);
    }
}
