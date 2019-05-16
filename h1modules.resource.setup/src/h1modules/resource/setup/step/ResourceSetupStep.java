package h1modules.resource.setup.step;

import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;
import h1modules.resource.setup.Activator;
import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * @author keunecke
 * @company HIS eG
 */
public class ResourceSetupStep implements ProjectSetupStep {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ResourceSetupStep.class.getSimpleName());

    @Override
    public void performStep(IProject project, InitialProjectConfigurationChoices choices) {
    	logger.debug("Starting resource setup");
        try {
            new ResourceSupport(project, choices).createFiles();
        } catch (CoreException | UnsupportedEncodingException e) {
    		logger.error2(e.getMessage(), e);
        }
        logger.debug("Finished resource setup");
    }
}
