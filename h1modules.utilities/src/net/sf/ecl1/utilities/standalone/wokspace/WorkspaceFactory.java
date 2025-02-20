package net.sf.ecl1.utilities.standalone.wokspace;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import net.sf.ecl1.utilities.Activator;


/**
 * A factory class for creating {@link IWorkspace} objects that work in Eclipse and non-Eclipse (standalone) environments.
 * <p>
 * The factory determines the correct {@link IWorkspace} implementation based on the environment
 * <ul>
 *     <li>If running in Eclipse, it returns an instance of {@link org.eclipse.core.internal.resources.Workspace}</li>
 *     <li>If running in a non-Eclipse environment, it returns an instance of {@link WorkspaceImpl}</li>
 * </ul>
 */
public class WorkspaceFactory {

    /**
     * Create a new {@link IWorkspace} instance based on the environment.
     */
    public static IWorkspace getWorkspace() {
        if (Activator.isRunningInEclipse()) {
            return ResourcesPlugin.getWorkspace();
        } else {
            return new WorkspaceImpl();
        }
    }
}
