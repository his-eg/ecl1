package net.sf.ecl1.utilities.standalone.vscode;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;

import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceRootImpl;

/**
 * Class to hide non-Projects from workspace-parent in vscode workspace.
 */
public class HideNonProjectsInWorkspace {

    public static void main(String[] args) {
        Path wsParentPath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath().getParent();
        String[] exclusionProperties = new String[]{"files.exclude", "search.exclude", "files.watcherExclude"};

        List<String> wsParentFiles = new ArrayList<>(Arrays.asList(wsParentPath.toFile().list()));
        List<IProject> projects = new WorkspaceRootImpl().getParentFolderProjects();

        // Remove folders that should not be excluded
        for(IProject project : projects){
            wsParentFiles.remove(project.getName());
        }
        wsParentFiles.remove(".vscode");
        wsParentFiles.remove("eclipse-workspace");

        SettingsHelper helper = new SettingsHelper();
        List<String> oldExclusions;
        // Remove exclusions that no longer exist in the workspace
        for (String property : exclusionProperties){
            oldExclusions = helper.getExclusions(property);
            oldExclusions.removeAll(wsParentFiles);
            for (String exclusion : oldExclusions){
                helper.removeExclusion(property, exclusion);
            }
        }

        // Set exclusions
        for(String file : wsParentFiles){
            for (String property : exclusionProperties){
                helper.setExclusion(property, file );
            }
        }

        helper.save();
    }
}