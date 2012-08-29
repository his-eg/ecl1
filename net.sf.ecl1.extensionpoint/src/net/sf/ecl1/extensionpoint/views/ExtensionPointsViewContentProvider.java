package net.sf.ecl1.extensionpoint.views;

import java.util.ArrayList;

import net.sf.ecl1.extensionpoint.collector.manager.ExtensionPointManager;
import net.sf.ecl1.extensionpoint.collector.model.ExtensionPointInformation;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.google.common.collect.Multimap;
/**
 * ContentProvider for ExtensionPoints
 *  
 * @author keunecke
 */
public class ExtensionPointsViewContentProvider implements IStructuredContentProvider {
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        v.refresh();
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
        Multimap<IType, ExtensionPointInformation> allExtensions = ExtensionPointManager.get().getExtensions();
        ArrayList<ExtensionPointInformation> extensionPoints = new ArrayList<ExtensionPointInformation>();
        extensionPoints.addAll(allExtensions.values());
        return extensionPoints.toArray();
	}

}