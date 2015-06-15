package net.sf.ecl1.importwizard;

import h1modules.utilities.utils.Activator;

import java.io.File;
import java.util.Collection;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Imports projects from a git repo into local workspace
 *
 * @author keunecke
 */
public class ProjectFromGitImporter {

    // base path of the source repo, e.g. ssh://git@git/
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
     * Import a collection of extensions into local eclipse workspace
     *
     * @param extensionsToImport names to the extensions
     * @throws CoreException
     */
    public void importProjects(Collection<String> extensionsToImport) throws CoreException {
        for (String extension : extensionsToImport) {
            importProject(extension);
        }
    }

    /**
     * Import an extension into local eclipse workspace
     *
     * @param extensionToImport name to the extension
     * @throws CoreException
     */
    public void importProject(String extensionToImport) throws CoreException {
        disableAutoBuild();
        String fullRepositoryPath = baseRepositoryPath + extensionToImport;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath workspacePath = workspace.getRoot().getLocation();
        IPath extensionPath = workspacePath.append(extensionToImport);
        File extensionFolder = extensionPath.toFile();
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String branch = store.getString(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE);

        try {

            try {
                CloneCommand clone = Git.cloneRepository();
                if (branch != null && !"HEAD".equals(branch)) {
                    clone.setBranch(branch);
                }
                clone.setDirectory(extensionFolder).setURI(fullRepositoryPath).setCloneAllBranches(true);
                clone.call();
            } catch (GitAPIException e) {
                e.printStackTrace();
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
            enableAutoBuild();
        }
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
