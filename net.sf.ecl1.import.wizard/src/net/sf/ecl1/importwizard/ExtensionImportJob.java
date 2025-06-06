package net.sf.ecl1.importwizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * The job that actually performs deleting of existing folders in the workspace and the extension import.
 * It is called from ExtensionImportWizard's performFinish() method and executed as a separate thread.
 *
 * @author Keunecke / tneumann
 */
public class ExtensionImportJob extends Job {

    private static final ICommonLogger logger = LoggerFactory.getLogger(ExtensionImportJob.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

    private static final String ERROR_MESSAGE_EXISTING_FOLDER = "Your workspace contains folders named like extensions you want to import: %s\n\nThese folders must be deleted before the import, but first you might want to check if they contain files you want to keep. Then delete the folders manually or set the 'Delete folders?' option on the confirmation page of this wizard.";
    private static final String ERROR_MESSAGE_DELETE_FAILED = "Some extensions could not be imported, because deleting existing folders before the import failed: %s";

	private Collection<String> extensionsToImport;
	private boolean openProjectsAfterImport;
	private boolean deleteFolders;
	
	public final static Object JOB_FAMILY = new Object();

	public ExtensionImportJob(Collection<String> extensionsToImport, boolean openProjectsAfterImport, boolean deleteFolders) {
		super("Extension Import");
		this.extensionsToImport = extensionsToImport;
		this.openProjectsAfterImport = openProjectsAfterImport;
		this.deleteFolders = deleteFolders;
	}
		
    @Override
	public boolean belongsTo(Object family) {
		return JOB_FAMILY.equals(family);
	}
    
    
    @Override
    protected IStatus run(IProgressMonitor monitor) { 	
    	    	
    	// The folders in workspace that must be scrubbed before extension import
        Collection<String> existingFolders = checkForExistingFolders(extensionsToImport);
        String existingFoldersStr = Joiner.on(", ").join(existingFolders);

        // convert monitor to SubMonitor and set total number of work units
        final int totalWork = existingFolders.size() + extensionsToImport.size();
        SubMonitor subMonitor = SubMonitor.convert(monitor, totalWork);

        // first part of the job: delete projects from workspace (if requested)
    	ArrayList<String> extensionsWithDeleteErrors = new ArrayList<String>();
        if (!existingFolders.isEmpty()) {
            if (deleteFolders==false) {
            	// scrubbing folders is necessary but not permitted by the user
            	String message = String.format(ERROR_MESSAGE_EXISTING_FOLDER, existingFoldersStr);
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
            }

            // scrubbing folders is necessary and authorized
            for (String extension : existingFolders) {
				//Check, if user has requested a cancel
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

                // set the name of the current work
                String taskName = "Delete extension folder from workspace: " + extension;
                subMonitor.setTaskName(taskName);
                logger.info(taskName);

                // do one task
                IWorkspace workspace = WorkspaceFactory.getWorkspace();
                IWorkspaceRoot root = workspace.getRoot();
                File workspaceFile = root.getLocation().toFile();
                File extensionFolder = new File(workspaceFile, extension);
                try {
                    FileUtils.deleteDirectory(extensionFolder);
                } catch (IOException e) {
                	extensionsWithDeleteErrors.add(extension);
                	logger.error2("Extension folder " + extension + " could not be deleted from workspace", e);
                }
                // reduce total work by 1
                subMonitor.worked(1);
            }
        }

        // Second part of the job: import extension projects from git repository.
        // Extensions with folders in the workspace that could not be deleted can not be imported.
        String reposerver = PreferenceWrapper.getGitServer();
        logger.debug("reposerver = " + reposerver);
        ProjectFromGitImporter importer = new ProjectFromGitImporter(reposerver, openProjectsAfterImport);
        for (String extension : extensionsToImport) {
        	if (extensionsWithDeleteErrors.contains(extension)) {
        		// delete failed -> extension can not be imported
        		continue;
        	}

				//Check, if user has requested a cancel
				if(monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

                // set the name of the current work
                String taskName = "Import extension " + extension;
                subMonitor.setTaskName(taskName);
                logger.info(taskName);
                
                try {
                	importer.importProject(extension);
	            } catch (CoreException e) {
	            	logger.error2("An exception occured while trying to import the following extension: " + extension + 
	            			"\n This is the exception: ",e);
	            	return Status.CANCEL_STATUS;
	            }
                
                // reduce total work by 1
                subMonitor.worked(1);

        }

        // result
        if (!extensionsWithDeleteErrors.isEmpty()) {
        	// some extensions could not be imported because existing folders could not be deleted
        	String extensionsWithDeleteErrorsStr = Joiner.on(", ").join(extensionsWithDeleteErrors);
        	String message = String.format(ERROR_MESSAGE_DELETE_FAILED, extensionsWithDeleteErrorsStr);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message);
        }
        
    	return Status.OK_STATUS;
    }

    /**
     * Check if a folder with the name of an extension to import name already exists in workspace.
     * Any folders are returned, not just such with a valid project nature.
     *
     * @param extensionsToImport
     * @return the folders that already exist in workspace
     *
     * @see {@link ExtensionImportWizardModel.initExtensionsInWorkspace()}
     */
    private Collection<String> checkForExistingFolders(Collection<String> extensionsToImport) {
        IWorkspace workspace = WorkspaceFactory.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IPath fullPath = root.getLocation();
        File workspaceFile = fullPath.toFile();
        Collection<String> result = Lists.newArrayList();

        for (String extension : extensionsToImport) {
            File extensionFolder = new File(workspaceFile, extension);
            if (extensionFolder.exists()) {
                result.add(extension);
            }
        }
        return result;
    }  
    
}
