package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.PropertyUtil;
import net.sf.ecl1.utilities.hisinone.CvsTagUtil;

import java.text.ParseException;
import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

	public static boolean IS_LEGACY_GIT_URL_STYLE;

	public static final String GIT_BASE_REPOSITORY_PATH = "ssh://git@git.his.de/";
	public static final String GITLAB_BASE_REPOSITORY_PATH = "ssh://git@gitlab.his.de/";

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE, "http://build.his.de/build/");
        
        // initialize HISinOne version preference setting from webapps/CVS/Tag
        String branch = CvsTagUtil.getCvsTagVersionLongString();
        if (branch==null || branch.equals(CvsTagUtil.UNKNOWN_VERSION)) {
        	// there is no webapps project yet, use HEAD as default
        	branch = CvsTagUtil.HEAD_VERSION;
        }
        store.setDefault(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, branch);
        store.setDefault(ExtensionToolsPreferenceConstants.TEMPLATE_ROOT_URLS, "http://devtools.his.de/ecl1/templates,http://ecl1.sourceforge.net/templates");
		store.setDefault(ExtensionToolsPreferenceConstants.LOG_LEVEL_PREFERENCE, "INFO");
		
		// read ecl1 configuration: note that the build server URL may not be the default preference!
        String buildServer = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE);
		String configFile = buildServer + "userContent/ecl1.properties";
        String configStr = new RemoteProjectSearchSupport().getRemoteFileContent(configFile);
        try {
        	Map<String, String> configProps = PropertyUtil.stringToProperties(configStr);
            String urlStyle = configProps!=null ? configProps.get("urlStyle") : null;
            IS_LEGACY_GIT_URL_STYLE = urlStyle!=null && urlStyle.equals("legacy");
        } catch (ParseException e) {
        	logger.error2("Error parsing extension import configuration: " + e.getMessage(), e);
		}

        // set default git server url in dependency of the configuration
        String defaultGitUrl = IS_LEGACY_GIT_URL_STYLE ? GIT_BASE_REPOSITORY_PATH : GITLAB_BASE_REPOSITORY_PATH;
        store.setDefault(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE, defaultGitUrl);
    }
}
