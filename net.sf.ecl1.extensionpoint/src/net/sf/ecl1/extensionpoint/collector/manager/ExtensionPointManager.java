package net.sf.ecl1.extensionpoint.collector.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.jdt.core.IType;
/**
 * Manager for collected extension points
 * 
 * @author keunecke
 */
public final class ExtensionPointManager {

    private static ExtensionPointManager instance;
	
	/** Map containing source extension name as key and the defined extension point */
    private final Map<String, Map<String, Collection<ExtensionPointInformation>>> extensions = new HashMap<String, Map<String, Collection<ExtensionPointInformation>>>();
	
    private final Collection<ExtensionPointManagerChangeListener> listeners = new LinkedList<ExtensionPointManagerChangeListener>();

    public static ExtensionPointManager get() {
        if(instance == null) {
            instance = new ExtensionPointManager();
        }
        return instance;
    }

	/**
	 * Add a discovered extension
	 * 
	 * @param extension
	 * @param epi
	 */
    public final void addExtensions(String extension, IType type, Collection<ExtensionPointInformation> epis) {
        Map<String, Collection<ExtensionPointInformation>> pointsInProject = extensions.get(extension);
        if (pointsInProject == null) {
            pointsInProject = new HashMap<String, Collection<ExtensionPointInformation>>();
            extensions.put(extension, pointsInProject);
        }
        String fullyQualifiedName = type.getFullyQualifiedName();
        Collection<ExtensionPointInformation> collection = pointsInProject.get(fullyQualifiedName);
        if (collection == null) {
            collection = new HashSet<ExtensionPointInformation>();
            pointsInProject.put(fullyQualifiedName, collection);
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
    public final void removeExtensions(String extension, IType type) {
        Map<String, Collection<ExtensionPointInformation>> pointsInProject = extensions.get(extension);
        Collection<ExtensionPointInformation> collection = pointsInProject.remove(type.getFullyQualifiedName());
        if (collection != null) {
            collection.clear();
        }
        updateListeners();
    }

    /**
     * Get all extension points
     * 
     * @return map with key extension and value collection of contained extension points
     */
    public final Map<String, Map<String, Collection<ExtensionPointInformation>>> getExtensions() {
        return new HashMap<String, Map<String, Collection<ExtensionPointInformation>>>(extensions);
	}

    public final void register(ExtensionPointManagerChangeListener l) {
        listeners.add(l);
    }

    private final void updateListeners() {
        for (ExtensionPointManagerChangeListener listener : listeners) {
            listener.update();
        }
    }

}
