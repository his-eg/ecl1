package de.his.cs.sys.extensions.setup.step;

import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.his.cs.sys.extensions.Activator;
import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.ResourceSupport;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * @author keunecke
 * @company HIS eG
 */
public class ResourceSetupStep implements ProjectSetupStep {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ResourceSetupStep.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

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
