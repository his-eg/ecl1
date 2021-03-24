package net.sf.ecl1.utilities.preferences;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

import java.io.IOException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.Git;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @SuppressWarnings("unused")
	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, PreferenceInitializer.class.getSimpleName());

	public static final String GITLAB_BASE_REPOSITORY_PATH = "ssh://git@gitlab.his.de/";
	
	public static final String UNKNOWN_BRANCH = "Unknown_branch";
	public static final String MASTER = "master";
	/** The local master branch is called HEAD on the remote jenkins server */
	public static final String REMOTE_ALIAS_FOR_LOCAL_MASTER_BRANCH = "HEAD";

    
	private String getCheckedOutBranchOfWebapps() {
        IProject webappsProject = WebappsUtil.findWebappsProject();
        if(webappsProject != null) {
        	String webappsPath = webappsProject.getLocation().toString();
            Git git = GitUtil.searchGitRepo(webappsPath);
            try {
				String branch = git.getRepository().getFullBranch();
				//Remove "refs/heads/" from branch name
				branch = branch.substring(branch.lastIndexOf("/")+1);
				if(branch.equals(MASTER)) {
					branch = REMOTE_ALIAS_FOR_LOCAL_MASTER_BRANCH;
				}
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
        store.setDefault(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY, getCheckedOutBranchOfWebapps());
        store.setDefault(PreferenceWrapper.TEMPLATE_ROOT_URLS_PREFERENCE_KEY, "http://devtools.his.de/ecl1/templates,http://ecl1.sourceforge.net/templates");
		store.setDefault(PreferenceWrapper.LOG_LEVEL_PREFERENCE_KEY, "INFO");
        store.setDefault(PreferenceWrapper.GIT_SERVER_PREFERENCE_KEY, GITLAB_BASE_REPOSITORY_PATH);
    }
}
