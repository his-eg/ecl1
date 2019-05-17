package net.sf.ecl1.utilities.preferences;

import java.util.Arrays;
import java.util.List;

import net.sf.ecl1.utilities.Activator;

/**
 * Encapsulate access to Eclipse preferences.
 * @author tneumann
 */
public class PreferenceWrapper {
    
    /** Preference for URI to git server */
    public static final String GIT_SERVER_PREFERENCE_KEY = "gitServer";

    /** Preference for URL of jenkins build server */
    public static final String BUILD_SERVER_PREFERENCE_KEY = "buildServer";

    /** Preference for search view (branch) on jenkins build server */
    public static final String BUILD_SERVER_VIEW_PREFERENCE_KEY = "buildServerView";

    /** Preference for the root URLs of the templates (comma-separated list) */
    public static final String TEMPLATE_ROOT_URLS_PREFERENCE_KEY = "templateRootUrls";

    /** Messages with a log level greater or equal than this preference are logged, others not. */
    public static final String LOG_LEVEL_PREFERENCE_KEY = "net.sf.ecl1.logLevel";

    
    /**
     * @return git repository server URL
     */
	public static String getGitServer() {
		return Activator.getPreferences().getString(GIT_SERVER_PREFERENCE_KEY);
	}
	
	/**
	 * @return build server base URL, e.g. "http://build.his.de/build/"
	 */
	public static String getBuildServer() {
        return Activator.getPreferences().getString(BUILD_SERVER_PREFERENCE_KEY);
	}
	
    /**
     * @return the build server view / HisInOne branch declared on the preferences page,
     * like "HEAD" or "HISinOne_VERSION_07_RELEASE_01".
     */
    public static String getBuildServerView() {
        return Activator.getPreferences().getString(BUILD_SERVER_VIEW_PREFERENCE_KEY);
    }
    
    /**
     * @return a list of URLs from where extension template files may be fetched.
     */
    public static List<String> getTemplateRootUrls() {
        String templateRootUrlsStr = Activator.getPreferences().getString(TEMPLATE_ROOT_URLS_PREFERENCE_KEY);
        return Arrays.asList(templateRootUrlsStr.split(","));
    }
    
    /**
     * @return the users log level preference
     */
    public static String getLogLevel() {
    	return Activator.getPreferences().getString(LOG_LEVEL_PREFERENCE_KEY);
    }
}
