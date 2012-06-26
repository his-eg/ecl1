/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 26.06.2012 by keunecke
 */
package de.his.cs.sys.extensions.steps;

import org.eclipse.jdt.core.IJavaProject;

/**
 * Step for the setup of an extension project
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public interface ProjectSetupStep {
	
	/**
	 * perform the actions of this step on the given project
	 * 
	 * @param project
	 */
	public void performStep(IJavaProject project);

}
