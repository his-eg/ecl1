/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 14.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.pages.packages;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ExtensionStrategy implements PackageStructureStrategy {

	/* (non-Javadoc)
	 * @see de.his.cs.sys.extensions.wizards.pages.PackageStructureStrategy#packagesToCreate(java.lang.String)
	 */
	@Override
	public Collection<String> packagesToCreate(String project) {
		return Arrays.asList("de.his.extensions." + project);
	}

}
