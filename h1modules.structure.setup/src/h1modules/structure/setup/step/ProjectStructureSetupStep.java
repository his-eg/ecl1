/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 28.06.2012 by keunecke
 */
package h1modules.structure.setup.step;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;

import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import de.his.cs.sys.extensions.wizards.utils.ProjectSupport;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ProjectStructureSetupStep implements ProjectSetupStep {
    
    private static final String[] PATHS = { "src/java", "src/test", "src/generated", "resource" };

    @Override
    public void performStep(IProject project) {
        try {
            ProjectSupport support = new ProjectSupport();
            support.addNatures(project);
            support.addToProjectStructure(project, PATHS);
            support.setSourceFolders(project, PATHS);
            support.setJreEnvironment(project);
        } catch (JavaModelException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

}
