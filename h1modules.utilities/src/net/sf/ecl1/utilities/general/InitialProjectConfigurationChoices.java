package net.sf.ecl1.utilities.general;

import java.util.Collection;

/**
 * Container for the initial values for a new extension project entered by a user
 *
 * @company HIS eG
 * @author keunecke
 */
public class InitialProjectConfigurationChoices {

    private final Collection<String> projectsToReference;

    private final String name;

    private final String version;

    /**
     * Standard constructor
     *
     * @param projectsToReference, must not be null
     * @param name, must not be null
     * @param version, must not be null
     */
    public InitialProjectConfigurationChoices(Collection<String> projectsToReference, String name, String version) {
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
