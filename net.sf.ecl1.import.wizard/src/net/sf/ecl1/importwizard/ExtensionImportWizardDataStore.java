package net.sf.ecl1.importwizard;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;
import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

/**
 * Data used throughout the Extension Import Wizard.
 * @author tneumann
 */
public class ExtensionImportWizardDataStore {
    
    private RemoteProjectSearchSupport remoteProjectSearchSupport = null;
    
    // actual branch
    private String branch = null;
    
    // All extensions existing on repo server
    private Set<String> remoteExtensions = null;

    // All extensions existing in workspace
    private Set<String> extensionsInWorkspace = null;

    // Extensions chosen by the user for installation
    private Set<String> selectedExtensions = null;

    // map from extension to the extensions they need.
    // a null list means that the extension dependency has not been initialized yet.
    private Map<String, List<String>> extensions2dependencyExtensions;

    // dependent extensions
    private Collection<String> dependencyExtensions = null;

    public ExtensionImportWizardDataStore() {
    	remoteProjectSearchSupport = new RemoteProjectSearchSupport();
    	initRemoteExtensions();
        initExtensionsInWorkspace();
        extensions2dependencyExtensions = new HashMap<String, List<String>>();
    }
    
    /**
     * @return Jenkins access helper
     */
    RemoteProjectSearchSupport getRemoteProjectSearchSupport() {
    	return remoteProjectSearchSupport;
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
            System.out.println("Replacing original '" + remoteProjectIncludingBranch + "' with '" + remoteProject + "'");
            remoteExtensions.add(remoteProject);
        }
    }

    /**
     * @return collection of remote extensions, initialized by wizard page 1
     */
    Collection<String> getRemoteExtensions() {
    	return remoteExtensions;
    }

    String getBranch() {
    	if (branch==null) {
            IPreferenceStore store = Activator.getDefault().getPreferenceStore();
            branch = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE);
    	}
    	return branch;
    }
    
    /**
     * Read extensions already present in workspace.
     */
    private void initExtensionsInWorkspace() {
    	extensionsInWorkspace = new TreeSet<String>();
        Collection<String> result = new ArrayList<String>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            String name = iProject.getName();
            result.add(name);
        }
        extensionsInWorkspace.addAll(result);
    }

	Set<String> getExtensionsInWorkspace() {
		return extensionsInWorkspace;
	}
	
	void setSelectedExtensions(Set<String> selectedExtensions) {
		this.selectedExtensions = selectedExtensions;
	}
	
	Set<String> getSelectedExtensions() {
		return this.selectedExtensions;
	}
    
    void initAllDependencyExtensions() {
    	System.out.println("remoteExtensions = " + remoteExtensions);
    	System.out.println("selectedExtensions = " + selectedExtensions);
        
    	dependencyExtensions = new ArrayList<String>();
        Set<String> unprocessedExtensions = new TreeSet<String>(selectedExtensions); // copy to keep selectedExtensions unmodified
        while (!unprocessedExtensions.isEmpty()) {
        	// get one unprocessed extension
        	String currentExtension = unprocessedExtensions.iterator().next();
        	unprocessedExtensions.remove(currentExtension);
        	// find dependencies of currentExtension
        	Collection<String> dependencies = getDirectDependencyExtensions(currentExtension);
        	for (String dependency : dependencies) {
        		// is this a dependency that must be considered and has not been considered yet?
        		if (!selectedExtensions.contains(dependency) && !dependencyExtensions.contains(dependency)) {
        			dependencyExtensions.add(dependency);
        			unprocessedExtensions.add(dependency);
        		}
        	}
        }
    }

	/**
	 * Returns the list of extensions directly required by the given extension.
	 * "directly" means that there is no recurrent evaluation.
	 * 
	 * @param extension
	 * @return list of extension names
	 */
	List<String> getDirectDependencyExtensions(String extension) {
		List<String> dependencyExtensions = extensions2dependencyExtensions.get(extension);
		if (dependencyExtensions != null) {
			return dependencyExtensions;
		}
		
		// dependencies have not been initialized yet for the given extension.
		// register empty list to signal that initialization has taken place now:
		dependencyExtensions = new ArrayList<String>();
		extensions2dependencyExtensions.put(extension, dependencyExtensions);
		// read extension ".classpath" file
    	String classpathContent = remoteProjectSearchSupport.getRemoteFileContent(extension, ".classpath");
    	if (classpathContent == null) {
    		// there is no ".classpath"-file in the extension project?
    		return dependencyExtensions; // return empty list
    	}
    	
    	// create XML document
    	Document doc = null;
    	StringReader classpathContentStream = new StringReader(classpathContent); 
    	try {
        	doc = new SAXBuilder().build(classpathContentStream);
    	} catch (IOException | JDOMException e) {
    		System.out.println("Exception parsing '.classpath' file for extension " + extension  + ": " + e);
    	} finally {
    		classpathContentStream.close();
    	}
    	if (doc==null) {
    		System.out.println("Could not create XML document from '.classpath' file of extension " + extension);
    		return dependencyExtensions; // return empty list
    	}
    	
    	// parse XML: adapted from class ExtensionProjectDependencyLoader
		Element root = doc.getRootElement();
        @SuppressWarnings("unchecked")
		List<Element> classpathEntries = root.getChildren("classpathentry");
        for (Element classpathEntry : classpathEntries) {
            if (isProjectDependency(classpathEntry)) {
                String projectDependency = classpathEntry.getAttributeValue("path").substring(1);
                dependencyExtensions.add(projectDependency);
            }
        }
		return dependencyExtensions;
	}

	// copied from class ExtensionProjectDependencyLoader
    private boolean isProjectDependency(Element classpathEntry) {
    	return isSourceEntry(classpathEntry) && isProjectRelatedEntry(classpathEntry);
    }

	// copied from class ExtensionProjectDependencyLoader
    private boolean isSourceEntry(Element classpathEntry) {
        String kind = classpathEntry.getAttributeValue("kind");
        return kind != null && "src".equals(kind);
    }

	// copied from class ExtensionProjectDependencyLoader
    private boolean isProjectRelatedEntry(Element classpathEntry) {
        String path = classpathEntry.getAttributeValue("path");
        return path != null && !path.isEmpty() && path.startsWith("/") && !"/webapps".equals(path);
    }

    /**
     * @return extensions that the user-selected extensions depend on.
     */
    Collection<String> getAllDependencyExtensions() {
    	return dependencyExtensions;
    }
}
