/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 26.06.2012 by keunecke
 */
package h1modules.resource.setup.step;

import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.InitialProjectConfigurationChoices;
import de.his.cs.sys.extensions.wizards.utils.ProjectSupport;
import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ResourceSetupStep implements ProjectSetupStep {
    
    @Override
    public void performStep(IProject project, InitialProjectConfigurationChoices choices) {
        System.out.println("performing resource setup");
        try {
            new ResourceSupport(project, choices).createFiles();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
