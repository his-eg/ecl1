package net.sf.ecl1.changeset.exporter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Sets;

/**
 * Utilities for management of release.xml files
 *
 * @author keunecke
 *
 */
public class ReleaseXmlUtil {

	private static final String RELEASE_XML_FOLDER = "qisserver/WEB-INF/conf/service/patches/hisinone";
	
	private static final String HOTFIX_PREFIX = "Hotfix ";
	
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
     * @param webapps
     * @return
     */
    public static IFolder getReleaseXmlFolder() {
        return getWebapps().getFolder(RELEASE_XML_FOLDER);
    }

    /**
     * Get a specific release.xml file
     *
     * @param file name
     * @return IFile
     */
    public static IFile getReleaseXmlFile(String file) {
        IFile releaseFile = getReleaseXmlFolder().getFile(file);
        if(releaseFile.exists()){
            return releaseFile;
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
    		return "UNKNOWN_VERSION";
    	}
        String contents = readContent(releaseFile);
        if (contents==null) {
    		System.err.println("IOException occurred reading file 'release.xml'");
    		return "UNKNOWN_VERSION";
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
    		return "UNKNOWN_VERSION";
    	}
    	if (doc==null) {
    		System.err.println("Could not create XML document from 'release.xml' file");
    		return "UNKNOWN_VERSION";
    	}
    	
    	// parse XML
		Element root = doc.getDocumentElement();
		NodeList patchEntries = root.getElementsByTagName("patch");
		int patchEntriesCount;
		if (patchEntries==null || (patchEntriesCount = patchEntries.getLength()) == 0) {
    		System.err.println("'release.xml' file does not contain <patch> elements");
    		return "UNKNOWN_VERSION";
		}
		
    	String firstMajorVersion = null;
    	int maxMinorVersion = Integer.MIN_VALUE;
        for (int index=0; index<patchEntriesCount; index++) {
        	Node node = patchEntries.item(index);
        	if (!(node instanceof Element)) continue;
        	Element patchEntry = (Element) node;
        	// read hotfix version
        	String hotfixStr = patchEntry.getAttribute("name");
        	if (hotfixStr==null || !hotfixStr.startsWith(HOTFIX_PREFIX)) {
        		// log but otherwise ignore if there are other patches
        		System.err.println("Patch '" + hotfixStr + "': name does not start with expected prefix 'Hotfix '");
        		continue;
        	}
        	int lastPointPos = hotfixStr.lastIndexOf('.');
        	if (lastPointPos<0) {
        		// log but otherwise ignore if there are other patches
        		System.err.println("Patch '" + hotfixStr + "': name does not contain '.' as major/minor version separator");
        		continue;
        	}
        	
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
        	if (firstMajorVersion==null) {
        		firstMajorVersion = majorVersion;
        	} else {
        		if (!majorVersion.equals(firstMajorVersion)) {
            		System.err.println("Patch '" + hotfixStr + "': major version differs from first element major version " + firstMajorVersion);
            		// ignore otherwise
        		}
        	}

        	// check if current minor version is the new greatest
        	if (minorVersionInt > maxMinorVersion) {
        		maxMinorVersion = minorVersionInt;
        	}
        }
        
        if (firstMajorVersion==null) {
        	// there was no valid <patch> element
    		System.err.println("'release.xml' does not contain valid <patch> elements");
    		return "UNKNOWN_VERSION";
        }
        // return highest version with minor version incremented by 1
        return firstMajorVersion + "." + String.valueOf(maxMinorVersion + 1);
    }
    
    /**
     * Utility method for determining webapps project branch and respective version number
     *
     * examples:
     * - HISinOne_VERSION_07 --> 7.0
     * - HISinOne_VERSION_06_RELEASE_01 --> 6.1
     * @return
     */
    public static String getCvsTagVersionShortString() {
        IProject webapps = getWebapps();
        IFile file = webapps.getFile("CVS/Tag");
        if (!file.exists()) {
            return "HEAD";
        }
        String contents = readContent(file);
        if (contents != null) {
            String reducedBranch = contents.trim().replace("THISinOne_", "");
            List<String> branchNameComponents = Arrays.asList(reducedBranch.split("_"));
            String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
            String minorVersion = "0";
            if (branchNameComponents.size() > 2) {
                minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(3)));
            }
            return majorVersion + "." + minorVersion;
        }
        return "UNKNOWN_VERSION";
    }

    /**
     * Read a file's content to a string
     *
     * @param file
     * @return
     */
    private static String readContent(IFile file) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(file.getLocationURI()));
            return new String(encoded, Charset.defaultCharset()).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IProject getWebapps() {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        List<IProject> projects = Arrays.asList(ws.getRoot().getProjects());
        for (IProject project : projects) {
            if (isWebapps(project)) {
                return project;
            }
        }
        return null;
    }

    private static boolean isWebapps(IProject project) {
        return project.exists(new Path("qisserver"));
    }
}
