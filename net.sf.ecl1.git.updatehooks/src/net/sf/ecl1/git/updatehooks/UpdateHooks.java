package net.sf.ecl1.git.updatehooks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS_Win32;
import org.eclipse.jgit.util.FS_Win32_Cygwin;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;

import com.sun.org.apache.xpath.internal.ExtensionsProvider;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.ExtensionUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

public class UpdateHooks implements IStartup {
    
	private static final ConsoleLogger logger = new ConsoleLogger(UpdateHooksActivator.getDefault().getLog(), UpdateHooksActivator.PLUGIN_ID, UpdateHooks.class.getSimpleName());
    
    private static final String HOOKS_DIR_ECLIPSE_PROJECTS = ".git/hooks/commit-msg";
    private static final String HOOKS_DIR_ECL1 = "/githooks/commit-msg";
    private static final int MAX_SIZE_OF_HOOK = 50000;
	
    
	@Override
	public void earlyStartup() {
		Job job = new Job("ecl1UpdateGitHooks") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
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
				FS fs = FS.detect();
				if (fs.getClass() == FS_Win32.class) { 
					logger.error2("Detected a Windows machine without sh.exe in its Path! \n"
							+ "Git hooks will only work in Git Bash, but not in eclipse! \n"
							+ "To enable processing of git hooks by eclipse, try running the following command in cmd.exe:\n"
							+ "setx PATH \"PATH=%PATH%;C:\\Program Files\\Git\\usr\\bin\"");
				}
				
				
				/*
				 * ---------------------------
				 * Locate git hook within ecl1
				 * ---------------------------
				 */
				Path hooksDirEcl1;
				try {
					/*
					 * Reading from within the plugin is a little complicated as explained here:
					 * http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
					 */
					URL url = new URL("platform:/plugin/" + UpdateHooksActivator.PLUGIN_ID + HOOKS_DIR_ECL1);
					hooksDirEcl1 = Paths.get(FileLocator.resolve(url).toURI());
					
				} catch (URISyntaxException | IOException e) {
					logger.error2("Failed to locate the git hooks within the ecl1 plugin. Exception: " + e.getMessage(), e);
					return Status.CANCEL_STATUS;
				}
				
				
				/* -----------------------------------
				 * Update git hook of webapps project
				 * -----------------------------------
				 */
				IProject webapps = WebappsUtil.findWebappsProject();
				if(webapps != null) {
					try {
						Path hooksDirWebapps = Paths.get(webapps.getFolder(HOOKS_DIR_ECLIPSE_PROJECTS).getLocationURI());

						Files.copy(hooksDirEcl1, hooksDirWebapps, StandardCopyOption.REPLACE_EXISTING);
						logger.info("Successfully updated the git hooks of the webapps project.");
					} catch (IOException e) {
						logger.error2("Failed to update the git hooks of the webapps project. Exception: " + e.getMessage(), e);
					}
					
				} else {
					logger.info("No webapps project found in your workspace.");
				}
				
				
				/* ------------------------------------------
				 * Update git hook of all extension projects
				 * ------------------------------------------
				 */
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				
				TreeMap<String,String> extensions = new TreeMap<>();
				ExtensionUtil.getInstance().scanForExtensionProjects(extensions);
				

				for(Entry<String,String> e : extensions.entrySet()) {
					IProject extensionProject = root.getProject(e.getValue());
					
					try {
						
						Path hooksDirExtension = Paths.get(extensionProject.getFolder(HOOKS_DIR_ECLIPSE_PROJECTS).getLocationURI());
						
						Files.copy(hooksDirEcl1, hooksDirExtension, StandardCopyOption.REPLACE_EXISTING);
						logger.info("Successfully updated the git hooks of the following extensions project: " + extensionProject.getName());
					} catch (IOException e1) {
						logger.error2("Failed to update the git hooks of the folloing extensions project: " + extensionProject.getName() + 
								"\nException: " + e1.getMessage(), e1);
					}

					
				}
			
				
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
