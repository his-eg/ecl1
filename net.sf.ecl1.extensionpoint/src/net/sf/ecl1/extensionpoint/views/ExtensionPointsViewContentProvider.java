package net.sf.ecl1.extensionpoint.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.sf.ecl1.extensionpoint.collector.ExtensionPointManager;
import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
/**
 * ContentProvider for ExtensionPoints
 *  
 * @author keunecke
 */
public class ExtensionPointsViewContentProvider implements IStructuredContentProvider {
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
		Map<String, Collection<ExtensionPointInformation>> extensions = ExtensionPointManager.getExtensions();
		ArrayList<ExtensionPointInformation> extensionPoints = new ArrayList<ExtensionPointInformation>();
		for (Collection<ExtensionPointInformation> extensionPointInformation : extensions.values()) {
			extensionPoints.addAll(extensionPointInformation);
		}
		return extensionPoints.toArray();
	}
}