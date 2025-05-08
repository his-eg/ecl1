package net.sf.ecl1.utilities.standalone.workspace;

import java.nio.file.Path;

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
 * Non-Eclipse environments can use {@link WorkspaceFactory.setCustomPath} to set a custom workspace path.
 */
public class WorkspaceFactory {

    private static Path customWorkspacePath;
    /**
     * Create a new {@link IWorkspace} instance based on the environment.
     */
    public static IWorkspace getWorkspace() {
        if (Activator.isRunningInEclipse()) {
            return ResourcesPlugin.getWorkspace();
        } else {
            if(customWorkspacePath == null){
                return new WorkspaceImpl();
            }
            return new WorkspaceImpl(customWorkspacePath);
        }
    }

    /**
     * Set custom path for workspace.
     */
    public static void setCustomPath(Path path){
        customWorkspacePath = path;
    }
}
