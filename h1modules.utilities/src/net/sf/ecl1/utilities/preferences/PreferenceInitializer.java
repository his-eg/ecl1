package net.sf.ecl1.utilities.preferences;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.GitUtil;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public static final String GITLAB_BASE_REPOSITORY_PATH = "ssh://git@gitlab.his.de/";
	
	/*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getPreferences();
        store.setDefault(PreferenceWrapper.BUILD_SERVER_PREFERENCE_KEY, "http://build.his.de/build/");
        store.setDefault(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY, GitUtil.getCheckedOutBranchOfWebapps());
        store.setDefault(PreferenceWrapper.TEMPLATE_ROOT_URLS_PREFERENCE_KEY, "http://devtools.his.de/ecl1/templates,http://ecl1.sourceforge.net/templates");
		store.setDefault(PreferenceWrapper.LOG_LEVEL_PREFERENCE_KEY, "INFO");
        store.setDefault(PreferenceWrapper.GIT_SERVER_PREFERENCE_KEY, GITLAB_BASE_REPOSITORY_PATH);
        store.setDefault(PreferenceWrapper.DETECT_BRANCH_AUTOMATICALLY, true);
    }
}
