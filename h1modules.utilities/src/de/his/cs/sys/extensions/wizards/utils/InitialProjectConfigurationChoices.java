/*
 * Copyright (c) 2012 HIS eG All Rights Reserved.
 *
 * $Id$
 *
 * $Log$
 *
 * Created on 02.07.2012 by keunecke
 */
package de.his.cs.sys.extensions.wizards.utils;

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
     * Standard constructor
     *
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

    /**
     * Get the projects that will be referenced by a new project
     * @return a collection of project names
     */
    public Collection<String> getProjectsToReference() {
        return projectsToReference;
    }

    /**
     * The name of the new project
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The initial version for the extension
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

}
