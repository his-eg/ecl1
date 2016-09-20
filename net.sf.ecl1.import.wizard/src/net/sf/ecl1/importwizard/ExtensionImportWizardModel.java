package net.sf.ecl1.importwizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;
import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

/**
 * The data model of the Extension Import Wizard.
 * @author tneumann
 */
public class ExtensionImportWizardModel {
    
	private static final String JENKINS_WEBAPPS_NAME = "/webapps";

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

    public ExtensionImportWizardModel() {
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
    
	/**
	 * Find all extensions on which the current selection depends on. The evaluation is "deep", i.e. iteratively
	 * finds dependencies of dependencies, too.
	 */
    void findDeepDependencyExtensions() {
    	//System.out.println("remoteExtensions = " + remoteExtensions);
    	//System.out.println("selectedExtensions = " + selectedExtensions);
        
    	dependencyExtensions = new ArrayList<String>();
        Set<String> unprocessedExtensions = new TreeSet<String>(selectedExtensions); // copy to keep selectedExtensions unmodified
        while (!unprocessedExtensions.isEmpty()) {
        	// get one unprocessed extension
        	String currentExtension = unprocessedExtensions.iterator().next();
        	unprocessedExtensions.remove(currentExtension);
        	// find dependencies of currentExtension
        	Collection<String> dependencies = findFlatDependencyExtensions(currentExtension);
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
	 * Only one dependency level is evaluated.
	 * 
	 * @param extension
	 * @return list of extension names
	 */
	List<String> findFlatDependencyExtensions(String extension) {
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
    	InputStream classpathContentStream = null;
    	try {
        	classpathContentStream = new ByteArrayInputStream(classpathContent.getBytes("UTF-8"));
        	doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(classpathContentStream);
    		classpathContentStream.close();
    	} catch (IOException | SAXException | ParserConfigurationException e) {
    		System.out.println("Exception parsing '.classpath' file for extension " + extension  + ": " + e);
    	}
    	if (doc==null) {
    		System.out.println("Could not create XML document from '.classpath' file of extension " + extension);
    		return dependencyExtensions; // return empty list
    	}
    	
    	// parse XML: adapted from class ExtensionProjectDependencyLoader,
    	// and converted to org.w3c.dom because there is no Eclipse Osgi bundle for org.jdom
		Element root = doc.getDocumentElement();
		NodeList classpathEntries = root.getElementsByTagName("classpathentry");
        int classpathEntriesSize = classpathEntries.getLength();
        for (int index=0; index<classpathEntriesSize; index++) {
        	Node node = classpathEntries.item(index);
        	if (!(node instanceof Element)) continue;
        	Element classpathEntry = (Element) node;
        	if (isProjectDependency(classpathEntry)) {
                String projectDependency = classpathEntry.getAttribute("path").substring(1);
                dependencyExtensions.add(projectDependency);
            }
        }
		return dependencyExtensions;
	}

	// adapted from class ExtensionProjectDependencyLoader
    private boolean isProjectDependency(Element classpathEntry) {
        String kind = classpathEntry.getAttribute("kind");
        boolean isSourceEntry = kind != null && "src".equals(kind);
        String path = classpathEntry.getAttribute("path");
        boolean isProjectRelatedEntry = path != null && !path.isEmpty() && path.startsWith("/") && !JENKINS_WEBAPPS_NAME.equals(path);
    	return isSourceEntry && isProjectRelatedEntry;
    }

    /**
     * @return extensions that the user-selected extensions depend on.
     */
    Collection<String> getDeepDependencyExtensions() {
    	return dependencyExtensions;
    }
}
