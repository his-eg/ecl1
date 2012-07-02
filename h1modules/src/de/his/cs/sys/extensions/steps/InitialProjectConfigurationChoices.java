/*
 * Copyright (c) 2012 HIS GmbH All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 02.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.steps;

import java.util.Collection;

/**
 * Container for the initial values for a new extension project entered by a user
 * 
 * @author keunecke
 * @version $Revision$ 
 */
public class InitialProjectConfigurationChoices {
	
	private final Collection<String> projectsToReference;
	
	private final String name;
	
	private final String version;

	/**
	 * @param projectsToReference
	 * @param name
	 * @param version
	 */
	public InitialProjectConfigurationChoices(Collection<String> projectsToReference, String name,
			String version) {
		this.projectsToReference = projectsToReference;
		this.name = name;
		this.version = version;
	}

	public Collection<String> getProjectsToReference() {
		return projectsToReference;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

}
