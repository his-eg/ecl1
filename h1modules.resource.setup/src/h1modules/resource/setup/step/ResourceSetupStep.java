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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.ResourceSupport;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ResourceSetupStep implements ProjectSetupStep {

    /* (non-Javadoc)
     * @see de.his.cs.sys.extensions.steps.ProjectSetupStep#performStep(de.his.cs.sys.extensions.steps.IJavaProject)
     */
    @Override
    public void performStep(IJavaProject project) {
        System.out.println("performing resource setup");
        try {
            new ResourceSupport(project.getProject()).createFiles();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
