package net.sf.ecl1.importwizard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

/**
 * Job that actually performs deleting of existing folders in the workspace and the extension import.
 * Is called from ExtensionImportWizard's performFinish() method.
 * @author Keunecke/tneumann
 */
public class ExtensionImportJob extends Job {

    private static final String ERROR_MESSAGE_EXISTING_FOLDER = "Workspace contains folders named like extensions you want to import. Delete them first: %s";
    private static final String ERROR_MESSAGE_DELETE_FAILED = "Some extensions in your workspace could not be deleted: %s";

	private Collection<String> extensionsToImport;
	private boolean openProjectsAfterImport;
	private boolean deleteFolders;
	
	private ArrayList<String> errorMessages = new ArrayList<String>();
	
	public ExtensionImportJob(Collection<String> extensionsToImport, boolean openProjectsAfterImport, boolean deleteFolders) {
		super("Extension Import");
		this.extensionsToImport = extensionsToImport;
		this.openProjectsAfterImport = openProjectsAfterImport;
		this.deleteFolders = deleteFolders;
	}
	
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        Collection<String> existingFolders = checkForExistingFolders(extensionsToImport);
        String extensionsString = Joiner.on(",").join(existingFolders);
        // progress steps
        final int totalSteps = existingFolders.size() + extensionsToImport.size();
        // convert to SubMonitor and set total number of work units
        SubMonitor subMonitor = SubMonitor.convert(monitor, totalSteps);
        // first part of the job: delete projects from workspace (if requested)
        if (!existingFolders.isEmpty()) {
            if (deleteFolders) {
            	ArrayList<String> extensionsWithDeleteErrors = new ArrayList<String>();
                for (String extension : existingFolders) {
                    try {
                        // sleep a second
                        TimeUnit.SECONDS.sleep(1);

                        // set the name of the current work
                        subMonitor.setTaskName("Delete extension project from workspace: " + extension);

                        // do one task
                        IWorkspace workspace = ResourcesPlugin.getWorkspace();
                        IWorkspaceRoot root = workspace.getRoot();
                        File workspaceFile = root.getLocation().toFile();
                        File extensionFolder = new File(workspaceFile, extension);
                        try {
                            FileUtils.deleteDirectory(extensionFolder);
                        } catch (IOException e) {
                        	extensionsWithDeleteErrors.add(extension);
                        	System.err.println("Extension " + extension + " could not be deleted from workspace");
                            e.printStackTrace();
                        }
                        // reduce total work by 1
                        subMonitor.split(1);
                    } catch (InterruptedException e) {
                    	return Status.CANCEL_STATUS;
                    }
                }
                if (!extensionsWithDeleteErrors.isEmpty()) {
                	// set error message
                    errorMessages.add(String.format(ERROR_MESSAGE_DELETE_FAILED, extensionsWithDeleteErrors));
                    return new Status(Status.ERROR, "unknown", ""); // TODO: Use 3rd argument = message ?
                    // TODO: Try import anyway?
                }
            } else {
            	errorMessages.add(String.format(ERROR_MESSAGE_EXISTING_FOLDER, extensionsString));
                return new Status(Status.ERROR, "unknown", ""); // TODO: Use 3rd argument = message ?
            }
        }
        // second part of the job: import extension projects from git repository
        String reposerver = Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE);
        ProjectFromGitImporter importer = new ProjectFromGitImporter(reposerver, openProjectsAfterImport);
        for (String extension : extensionsToImport) {
            try {
                // sleep a second
                TimeUnit.SECONDS.sleep(1);

                // set the name of the current work
                subMonitor.setTaskName("Import extension " + extension);

                // do one task
                importer.importProject(extension);
                // reduce total work by 1
                subMonitor.split(1);
            } catch (InterruptedException | CoreException e) {
            	return Status.CANCEL_STATUS;
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Check if a folder with an extension name already exists in workspace
     *
     * @param extensionsToImport
     * @return
     */
    private Collection<String> checkForExistingFolders(Collection<String> extensionsToImport) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
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

    /**
     * @return errors that occurred during job execution
     */
	public List<String> getErrorMessages() {
		return errorMessages;
	}
}
