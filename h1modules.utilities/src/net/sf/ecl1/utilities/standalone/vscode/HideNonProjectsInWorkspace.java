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

        List<String> wsParentFiles = new ArrayList<>(Arrays.asList(wsParentPath.toFile().list()));
        List<IProject> projects = new WorkspaceRootImpl().getParentFolderProjects();

        for(IProject project : projects){
            wsParentFiles.remove(project.getName());
        }
        wsParentFiles.remove(".vscode");
        wsParentFiles.remove("eclipse-workspace");

        SettingsHelper helper = new SettingsHelper();
        for(String file : wsParentFiles){
            helper.setExclusion("files.exclude", file + "/");
            helper.setExclusion("search.exclude", file + "/");
            helper.setExclusion("files.watcherExclude", file + "/");
        }
        helper.save();
    }
}