package h1modules.utilities.utils;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * Utility methods to extract version strings from webapps/CVS/Tag file.
 * 
 * @author keunecke / tneumann
 */
public class CvsTagUtil {
    
	public static final String CVS_TAG = "CVS/Tag";
	
	public static final String HEAD_VERSION = "HEAD";
	public static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
	
	public static final String HISINONE_PREFIX = "HISinOne_";
	public static final String VERSION_PREFIX = "VERSION";

    /**
     * @return webapps project branch version in long notation, read from webapps/CVS/Tag.</br>
     * 
     * Examples:</br>
     * HISinOne_VERSION_07</br>
     * HISinOne_VERSION_06_RELEASE_01
     */
    public static String getCvsTagVersionLongString() {
        IProject webapps = HISinOneFileUtil.getWebapps();
        if (webapps != null) {
        	// webapps project exists. all branches except HEAD have a webapps/CVS/Tag file:
            IFile file = webapps.getFile(CVS_TAG);
            if (!file.exists()) {
                return HEAD_VERSION;
            }
            String contents = HISinOneFileUtil.readContent(file);
            if (contents != null) {
            	// CVS/Tag content is "T" followed by version string -> remove the "T"
            	return contents.trim().substring(1);
            }
        }
        return UNKNOWN_VERSION;
    }
    
    /**
     * @return webapps project branch and respective version number in short notation, read from webapps/CVS/Tag.</br>
     *
     * Examples:</br>
     * old convention:
     * HISinOne_VERSION_07 --> 7.0</br>
     * HISinOne_VERSION_06_RELEASE_01 --> 6.1
     * new convention:
     * HISinOne_2017_06 -> 2017.6
     */
    public static String getCvsTagVersionShortString() {
        String longVersion = getCvsTagVersionLongString();
        if (longVersion.equals(HEAD_VERSION) || longVersion.equals(UNKNOWN_VERSION)) {
        	return longVersion;
        }

        // extract short version from old long version format
        String reducedBranch = longVersion.trim().replace(HISINONE_PREFIX, "");
        List<String> branchNameComponents = Arrays.asList(reducedBranch.split("_"));
        if (reducedBranch.startsWith(VERSION_PREFIX)) {
        	// old convention like HISinOne_VERSION_07 or HISinOne_VERSION_06_RELEASE_01
	        String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
	        String minorVersion = "0";
	        if (branchNameComponents.size() > 2) {
	            minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(3)));
	        }
	        return majorVersion + "." + minorVersion;
        } else {
        	// new convention like HISinOne_2017_06
        	String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(0)));
        	String minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
	        return majorVersion + "." + minorVersion;
        }
    }
}
