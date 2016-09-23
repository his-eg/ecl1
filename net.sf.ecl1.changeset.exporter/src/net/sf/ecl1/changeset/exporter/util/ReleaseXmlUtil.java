package net.sf.ecl1.changeset.exporter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

import h1modules.utilities.utils.CvsTagUtil;
import h1modules.utilities.utils.HISinOneFileUtil;

/**
 * Utilities for management of release.xml files
 *
 * @author keunecke / tneumann
 */
public class ReleaseXmlUtil {

	private static final String RELEASE_XML_FOLDER = "qisserver/WEB-INF/conf/service/patches/hisinone";
	
	private static final String HOTFIX_PREFIX = "Hotfix ";

    /**
     * @param webapps
     * @return
     */
    public static IFolder getReleaseXmlFolder() {
    	IProject webappsProject = HISinOneFileUtil.getWebapps();
    	if (webappsProject!=null) {
    		return webappsProject.getFolder(RELEASE_XML_FOLDER);
    	}
		System.err.println("There is no webapps project in your workspace");
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
                e.printStackTrace();
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
    		System.err.println("File 'release.xml' does not exist");
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
        String contents = HISinOneFileUtil.readContent(releaseFile);
        if (contents==null) {
    		System.err.println("IOException occurred reading file 'release.xml'");
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
    		System.err.println("Exception parsing 'release.xml' file: " + e);
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
    	if (doc==null) {
    		System.err.println("Could not create XML document from 'release.xml' file");
    		return CvsTagUtil.UNKNOWN_VERSION;
    	}
    	
    	// parse XML
		Element root = doc.getDocumentElement();
		NodeList patchEntries = root.getElementsByTagName("patch");
		int patchEntriesCount;
		if (patchEntries==null || (patchEntriesCount = patchEntries.getLength()) == 0) {
    		System.err.println("'release.xml' file does not contain <patch> elements");
    		return CvsTagUtil.UNKNOWN_VERSION;
		}
		
    	String referenceMajorVersion = CvsTagUtil.getCvsTagVersionShortString();
    	if (referenceMajorVersion.equals(CvsTagUtil.HEAD_VERSION) || referenceMajorVersion.equals(CvsTagUtil.UNKNOWN_VERSION)) {
    		// the reference version string is not comparable with the entries in release.xml -> determine dynamically 
    		referenceMajorVersion = null;
    	}
    	int maxMinorVersion = Integer.MIN_VALUE;
    	
        for (int index=0; index<patchEntriesCount; index++) {
        	Node node = patchEntries.item(index);
        	if (!(node instanceof Element)) continue;
        	Element patchEntry = (Element) node;
        	// read hotfix version
        	String hotfixStr = patchEntry.getAttribute("name");
        	if (hotfixStr==null || !hotfixStr.startsWith(HOTFIX_PREFIX)) {
        		// log found problem but otherwise ignore it if there are other <patch> elements
        		System.err.println("Patch '" + hotfixStr + "': name does not start with expected prefix 'Hotfix '");
        		continue;
        	}
        	// count points in version
        	int pointCount = 0;
        	int pos = -1;
        	while ((pos = hotfixStr.indexOf('.', pos+1)) > -1) {
        		pointCount++;
        	}
        	if (pointCount != 3) {
        		System.err.println("Patch '" + hotfixStr + "': version does not have the expected format x.x.x.x");
        		continue;
        	}
        	int lastPointPos = hotfixStr.lastIndexOf('.');
        	String majorVersion = hotfixStr.substring(HOTFIX_PREFIX.length(), lastPointPos).trim();
        	String minorVersion = hotfixStr.substring(lastPointPos+1).trim();
        	int minorVersionInt;
        	try {
        		minorVersionInt = Integer.parseInt(minorVersion);
        	} catch (NumberFormatException nfe) {
        		System.err.println("Patch '" + hotfixStr + "': minor version " + minorVersion + " is not a number");
        		continue;
        	}
        	
        	// check major version consistency
        	if (referenceMajorVersion==null) {
        		referenceMajorVersion = majorVersion;
        	} else {
        		if (!majorVersion.equals(referenceMajorVersion)) {
            		System.err.println("Patch '" + hotfixStr + "': major version differs from first major version " + referenceMajorVersion);
            		// ignore otherwise
        		}
        	}

        	// check if current minor version is the new greatest
        	if (minorVersionInt > maxMinorVersion) {
        		maxMinorVersion = minorVersionInt;
        	}
        }
        
        if (referenceMajorVersion==null) {
        	// there was no valid <patch> element
    		System.err.println("'release.xml' does not contain valid <patch> elements");
    		return CvsTagUtil.UNKNOWN_VERSION;
        }
        // return highest version with minor version incremented by 1
        return referenceMajorVersion + "." + String.valueOf(maxMinorVersion + 1);
    }
}
