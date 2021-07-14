package net.sf.ecl1.utilities.hisinone;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

public class EclipseUtil {

	private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, EclipseUtil.class.getSimpleName());

	
    public static void setWorkspaceAutoBuild(boolean flag) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceDescription description = workspace.getDescription();
        description.setAutoBuilding(flag);
        try {
			workspace.setDescription(description);
		} catch (CoreException e) {
			logger.error2("An exception occured when trying to disable/enable auto building. This was the exception: ", e);
		}
    }
	
	
}
