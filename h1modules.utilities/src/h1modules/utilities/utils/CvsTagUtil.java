package h1modules.utilities.utils;

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
     * RELEASE_2017_06                (new convention)</br>
     * HISinOne_VERSION_07            (old convention)</br>
     * HISinOne_VERSION_06_RELEASE_01 (old convention)
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
}
