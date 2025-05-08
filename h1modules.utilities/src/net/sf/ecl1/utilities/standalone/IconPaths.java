package net.sf.ecl1.utilities.standalone;

import java.nio.file.Path;

import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;


public class IconPaths {

    private static final String ECL1_ICON = "ecl1/h1modules.utilities/icons/ecl1_icon.png";

    private static Path workspacePath = null;
    
    private static Path getWorkspacePath(){
        if(workspacePath == null){
            workspacePath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath();
        }
        return workspacePath;
    }

    public static String getEcl1IconPath(){
        return getWorkspacePath().resolve(ECL1_ICON).toString();
    }
}
