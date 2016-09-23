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
    
    /**
     * @return webapps project branch version in long notation, read from webapps/CVS/Tag.</br>
     * 
     * Examples:</br>
     * HISinOne_VERSION_07</br>
     * HISinOne_VERSION_06_RELEASE_01
     */
    public static String getCvsTagVersionLongString() {
        IProject webapps = HISinOneFileUtil.getWebapps();
        IFile file = webapps.getFile("CVS/Tag");
        if (!file.exists()) {
            return "HEAD";
        }
        String contents = HISinOneFileUtil.readContent(file);
        if (contents != null) {
        	// CVS/Tag content is "T" followed by version string -> remove the "T"
        	return contents.trim().substring(1);
        }
        return "UNKNOWN_VERSION";
    }
    
    /**
     * @return webapps project branch and respective version number in short notation, read from webapps/CVS/Tag.</br>
     *
     * Examples:</br>
     * HISinOne_VERSION_07 --> 7.0</br>
     * HISinOne_VERSION_06_RELEASE_01 --> 6.1
     */
    public static String getCvsTagVersionShortString() {
        String longVersion = getCvsTagVersionLongString();
        if (longVersion.equals("HEAD") || longVersion.equals("UNKNOWN_VERSION")) {
        	return longVersion;
        }

        // extract short version from old long version format
        String reducedBranch = longVersion.trim().replace("HISinOne_", "");
        List<String> branchNameComponents = Arrays.asList(reducedBranch.split("_"));
        String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
        String minorVersion = "0";
        if (branchNameComponents.size() > 2) {
            minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(3)));
        }
        return majorVersion + "." + minorVersion;
        // TODO: extract short version from new long version format (e.g. HISinOne_2017_06 ?) when it is exactly known
    }
}
