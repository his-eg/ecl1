/*
 * Copyright (c) 2012 HIS eG All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 14.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.pages.packages;

import java.util.Collection;

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
