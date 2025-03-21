package net.sf.ecl1.updatecheck.standalone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.updatecheck.UpdateCheckActivator;
import net.sf.ecl1.utilities.general.SwtUtil;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.IconPaths;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Class that handles checking for updates and applying updates to ecl1 standalone.
 */
public class UpdateCheckApp{

    private static final ICommonLogger logger = LoggerFactory.getLogger(UpdateCheckApp.class.getSimpleName(), UpdateCheckActivator.PLUGIN_ID, UpdateCheckActivator.getDefault());

    public static void main(String[] args){
        Path workspacePath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath();
		File ecl1Git = workspacePath.resolve("ecl1").resolve(".git").toFile();

		String vsixBefore = getExtensionVsix(workspacePath);

        try (Git git = Git.open(ecl1Git)){
			Repository repo = git.getRepository();

			// Fetch latest changes
			git.fetch().call();

            ObjectId localHead = repo.resolve("refs/heads/master^{commit}");
			ObjectId remoteHead = repo.resolve("refs/remotes/origin/master^{commit}");

			// check for changes
			if (!localHead.equals(remoteHead)) {
				PullResult pullResult = git.pull().call();
				if (pullResult.isSuccessful()) {
					logger.info("Updated ecl1 successfully.");
					String vsixAfter = getExtensionVsix(workspacePath);
					if(!vsixBefore.equals(vsixAfter)){
						logger.info("New VSCode Extension version available: " + vsixBefore + " -> " + vsixAfter);
						showUpdateDialog(workspacePath, vsixBefore, true);
					}else{
						showUpdateDialog(workspacePath, vsixBefore, false);
					}
				} else {
					logger.error("Failed to pull ecl1.");
				}
			} 

		} catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
			logger.error("Ecl1 is not managed via Git?: " + e.getMessage());
		} catch (IOException | GitAPIException | JGitInternalException e) {
			logger.error("Failed to pull ecl1: " + e.getMessage());
		}
    }

	private static String getExtensionVsix(Path workspacePath){
		File vsixFolder = workspacePath.resolve("ecl1/vscodeExtension/ecl1").toFile();
		String[] searchFiles = vsixFolder.list();
		for(String file : searchFiles){
			if(file.endsWith(".vsix")){
				return file;
			}
		}
		return "";
	}

	private static void showUpdateDialog(Path workspacePath, String vsix, boolean extensionUpdate) {
        Display display = new Display();
        SwtUtil.bringShellToForeground(display);
        Image icon = new Image(display, IconPaths.getEcl1IconPath());

		String msg = "Latest ecl1 version pulled successfully.\n\n";
		if(extensionUpdate){
			msg += "New VSCode Extension available!\n\nManual installation required!\n\npath: " + workspacePath.resolve("ecl1/vscodeExtension/ecl1").resolve(vsix).toString();
		}
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