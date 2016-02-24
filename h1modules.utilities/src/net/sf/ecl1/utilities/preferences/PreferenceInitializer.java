package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;

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
        store.setDefault(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, "HEAD");
        store.setDefault(ExtensionToolsPreferenceConstants.TEMPLATE_ROOT_URL, "http://ecl1.sourceforge.net/templates");
    }

}
