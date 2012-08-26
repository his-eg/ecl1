package net.sf.ecl1.extensionpoint.collector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;
/**
 * Manager for collected extension points
 * 
 * @author keunecke
 */
public final class ExtensionPointManager {
	
	/** Map containing source extension name as key and the defined extension point */
	private static final Map<String, Collection<ExtensionPointInformation>> extensions = new HashMap<String, Collection<ExtensionPointInformation>>();
	
	/**
	 * Add a discovered extension
	 * 
	 * @param extension
	 * @param epi
	 */
	public static final void addExtensions(String extension, Collection<ExtensionPointInformation> epis) {
        Collection<ExtensionPointInformation> collection = extensions.get(extension);
        if (collection == null) {
            collection = new HashSet<ExtensionPointInformation>();
            extensions.put(extension, collection);
        }
        collection.addAll(epis);
	}

    /**
     * Remove extension points from an extension
     * 
     * @param extension
     * @param epis
     */
    public static final void removeExtensions(String extension, Collection<ExtensionPointInformation> epis) {
        Collection<ExtensionPointInformation> collection = extensions.get(extension);
        if (collection != null) {
            collection.removeAll(epis);
        }
    }

    /**
     * Get all extension points
     * 
     * @return map with key extension and value collection of contained extension points
     */
	public static final Map<String, Collection<ExtensionPointInformation>> getExtensions() {
		return new HashMap<String, Collection<ExtensionPointInformation>>(extensions);
	}

}
