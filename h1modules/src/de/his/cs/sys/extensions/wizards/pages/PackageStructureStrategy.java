/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 14.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.pages;

import java.util.Collection;

import org.eclipse.core.resources.IProject;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public interface PackageStructureStrategy {
	
	/**
	 * @param project
	 * @return package names to create
	 */
	public Collection<String> packagesToCreate(String project);
	
}
