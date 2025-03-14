package net.sf.ecl1.git.updatehooks;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS_POSIX;
import org.eclipse.jgit.util.FS_Win32;
import org.eclipse.ui.IStartup;

import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;


public class UpdateHooks implements IStartup {
    
    private static final ICommonLogger logger = LoggerFactory.getLogger(UpdateHooks.class.getSimpleName(), UpdateHooksActivator.PLUGIN_ID, UpdateHooksActivator.getDefault());
    
    private static final String HOOKS_DIR_ECLIPSE_PROJECTS = ".git/hooks/commit-msg";
    private static final String HOOKS_DIR_ECL1 = "/githooks/commit-msg";
	private static final String HOOKS_DIR_ECL1_STANDALONE = "ecl1/net.sf.ecl1.git.updatehooks/src/githooks/commit-msg";
    
    private final FS fs = FS.detect();
    
    
	@Override
	public void earlyStartup() {
		// standalone
		if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
			runJob(new NullProgressMonitor());
			return;
		}

		Job job = new Job("ecl1: Updating git hooks in all git versioned HIS projects in the workspace") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return runJob(monitor);
			}
		};
		//Register the job at the activator to enable the activator to cancel the job, when eclipse shut sdown
		UpdateHooksActivator.getDefault().setUpdateHooksJobs(job);
		job.schedule();
	}

	private IStatus runJob(IProgressMonitor monitor) {
		/*
		 * ---------------------------------
		 * Warn Windows users (generally a good idea :) 
		 * ---------------------------------
		 * 
		 * Note: We use JGit methods here to detect the file system (fs), because
		 * the git hook scripts must be executed by JGit later. If JGit detects Windows _without_ 
		 * Cygwin, the user is warned that eclipse/JGit won't execute the git hook scripts.
		 * 
		 * FS_Posix and FS_Win32_Cygwin are unproblematic. Only warn about FS_Win32.
		 */
		if (fs.getClass() == FS_Win32.class) { 
			logger.error2("Detected a Windows machine without sh.exe in its Path! \n"
					+ "Git hooks will only work in Git Bash, but not in eclipse! \n"
					+ "To enable processing of git hooks by eclipse, please refer to the following manual: "
					+ "https://wiki.his.de/mediawiki/index.php/Einrichtung_einer_HISinOne-Arbeitsumgebung#Eclipse_dazu_bef.C3.A4higen_Git_hooks_ausf.C3.BChren_zu_k.C3.B6nnen");
		}
		
		/* -----------------------------------
		 * Determine the number of projects that we have to process
		 * -----------------------------------
		 */
		int totalAmountOfWork = 0;
		//Webapps
		IProject webapps = WebappsUtil.findWebappsProject();
		if (webapps != null) {
			totalAmountOfWork++;
		}
		//Extensions
		IWorkspaceRoot root = WorkspaceFactory.getWorkspace().getRoot();
		TreeMap<String,String> extensions = new TreeMap<>();
		ExtensionUtil.getInstance().scanForExtensionProjects(extensions);
		totalAmountOfWork += extensions.entrySet().size();
		monitor.beginTask("Updating git hooks", totalAmountOfWork);
		
		
		/* -----------------------------------
		 * Update git hook of webapps project
		 * -----------------------------------
		 */
		if(webapps != null) {
			monitor.subTask("webapps");
			if(webapps.getLocation().append(".git").toFile().isFile()) {
				logger.info("Current project is managed by git, but you are currently in a linked work tree. Updating the git hooks will not work in a linked work tree. Skipping...");
			} else {
				try {
					
					Path hooksDirWebapps = Paths.get(webapps.getFolder(HOOKS_DIR_ECLIPSE_PROJECTS).getLocationURI());
					copyHookToDestination(getCommitMsgHook(), hooksDirWebapps);

					logger.info("Successfully updated the git hooks of the webapps project.");
				} catch (IOException e) {
					logger.error2("Failed to update the git hooks of the webapps project. Exception: " + e.getMessage(), e);
				}
			}
			monitor.worked(1);
		} else {
			logger.info("No webapps project found in your workspace.");
		}
		
		
		/* ------------------------------------------
		 * Update git hook of all extension projects
		 * ------------------------------------------
		 */
		for(Entry<String,String> e : extensions.entrySet()) {
			monitor.subTask(e.getValue());
			if(monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			
			IProject extensionProject = root.getProject(e.getValue());
			
			try {
				Path hooksDirExtension = Paths.get(extensionProject.getFolder(HOOKS_DIR_ECLIPSE_PROJECTS).getLocationURI());
				copyHookToDestination(getCommitMsgHook(), hooksDirExtension);
	
				logger.info("Successfully updated the git hooks of the following extensions project: " + extensionProject.getName());
			} catch (IOException e1) {
				logger.error2("Failed to update the git hooks of the folloing extensions project: " + extensionProject.getName() + 
						"\nException: " + e1.getMessage(), e1);
			}

			monitor.worked(1);
		}
		
		
		return Status.OK_STATUS;
	}

	
    private InputStream getCommitMsgHook() {
		Path hooks;
		InputStream is = null;
		if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
			hooks = WorkspaceFactory.getWorkspace().getRoot().getLocation().toPath().resolve(HOOKS_DIR_ECL1_STANDALONE);
			try {
				is = new FileInputStream(hooks.toFile());
			} catch (FileNotFoundException e) {
				logger.error2("Error reading hooks file: " + hooks + "\n" + e.getMessage());
			}
		}else{
			is = this.getClass().getClassLoader().getResourceAsStream(HOOKS_DIR_ECL1);
		}

    	if(is == null) {
    		logger.error2("Failed to locate the git hooks within the ecl1 plugin.");
    	} 
    	return is;
    }
	
    private void copyHookToDestination(InputStream hook, Path destination) throws IOException {
		Files.copy(hook, destination, StandardCopyOption.REPLACE_EXISTING);
		/* 
		 * On a posix-complaint system, we need to set the executable bits manually.
		 * 
		 * We don't need to do this on windows machines, since the executable bits 
		 * are preserved when copying the hook from within the plugin-jar. As a matter of fact: 
		 * the setPosixFilePermissions call will throw an exception on windows machines
		 * (therefore the check for the file system).
		 * 
		 */
		if (fs.getClass() == FS_POSIX.class ) {
			Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("rwxr-x--x"));			
		}
    }
    
}
