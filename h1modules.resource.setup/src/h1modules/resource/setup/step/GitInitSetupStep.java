package h1modules.resource.setup.step;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;

/**
 * Initialize a git repository in the project root
 * 
 * @author keunecke
 */
public class GitInitSetupStep implements ProjectSetupStep {

    @Override
    public void performStep(IProject project, InitialProjectConfigurationChoices choices) {
        InitCommand initCommand = Git.init();
        IPath rawLocation = project.getFullPath();
        File ws = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        initCommand.setDirectory(new File(ws, rawLocation.lastSegment()));
        initCommand.call();
    }

}
