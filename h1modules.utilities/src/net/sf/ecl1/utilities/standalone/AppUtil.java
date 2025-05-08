package net.sf.ecl1.utilities.standalone;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;


public class AppUtil {

    public static void setCustomWorkspacePathIfExists(String[] args){
        // Dont use logger because it depends on workspace path
        if(args.length==1) {
            Path customPath = Paths.get(args[0]);
            if(!customPath.toFile().isDirectory() || !customPath.toFile().exists()){
                System.err.println("Path is invalid. It either does not exist or is not a directory. Path: " + customPath.toString());
                System.exit(1);
            }
            System.out.println("Using custom workspace path: " +customPath.toString());
            WorkspaceFactory.setCustomPath(customPath);
        }else if(args.length>1){
            System.err.println("Expected 0 or 1 argument! Got: " + Arrays.toString(args));
            System.exit(1);
        }
    }
}
