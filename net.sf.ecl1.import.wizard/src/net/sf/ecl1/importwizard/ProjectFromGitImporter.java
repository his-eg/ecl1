package net.sf.ecl1.importwizard;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Imports projects from a git repo into local workspace
 *
 * @author keunecke
 */
public class ProjectFromGitImporter {
	
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, ProjectFromGitImporter.class.getSimpleName());
	
    // base path of the source repo configured in preferences, e.g. "ssh://git@git.his.de/"
    private final String baseRepositoryPath;

    private final boolean openProjectOnCreation;

    /**
     * Create a new ProjectFromGitImporter with a source repository base path
     *
     * @param basePath
     */
    public ProjectFromGitImporter(String basePath, boolean openProjectOnCreation) {
        if (!basePath.endsWith("/")) {
            baseRepositoryPath = basePath + "/";
        } else {
            baseRepositoryPath = basePath;
        }
        this.openProjectOnCreation = openProjectOnCreation;
    }

    /**
     * Import an extension into local eclipse workspace
     *
     * @param extensionToImport name to the extension
     * 
     * @throws CoreException
     */
    public void importProject(String extensionToImport) throws CoreException {
    	
//        disableAutoBuild();
        
        // Compute full repository URL depending on configuration
        String fullRepositoryPath = getFullRepositoryPath(extensionToImport);
    	logger.debug("Extension " + extensionToImport + ": fullRepositoryPath = " + fullRepositoryPath);
    	if (fullRepositoryPath != null) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IPath workspacePath = workspace.getRoot().getLocation();
            IPath extensionPath = workspacePath.append(extensionToImport);
            File extensionFolder = extensionPath.toFile();
            String branch = PreferenceWrapper.getBuildServerView();

            try {
                try {
                    CloneCommand clone = Git.cloneRepository();
                    if (branch != null && !"HEAD".equals(branch)) {
                        clone.setBranch(branch);
                    }
                    clone.setDirectory(extensionFolder).setURI(fullRepositoryPath).setCloneAllBranches(true);
                    clone.call();
                } catch (GitAPIException e) {
            		logger.error2(e.getMessage(), e);
                }

                IProject extensionProject = workspace.getRoot().getProject(extensionToImport);
                IProjectDescription description = extensionProject.getWorkspace().newProjectDescription(extensionProject.getName());

                extensionProject.create(description, null);

                
                if (openProjectOnCreation) {
                    extensionProject.open(null);
                }

            } catch (CoreException e1) {
                throw e1;
            } finally {
                if (extensionFolder.exists()) {
                    extensionFolder.delete();
                }
//                enableAutoBuild();
            }
    	}
    }
    
    private String getFullRepositoryPath(String extensionToImport) {
    	logger.debug("Extension " + extensionToImport + ": baseRepositoryPath = " + baseRepositoryPath);

		// git lab style, e.g. ssh://git@gitlab.his.de/h1/cs/sys/cs.sys.build.utilities
		int c1Pos = extensionToImport.indexOf('.');
		if (c1Pos < 0) {
	    	logger.error2("Extension " + extensionToImport + ": Illegal extension name! An extension name must have at least 2 dot-separated segments!");
	    	return null;
		}
		String segment1 = extensionToImport.substring(0, c1Pos);
		String segment2;
		int c2Pos = extensionToImport.indexOf('.', c1Pos+1);
		if (c2Pos < 0) {
			// Some extensions like pm.hrm or rt.rtt have only 2 segments...
			segment2 = extensionToImport.substring(c1Pos+1);
		} else {
			segment2 = extensionToImport.substring(c1Pos+1, c2Pos);
		}
		// Create new URL according to https://hiszilla.his.de/hiszilla/show_bug.cgi?id=194146
		return baseRepositoryPath + "h1/" + segment1 + "/" + segment2 + "/" + extensionToImport;
    }
    
    private void enableAutoBuild() throws CoreException {
        setWorkspaceAutoBuild(true);
    }

    private void disableAutoBuild() throws CoreException {
        setWorkspaceAutoBuild(false);
    }

    private void setWorkspaceAutoBuild(boolean flag) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceDescription description = workspace.getDescription();
        description.setAutoBuilding(flag);
        workspace.setDescription(description);
    }
}
