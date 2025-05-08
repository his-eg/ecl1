package net.sf.ecl1.updatecheck.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.updatecheck.UpdateCheckActivator;
import net.sf.ecl1.utilities.general.SwtUtil;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.AppUtil;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Class that handles checking for updates and applying updates to ecl1 standalone.
 */
public class UpdateCheckApp{

    private static final ICommonLogger logger = LoggerFactory.getLogger(UpdateCheckApp.class.getSimpleName(), UpdateCheckActivator.PLUGIN_ID, UpdateCheckActivator.getDefault());

    public static void main(String[] args){
		AppUtil.setCustomWorkspacePathIfExists(args);
        Path workspacePath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath();
		File ecl1Git = workspacePath.resolve("ecl1").resolve(".git").toFile();

        try (Git git = Git.open(ecl1Git)){
			Repository repo = git.getRepository();

			// Fetch latest changes
			git.fetch().call();

            BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repo, "master");

            if (trackingStatus != null) {
                int aheadCount = trackingStatus.getAheadCount();
                int behindCount = trackingStatus.getBehindCount();

                if (aheadCount > 0 && behindCount == 0) {
                    logger.warn("Local ecl1 is ahead of remote. Skipping pull to avoid overwriting local changes.");
                }else if (aheadCount > 0 && behindCount > 0) {
                    logger.warn("Local ecl1 and remote have diverged. Manual merge may be required.");
                } else if (behindCount > 0) {
                    PullResult pullResult = git.pull().call();
                    if (pullResult.isSuccessful()) {
                        logger.info("Updated ecl1 successfully.");
                        showUpdateDialog();
                    } else {
                        logger.error("Failed to pull ecl1.");
                    }
                } else {
                    logger.debug("No ecl1 update available.");
                }
            } else {
                logger.warn("Git Tracking information for ecl1 master branch not found. Skipping pull.");
            }

		} catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
			logger.error("Ecl1 is not managed via Git?: " + e.getMessage());
		} catch (IOException | GitAPIException | JGitInternalException e) {
			logger.error("Failed to pull ecl1: " + e.getMessage());
		}
    }

	private static void showUpdateDialog() {
        Display display = new Display();
        SwtUtil.bringShellToForeground(display);
		Image icon = new Image(display, UpdateCheckApp.class.getResourceAsStream("/ecl1_icon.png"));
		String msg = "Latest ecl1 version pulled successfully.\n\n";
		MessageDialog dialog = new MessageDialog(display.getActiveShell(), "Ecl1 Update", icon, msg , MessageDialog.INFORMATION, new String[] { "OK" }, 0);

        dialog.open();

        if (!icon.isDisposed()) {
            icon.dispose();
        }
        if (!display.isDisposed()) {
            display.dispose();
        }
        dialog.close();
    }
}