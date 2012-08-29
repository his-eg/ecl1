package net.sf.ecl1.extensionpoint.collector.manager;

import java.util.Collection;
import java.util.LinkedList;

import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.jdt.core.IType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
/**
 * Manager for collected extension points
 * 
 * @author keunecke
 */
public final class ExtensionPointManager {

    private static ExtensionPointManager instance;
	
	/** Map containing source extension name as key and the defined extension point */
    private final Multimap<IType, ExtensionPointInformation> extensions = HashMultimap.create();
	
    private final Collection<ExtensionPointManagerChangeListener> listeners = new LinkedList<ExtensionPointManagerChangeListener>();

    /**
     * Singleton Getter
     * @return the singleton instance
     */
    public static ExtensionPointManager get() {
        if(instance == null) {
            instance = new ExtensionPointManager();
        }
        return instance;
    }

	    /**
     * Add discovered extensions
     * 
     * @param type 
     * @param epis 
     */
    public final void addExtensions(IType type, Collection<ExtensionPointInformation> epis) {
        extensions.putAll(type, epis);
        updateListeners();
	}

    /**
     * Remove extension points from an extension
     * 
     * @param type 
     */
    public final void removeExtensions(IType type) {
        extensions.removeAll(type);
        updateListeners();
    }

    /**
     * Clear all known extension points
     */
    public final void clear() {
        this.extensions.clear();
    }

    /**
     * Get all extension points
     * 
     * @return map with key extension and value collection of contained extension points
     */
    public final Multimap<IType, ExtensionPointInformation> getExtensions() {
        return HashMultimap.create(extensions);
	}

    /**
     * Register a change listener
     * @param l
     */
    public final void register(ExtensionPointManagerChangeListener l) {
        listeners.add(l);
    }

    private final void updateListeners() {
        for (ExtensionPointManagerChangeListener listener : listeners) {
            listener.update();
        }
    }

}
