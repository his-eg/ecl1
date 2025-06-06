/*
 * Copyright (c) 2012 HIS eG All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 21.06.2012 by keunecke
 */
package net.sf.ecl1.utilities.general;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * @author keunecke
 * @version $Revision$
 */
public class WorkspaceSupport {

    /**
     * Determine all projects that could be referenced by a new project
     *
     * @return a list of suitable projects
     */
    public List<String> getPossibleProjectsToReference() {
        List<String> result = new ArrayList<String>();
        IWorkspace workspace = WorkspaceFactory.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            String name = iProject.getName();
            if(isEligibleForReferencing(iProject)) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * Checks if a project is eligible as referenced project:
     * - webapps-project is eligible
     * - any extension project is eligible
     *
     * @param iProject
     * @return true if project may be referenced by a new project
     */
    private boolean isEligibleForReferencing(IProject iProject) {
        if(HisConstants.WEBAPPS.equals(iProject.getName())) {
            return true;
        }
        if(isExtensionProject(iProject)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a project is an extension project
     *
     * @param iProject
     * @return true if iProject is an extension project
     */
    private boolean isExtensionProject(IProject iProject) {
        IPath beanConfPath = new Path("src/java/extension.beans.spring.xml");
        IPath propertiesPath = new Path("extension.ant.properties");
        if (iProject.exists(beanConfPath) || iProject.exists(propertiesPath)) {
            return true;
        }
        return false;
    }

}
