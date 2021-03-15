package net.sf.ecl1.utilities.preferences;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.general.PropertyUtil;
import net.sf.ecl1.utilities.general.RemoteProjectSearchSupport;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.Git;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, PreferenceInitializer.class.getSimpleName());

	public static boolean IS_LEGACY_GIT_URL_STYLE;

	public static final String GIT_BASE_REPOSITORY_PATH = "ssh://git@git.his.de/";
	public static final String GITLAB_BASE_REPOSITORY_PATH = "ssh://git@gitlab.his.de/";
	
	public static final String UNKNOWN_BRANCH = "Unknown_branch";

    
	private String getCheckedOutBranchOfWebapps() {
        IProject webappsProject = WebappsUtil.findWebappsProject();
        if(webappsProject != null) {
        	String webappsPath = webappsProject.getLocation().toString();
            Git git = GitUtil.searchGitRepo(webappsPath);
            try {
				String branch = git.getRepository().getFullBranch();
				//Remove "refs/heads/" from branch name
				branch = branch.substring(branch.lastIndexOf("/")+1);
				return branch;
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return UNKNOWN_BRANCH;
	}
	
	/*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getPreferences();
        store.setDefault(PreferenceWrapper.BUILD_SERVER_PREFERENCE_KEY, "http://build.his.de/build/");
        
        
        
        
//        // initialize HISinOne version preference setting from webapps/CVS/Tag
//        String branch = CvsTagUtil.getCvsTagVersionLongString();
//        if (branch==null || branch.equals(CvsTagUtil.UNKNOWN_VERSION)) {
//        	// there is no webapps project yet, use HEAD as default
//        	branch = CvsTagUtil.HEAD_VERSION;
//        }
        store.setDefault(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY, getCheckedOutBranchOfWebapps());
        store.setDefault(PreferenceWrapper.TEMPLATE_ROOT_URLS_PREFERENCE_KEY, "http://devtools.his.de/ecl1/templates,http://ecl1.sourceforge.net/templates");
		store.setDefault(PreferenceWrapper.LOG_LEVEL_PREFERENCE_KEY, "INFO");
		
		// Read ecl1 configuration: Note that the build server URL may not be the default preference!
		// Furthermore we may get errors like UnknownHostException when the build server is not available
		// (no VPN, universities have no access etc.) In that case we will use the new gitlab URL style.
		// This should be ok for all after the migration to gitlab went through.
        String buildServer = store.getString(PreferenceWrapper.BUILD_SERVER_PREFERENCE_KEY);
		String configFile = buildServer + "userContent/ecl1.properties";
		String configStr = null;
		try {
			configStr = new RemoteProjectSearchSupport().getRemoteFileContent(configFile, true);
		} catch (Exception e) {
        	logger.error2("Unable to read ecl1 configuration. Do you have access to " + buildServer + " ? The original message was " + e.getMessage(), e);
		}
        try {
        	Map<String, String> configProps = PropertyUtil.stringToProperties(configStr);
            String urlStyle = configProps!=null ? configProps.get("urlStyle") : null;
            IS_LEGACY_GIT_URL_STYLE = urlStyle!=null && urlStyle.equals("legacy");
        } catch (ParseException e) {
        	logger.error2("Error parsing extension import configuration: " + e.getMessage(), e);
        	IS_LEGACY_GIT_URL_STYLE = false;
		}

        // set default git server url in dependency of the configuration
        String defaultGitUrl = IS_LEGACY_GIT_URL_STYLE ? GIT_BASE_REPOSITORY_PATH : GITLAB_BASE_REPOSITORY_PATH;
        store.setDefault(PreferenceWrapper.GIT_SERVER_PREFERENCE_KEY, defaultGitUrl);
    }
}
