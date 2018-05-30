package net.sf.ecl1.utilities.hisinone;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import net.sf.ecl1.utilities.general.FileUtil;

/**
 * Utility methods to extract version strings from webapps/CVS/Tag file.
 * 
 * @author keunecke / tneumann
 */
public class CvsTagUtil {
	
	public static final String HEAD_VERSION = "HEAD";
	public static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
    
	private static final String CVS_TAG = "CVS/Tag";

    /**
     * @return webapps project branch version in long notation, read from webapps/CVS/Tag.</br>
     * 
     * Examples:</br>
     * RELEASE_2017_06                (new convention)</br>
     * HISinOne_VERSION_07            (old convention)</br>
     * HISinOne_VERSION_06_RELEASE_01 (old convention)
     */
    public static String getCvsTagVersionLongString() {
        IProject webapps = WebappsUtil.findWebappsProject();
        if (webapps != null) {
        	// webapps project exists. all branches except HEAD have a webapps/CVS/Tag file:
            IFile file = webapps.getFile(CVS_TAG);
            if (!file.exists()) {
                return HEAD_VERSION;
            }
            String contents = FileUtil.readContent(file);
            if (contents != null) {
            	// CVS/Tag content is "T" followed by version string -> remove the "T"
            	return contents.trim().substring(1);
            }
        }
        return UNKNOWN_VERSION;
    }
}
