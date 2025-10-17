package net.sf.ecl1.importwizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.sf.ecl1.utilities.general.RemoteProjectSearchSupport;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * The data model of the Extension Import Wizard.
 * @author tneumann
 */
public class ExtensionImportWizardModel {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ExtensionImportWizardModel.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

	public static final String JENKINS_WEBAPPS_NAME = "/webapps";

	/**
	 * Extensions.json-file from build.his.de that stores meta info about the extensions.
	 *
	 * E.g.: Upstream and downstream extensions of a given extension.
	 */
	JsonObject extensionsJSON = null;

	class Extension implements Comparable<Extension> {

    	boolean checked = false;
    	String name;

    	Extension(String name) {
    		this.name = name;
    	}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}

		public String getName() {
			return name;
		}

		@Override
		public int compareTo(Extension o) {
			return name.compareTo(o.getName());
		}

    }

    private RemoteProjectSearchSupport remoteProjectSearchSupport;

    // actual branch
    private String branch;

    /**
     * All extensions existing on repo server.
     *
     * Extensions are defined as having two dots in their name.
     * All other projects on the repo server are filtered out.
     */
    private Set<String> remoteExtensions;

    /**
     * All extensions existing in workspace
     */
    private Set<String> extensionsInWorkspace;

    /**
     * Selectable extensions are extensions that are present on the
     * remote server and are _not_ in the local workspace.
     */
    private Set<Extension> selectableExtensions;

    /**
     * Key --> Name of the extension.
     * Value --> classpath file from the extension, which was obtained from build.his.de
     */
    private Map<String, ClasspathFile> classpathFiles;

    // dependent extensions
    private Collection<String> allRequiredDependencies = null;

    public ExtensionImportWizardModel() {
    	branch = PreferenceWrapper.getBuildServerView();
    	remoteProjectSearchSupport = new RemoteProjectSearchSupport();
    	initRemoteExtensions();
        initExtensionsInWorkspace();
        initSelectableExtensions();
        downloadExtensionsJSON();
        classpathFiles = new HashMap<String, ClasspathFile>();
    }

    private void downloadExtensionsJSON() {
		String extensionsJSONAsString = remoteProjectSearchSupport.getRemoteArtifactContent("FULL_BUILD", "extensions.json", true);
		if (extensionsJSONAsString != null ) {
			extensionsJSON = JsonParser.parseString(extensionsJSONAsString).getAsJsonObject();
		}
	}

	String getBranch() {
    	return branch;
    }



    private void initSelectableExtensions() {
    	selectableExtensions = new TreeSet<>();
    	for(String remoteExtension : remoteExtensions) {
    		if (!extensionsInWorkspace.contains(remoteExtension)) {
    			selectableExtensions.add(new Extension(remoteExtension));
    		}
    	}
    }

    /**
     * Returns all extensions that can be selected by the user.
     */
    Set<Extension> getSelectableExtensions() {
    	return selectableExtensions;
    }


    /**
     * Read remote extensions from configured build server.
     */
    private void initRemoteExtensions() {
    	remoteExtensions = new TreeSet<String>();
        Collection<String> remoteProjectsIncludingBranch = remoteProjectSearchSupport.getProjects();
        String branch = getBranch();
        for (String remoteProjectIncludingBranch : remoteProjectsIncludingBranch) {
            String remoteProject = remoteProjectIncludingBranch.replace("_" + branch, "");
        	Pattern p = Pattern.compile(".+\\..+\\..+");
        	Matcher m = p.matcher(remoteProject);
            if (remoteProject != null && !remoteProject.trim().isEmpty() && m.matches()) {
            	//logger.debug("Found remote project '" + remoteProjectIncludingBranch + "' , store it as '" + remoteProject + "'");
            	remoteExtensions.add(remoteProject);
            }
        }
    }

    /**
     * Read extension projects already present in workspace.
     * Only valid projects are returned, folders without project structure omitted.
     *
     * @see {@link ExtensionImportJob.checkForExistingFolders()}
     */
    private void initExtensionsInWorkspace() {
    	extensionsInWorkspace = new TreeSet<String>();
        IWorkspace workspace = WorkspaceFactory.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            String name = iProject.getName();
            extensionsInWorkspace.add(name);
        }
    }

	Set<String> getSelectedExtensions() {
		Set<String> selectedExtensions = new TreeSet<>();
		for (Extension e : selectableExtensions) {
			if (e.isChecked()) {
				selectedExtensions.add(e.getName());
			}
		}
		return selectedExtensions;
	}

	/**
	 * Find all extensions on which the current selection depends on. The evaluation is "deep", i.e. iteratively
	 * finds dependencies of dependencies, too. Extension on which the selected extensions depend on but are
	 * already in workspace are omitted.
	 */
    void findDependenciesOfSelectedExtensions() {
    	logger.debug("remoteExtensions = " + remoteExtensions);
    	logger.debug("selectedExtensions = " + getSelectedExtensions());

    	allRequiredDependencies = new ArrayList<String>(); // the result of this method
    	Set<String> processedExtensions = new HashSet<String>(); // required to avoid infinite loops when cyclic extension dependencies exist
        Set<String> unprocessedExtensions = new TreeSet<String>(getSelectedExtensions()); // copy to keep selectedExtensions unmodified
        while (!unprocessedExtensions.isEmpty()) {
        	// get one unprocessed extension
        	String extension = unprocessedExtensions.iterator().next();
        	unprocessedExtensions.remove(extension);

        	Collection<String> directDependencies = getDirectDependencies(extension);

        	// find all required extensions
        	for (String directDependency : directDependencies) {
        		if (processedExtensions.contains(directDependency)) {
        			logger.debug("Extension " + directDependency + " has already been analyzed, skip...");
        		} else if (!getSelectedExtensions().contains(directDependency) && !allRequiredDependencies.contains(directDependency)) {
        			// the required extension has not been selected and not been registered before
        			if (!extensionsInWorkspace.contains(directDependency)) {
        				// the required extension does not exist yet, so it must be imported
        				logger.info("Extension " + directDependency + " has been auto-selected for import, because the selected extensions depend on it.");
        				allRequiredDependencies.add(directDependency);
        			} // else: the required extension is already in workspace -> do not re-import, but check it's dependencies
        			unprocessedExtensions.add(directDependency);
        		}
        	}

        	/*
        	 * TODO:
        	 * After finishing ticket https://hiszilla.his.de/hiszilla/show_bug.cgi?id=261078, continue work here.
        	 * Namely, finish this ticket here: https://hiszilla.his.de/hiszilla/show_bug.cgi?id=261078.
        	 *
        	 * At this code point the variable classpathFile already knows all members of the ecl1 classpath container.
        	 * Thus, we can use it to obtain the optional dependencies.
        	 */

        	processedExtensions.add(extension);
        }
    }


    /**
     * Returns the direct dependencies of this extension either by parsing the
     * extensions.json (which is necessary for branches that use pipeline jobs)
     * or by parsing individual .classpath files (which is necessary for branches
     * that use freestyle jobs).
     *
     * @param extension
     * @return
     */
    private Collection<String> getDirectDependencies(String extension) {
		Collection<String> directDependencies = new ArrayList<>();

		// Webapps depends on api-extensions that are referenced in the ecl1 classpath container.
		// When importing extensions, we don't want to import the api-extensions as projects and thus we skip webapps
		if ("webapps".equals(extension)) {
			return directDependencies;
		}

		if(extensionsJSON != null) {
			JsonArray upstreamDependenciesAsJsonArray = extensionsJSON.get(extension).getAsJsonObject().get("vorgelagerteExtensions").getAsJsonArray();

			if (!upstreamDependenciesAsJsonArray.isEmpty()) {
				for (JsonElement dependency : upstreamDependenciesAsJsonArray) {
					directDependencies.add(removeBranchSuffixIfPresent(dependency.getAsString()));
				}
			}
		} else {
			directDependencies.addAll(getClasspathFile(extension).getRegularDependencies());

        	// Note: cs.sys.angularbase is not listed in the .classpath and thus we must add it here manually
			// if this is truly an angular extensions
			if (isAngularExtension(extension)) {
        		directDependencies.add("cs.sys.angularbase");
        	}

		}
		return directDependencies;
	}


    /**
     * Removes a branch suffix if present.
     *
     * @param input
     * @return
     */
    private String removeBranchSuffixIfPresent(String input) {
    	String branchWithUnderscore = "_" + branch;
		if (input.endsWith(branchWithUnderscore)) {
			return input.substring(0, input.length() - branchWithUnderscore.length());
		}
		return input;
	}


    private boolean isAngularExtension(String extension) {
		RemoteProjectSearchSupport remoteProjectSearchSupport = new RemoteProjectSearchSupport();
		String angularJson = remoteProjectSearchSupport.getRemoteFileContent(extension, "angular.json", false);
		if (angularJson != null) {
			return true;
		}
		return false;
	}

    private ClasspathFile getClasspathFile(String extension) {
    	ClasspathFile classpathFile = classpathFiles.get(extension);
    	if (classpathFile != null) {
    		return classpathFile;
    	}
    	classpathFile = new ClasspathFile(extension);
    	classpathFiles.put(extension, classpathFile);
    	return classpathFile;
    }

    /**
     * @return extensions that the user-selected extensions depend on.
     */
    Collection<String> getDependenciesOfSelectedExtensions() {
    	return allRequiredDependencies;
    }
}
