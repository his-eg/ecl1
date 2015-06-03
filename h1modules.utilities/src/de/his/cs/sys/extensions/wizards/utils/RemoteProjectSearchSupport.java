package de.his.cs.sys.extensions.wizards.utils;

import h1modules.utilities.utils.Activator;

import java.util.Collection;
import java.util.TreeSet;

import net.sf.ecl1.utilities.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Read remote projects from a jenkins view
 * Constants default to HIS context
 *
 * @author keunecke
 */
public class RemoteProjectSearchSupport {

    private class BuildJob {

        private String name;

        private String url;

        private String color;

        public String getName() {
            return name;
        }

    }

    private class BuildJobView {
        private Collection<BuildJob> jobs;

        public Collection<? extends String> getBuildJobNames() {
            Collection<String> result = new TreeSet<>();
            for (BuildJob buildJob : jobs) {
                result.add(buildJob.getName());
            }
            return result;
        }
    }

    private static final String HIS_DEFAULT_JENKINS_VIEW_INFIX = "view/";

    /**
     * Jenkins default addition for REST api calls
     */
    private static final String JENKINS_API_ADDITION = "/api/json";

    public Collection<String> getProjects() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String buildServer = store.getString(PreferenceConstants.BUILD_SERVER_PREFERENCE);
        String buildServerView = store.getString(PreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE);
        String lookUpTarget = buildServer + HIS_DEFAULT_JENKINS_VIEW_INFIX + buildServerView + JENKINS_API_ADDITION;
        TreeSet<String> result = new TreeSet<String>();
        BuildJobView view = JsonUtil.fromJson(BuildJobView.class, RestUtil.getJsonStream(lookUpTarget));
        result.addAll(view.getBuildJobNames());
        return result;
    }

}
