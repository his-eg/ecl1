package net.sf.ecl1.extensionpoint.collector.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	
    private static final Collection<ExtensionPointManagerChangeListener> listeners = new LinkedList<ExtensionPointManagerChangeListener>();

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
        updateListeners();
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
        updateListeners();
    }

    /**
     * Get all extension points
     * 
     * @return map with key extension and value collection of contained extension points
     */
	public static final Map<String, Collection<ExtensionPointInformation>> getExtensions() {
		return new HashMap<String, Collection<ExtensionPointInformation>>(extensions);
	}

    public static final void register(ExtensionPointManagerChangeListener l) {
        listeners.add(l);
    }

    private static final void updateListeners() {
        for (ExtensionPointManagerChangeListener listener : listeners) {
            listener.update();
        }
    }

}
