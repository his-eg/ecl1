package net.sf.ecl1.utilities.general;

import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

/**
 * SWT-related utility class.
 */
public class SwtUtil {

    private static final ICommonLogger logger = LoggerFactory.getLogger(Activator.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    /**
     * Brings the active shell to the foreground by minimizing and then maximizing it.
     * This ensures the shell becomes the active window, even if it was previously in the background.
     *
     * @param display the Display instance, which is used to get the active shell.
     */
    public static void bringShellToForeground(Display display) {
        if(display == null){
            logger.error("Error bringing Shell to foreground, Display is null");
            return;
        }
        // If running from jar shell is already in foreground
        boolean isJar = SwtUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar");
        if(isJar){
            return;
        }
        
        display.asyncExec(() -> {
            if(display.getActiveShell() == null){
                logger.warn("Could not bringing Shell to foreground, shell is null");
                return;
            }
            if(!display.getActiveShell().getMinimized()){
                display.getActiveShell().setMinimized(true);
            }
            display.getActiveShell().setMinimized(false);
        });
    }
}