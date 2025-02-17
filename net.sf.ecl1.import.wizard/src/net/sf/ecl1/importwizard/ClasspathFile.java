package net.sf.ecl1.importwizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.ecl1.utilities.general.RemoteProjectSearchSupport;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.logging.ConsoleLogger;

public class ClasspathFile {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ClasspathFile.class.getSimpleName());
	
	public static final String ECL1_CLASSPATH_CONTAINER = HisConstants.NET_SF_ECL1_ECL1_CONTAINER_ID + "/";
	public static final String JENKINS_WEBAPPS_NAME = "/webapps";
	
	private Collection<String> regularDependencies = new ArrayList<>();
	private Collection<String> ecl1ContainerDependencies = new ArrayList<>();
	
    private RemoteProjectSearchSupport remoteProjectSearchSupport = new RemoteProjectSearchSupport();
	
	public ClasspathFile(String extension) {
		String classpathContent = remoteProjectSearchSupport.getRemoteFileContent(extension, ".classpath", false);
		if (classpathContent == null) {
			return;
		}
		
		// create XML document
		Document doc = null;
		InputStream classpathContentStream = null;
		try {
	    	classpathContentStream = new ByteArrayInputStream(classpathContent.getBytes("UTF-8"));
	    	doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(classpathContentStream);
			classpathContentStream.close();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			logger.error2("Exception parsing '.classpath' file for extension " + extension  + ": " + e.getMessage(), e);
		}
		if (doc==null) {
			logger.error2("Could not create XML document from '.classpath' file of extension " + extension);
			return;
		}
		
		// parse XML: adapted from class ExtensionProjectDependencyLoader,
		// and converted to org.w3c.dom because there is no Eclipse Osgi bundle for org.jdom
		Element root = doc.getDocumentElement();
		NodeList classpathEntries = root.getElementsByTagName("classpathentry");
		if (classpathEntries!=null) {
	        int classpathEntriesCount = classpathEntries.getLength();
	        for (int index=0; index<classpathEntriesCount; index++) {
	        	Node node = classpathEntries.item(index);
	        	if (!(node instanceof Element)) continue;
	        	Element classpathEntry = (Element) node;
	        	if (isProjectDependency(classpathEntry)) {
	                String projectDependency = classpathEntry.getAttribute("path").substring(1);
	                regularDependencies.add(projectDependency);
	            }
	        	if(isEcl1Container(classpathEntry)) {
	        		ecl1ContainerDependencies = parseEclContainerPath(classpathEntry.getAttribute("path"));
	        	}
	        }
		}
	}
	
	private Collection<String> parseEclContainerPath(String path) {
		//Remove signature
		path = path.replace(ECL1_CLASSPATH_CONTAINER, "");
		//Parse
		return new ArrayList<String>(Arrays.asList(path.split(",")));
	}

	// adapted from class ExtensionProjectDependencyLoader
	private boolean isProjectDependency(Element classpathEntry) {
	    String kind = classpathEntry.getAttribute("kind");
	    boolean isSourceEntry = kind != null && "src".equals(kind);
	    if (!isSourceEntry) { // "Early" abort
	    	return false;
	    }
	    String path = classpathEntry.getAttribute("path");
	    boolean isProjectRelatedEntry = path != null && !path.isEmpty() && path.startsWith("/") && !JENKINS_WEBAPPS_NAME.equals(path);
		return isProjectRelatedEntry;
	}
	
	private boolean isEcl1Container(Element classpathEntry) {
		String kind = classpathEntry.getAttribute("kind");
	    boolean isConEntry = kind != null && "con".equals(kind);
	    if (!isConEntry) { // "Early" abort
	    	return false;
	    }
	    String path = classpathEntry.getAttribute("path");
	    boolean isEcl1Container = path != null && !path.isEmpty() && path.startsWith(ECL1_CLASSPATH_CONTAINER);
	    return isEcl1Container;
	}



	public Collection<String> getEcl1ContainerDependencies() {
		return ecl1ContainerDependencies;
	}

	public Collection<String> getRegularDependencies() {
		return regularDependencies;
	}
}
