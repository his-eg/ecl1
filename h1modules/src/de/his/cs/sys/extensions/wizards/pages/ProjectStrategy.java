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

import java.util.Arrays;
import java.util.Collection;

/**
 * @author keunecke
 * @version $Revision$ 
 */
public class ProjectStrategy implements PackageStructureStrategy {

	@Override
	public Collection<String> packagesToCreate(String project) {
		return Arrays.asList(project);
	}

}
