/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 21.06.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class WorkspaceSupport {
	
	public Collection<String> getPossibleProjectsToReference() {
		Collection<String> result = new ArrayList<String>();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
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
		if(HISConstants.WEBAPPS.equals(iProject.getName())) {
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
		IPath path = new Path("src/java/extension.beans.spring.xml");
		if(iProject.exists(path)) {
			return true;
		}
		return false;
	}

}
