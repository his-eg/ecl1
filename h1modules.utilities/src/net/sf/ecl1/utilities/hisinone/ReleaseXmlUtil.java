package net.sf.ecl1.utilities.hisinone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Sets;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.FileUtil;

/**
 * Utilities for management of release.xml files
 *
 * @author keunecke / tneumann
 */
public class ReleaseXmlUtil {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

	private static final String RELEASE_XML_FOLDER = "qisserver/WEB-INF/conf/service/patches/hisinone";
	
	private static final String HOTFIX_PREFIX = "Hotfix ";

    /**
     * @param webapps
     * @return
     */
    public static IFolder getReleaseXmlFolder() {
    	IProject webappsProject = WebappsUtil.findWebappsProject();
    	if (webappsProject!=null) {
    		return webappsProject.getFolder(RELEASE_XML_FOLDER);
    	}
		logger.error("There is no webapps project in your workspace");
		return null;
    }

    /**
     * Find release xml files in workspace
     * @return
     */
    public static Collection<IFile> getReleaseXmlFiles() {
        Collection<IFile> result = Sets.newHashSet();
        IFolder releaseFilesFolder = getReleaseXmlFolder();
        if (releaseFilesFolder.exists()) {
            try {
                List<IResource> releaseFileResources = Arrays.asList(releaseFilesFolder.members());
                for (IResource releaseFileResource : releaseFileResources) {
                    result.add((IFile) releaseFileResource);
                }
            } catch (CoreException e) {
        		logger.error(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Get a specific release.xml file
     *
     * @param file name
     * @return IFile
     */
    public static IFile getReleaseXmlFile(String file) {
    	IFolder releaseXmlFolder = getReleaseXmlFolder();
    	if (releaseXmlFolder!=null) {
	        IFile releaseFile = releaseXmlFolder.getFile(file);
	        if(releaseFile.exists()){
	            return releaseFile;
	        }
    	}
        return null;
    }

    /**
     * @return the next required hotfix version in short notation, like "7.1.0.140".
     * "next required" means the biggest version from the release.xml file with the minor version incremented by 1.
     */
    public static String getIncrementedReleaseXmlVersionShortString() {
    	IFile releaseFile = getReleaseXmlFile("release.xml");
    	if (releaseFile==null) {
    		logger.error("File 'release.xml' does not exist");
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
        String contents = FileUtil.readContent(releaseFile);
        if (contents==null) {
        	logger.error("IOException occurred reading file 'release.xml'");
    		return CvsTagUtil.UNKNOWN_VERSION;
        }

    	// create XML document
    	Document doc = null;
    	InputStream classpathContentStream = null;
    	try {
        	classpathContentStream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	// The release.xml refers to a release.dtd file, which can not be found by the document builder.
        	// This is caused by the builder trying to locate the dtd file directly in the Eclipse main directory,
        	// e.g. C:\HIS-Workspace\Programme\eclipse_neon\release.dtd.
        	// Probably we could pass the right location to the builder factory, but since we do not need validation here,
        	// we simply turn it off following http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
        	factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        	// now create a builder and parse the release.xml
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	doc = builder.parse(classpathContentStream);
    		classpathContentStream.close();
    	} catch (IOException | SAXException | ParserConfigurationException e) {
    		logger.error("Exception parsing 'release.xml' file: " + e);
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
    	if (doc==null) {
    		logger.error("Could not create XML document from 'release.xml' file");
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
    	
    	// parse XML
		Element root = doc.getDocumentElement();
		NodeList patchEntries = root.getElementsByTagName("patch");
		int patchEntriesCount;
		if (patchEntries==null || (patchEntriesCount = patchEntries.getLength()) == 0) {
			logger.error("'release.xml' file does not contain <patch> elements");
    		return CvsTagUtil.UNKNOWN_VERSION;
		}

    	int maxMinorVersion = Integer.MIN_VALUE;    	
    	TreeMap<String, Integer> distinctMajorVersions2Count = new TreeMap<String, Integer>(); // Set  registers only distinct values
        for (int index=0; index<patchEntriesCount; index++) {
        	Node node = patchEntries.item(index);
        	if (!(node instanceof Element)) continue;
        	Element patchEntry = (Element) node;
        	// read hotfix version
        	String hotfixStr = patchEntry.getAttribute("name");
        	if (hotfixStr==null || !hotfixStr.startsWith(HOTFIX_PREFIX)) {
        		// log found problem but otherwise ignore it if there are other <patch> elements
        		logger.error("Patch '" + hotfixStr + "': name does not start with expected prefix '" + HOTFIX_PREFIX + "'");
        		continue;
        	}

        	String versionStr = hotfixStr.substring(HOTFIX_PREFIX.length());
        	int lastPointPos = versionStr.lastIndexOf('.');
        	String majorVersion = versionStr.substring(0, lastPointPos).trim();
        	String minorVersion = versionStr.substring(lastPointPos+1).trim();
        	int minorVersionInt;
        	try {
        		minorVersionInt = Integer.parseInt(minorVersion);
        	} catch (NumberFormatException nfe) {
        		logger.error("Patch '" + hotfixStr + "': minor version " + minorVersion + " is not a number");
        		continue;
        	}

        	// patch is valid, register major version
        	Integer count = distinctMajorVersions2Count.get(majorVersion);
        	int countInt = (count==null) ? 1 : count.intValue()+1; // increment
        	distinctMajorVersions2Count.put(majorVersion, Integer.valueOf(countInt));
        	// check if current minor version is the greatest one so far
        	if (minorVersionInt > maxMinorVersion) {
        		maxMinorVersion = minorVersionInt;
        	}
        }
        
        if (distinctMajorVersions2Count.isEmpty()) {
        	// there was no valid <patch> element
        	logger.error("'release.xml' does not contain valid <patch> elements");
    		return CvsTagUtil.UNKNOWN_VERSION;
        }
        
        // find the major version that occurred most often
        String maxCountMajorVersion = null;
        if (distinctMajorVersions2Count.size()==1) {
        	maxCountMajorVersion = distinctMajorVersions2Count.firstKey();
        } else {
        	// there were distinct major versions
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : distinctMajorVersions2Count.entrySet()) {
            	int count = entry.getValue().intValue();
            	if (count > maxCount) {
            		maxCount = count;
            		maxCountMajorVersion = entry.getKey();
            	}
            }
            logger.error("'release.xml' contains distinct major versions: " + distinctMajorVersions2Count.keySet() + ". Only one of them can be correct.");
        }
        // return highest version with minor version incremented by 1
        return maxCountMajorVersion + "." + String.valueOf(maxMinorVersion + 1);
    }
}
