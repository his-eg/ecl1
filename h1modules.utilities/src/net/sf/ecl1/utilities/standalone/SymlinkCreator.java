package net.sf.ecl1.utilities.standalone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Class to add folders via symlink to the workspace.
 */
public class SymlinkCreator {

    private static final ICommonLogger logger = LoggerFactory.getLogger(SymlinkCreator.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("Missing required command-line argument! Please provide the target path to be linked.\n(Gradle usage: runSymlinkCreator -PsymlinkTarget=\"targetPath\")");
            System.exit(1);
        } else if (args.length > 1) {
            logger.error("Too many arguments! You should provide exactly one target path to be linked. Received: " 
                          + args.length + " arguments: " + Arrays.toString(args));
            System.exit(1); 
        }
        

        Path target = Paths.get(args[0]);
        Path link = Paths.get(WorkspaceFactory.getWorkspace().getRoot().getLocationURI()).resolve(target.getFileName());

        if (Files.exists(link)) {
            logger.info("Target (" + target.toString() + ") is already in the workspace.");
            System.exit(0);
        }        
        try {
            Files.createSymbolicLink(link, target);
            logger.info("Symlink created: " + link.toAbsolutePath() + " -> " + target.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Error creating symlink: " + e.getMessage(), e);
        }
    }
}
