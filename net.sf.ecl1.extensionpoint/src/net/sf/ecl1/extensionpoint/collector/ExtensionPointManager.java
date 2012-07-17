package net.sf.ecl1.extensionpoint.collector;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
		extensions.remove(extension);
		extensions.put(extension, epis);
	}

	public static final Map<String, Collection<ExtensionPointInformation>> getExtensions() {
		return new HashMap<String, Collection<ExtensionPointInformation>>(extensions);
	}

}
