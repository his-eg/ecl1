package h1modules.resource.setup.step;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * Initialize a git repository in the project root
 * 
 * @author keunecke
 */
public class GitInitSetupStep implements ProjectSetupStep {

    private static final ConsoleLogger logger = new ConsoleLogger(); // TODO We can't get an ILog here

    @Override
    public void performStep(IProject project, InitialProjectConfigurationChoices choices) {
        try {
        	InitCommand initCommand = Git.init();
        	IPath rawLocation = project.getFullPath();
        	File ws = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        	initCommand.setDirectory(new File(ws, rawLocation.lastSegment()));
			initCommand.call();
		} catch (GitAPIException e) {
			logger.error("Init of git repo failed: " + e.getMessage(), e);
		}
    }
}
