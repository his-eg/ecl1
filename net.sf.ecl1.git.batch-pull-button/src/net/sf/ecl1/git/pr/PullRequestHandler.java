package net.sf.ecl1.git.pr;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.sf.ecl1.git.Activator;

/**
 * Eclipse command handler for creating a Gitlab merge request.
 * Activated by a toolbar button.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Determines the current project/repository from the selection</li>
 *   <li>Reads the Gitlab configuration</li>
 *   <li>Shows a dialog to collect merge request parameters</li>
 *   <li>Runs the merge request creation in a background job</li>
 * </ol>
 */
public class PullRequestHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Determine the project directory from the current selection
        File projectDir = getProjectDirectory(event);
        if (projectDir == null) {
            MessageDialog.openError(shell, "Create Merge Request",
                    "Cannot determine the project directory.\n"
                            + "Please select a project in the Package Explorer or Project Explorer.");
            return null;
        }

        // Read configuration and local repository info
        GitlabConfig config;
        LocalRepository localRepo;
        try {
            config = new GitlabConfig();
            localRepo = new LocalRepository(projectDir);
            config.activateSection(localRepo.getServer());
        } catch (IOException e) {
            MessageDialog.openError(shell, "Create Merge Request",
                    "Error reading configuration or repository:\n" + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            MessageDialog.openError(shell, "Create Merge Request", e.getMessage());
            return null;
        }

        // Validate that the current branch is a dedicated feature branch
        String currentBranch = localRepo.getBranch();
        if (currentBranch == null || java.util.regex.Pattern.compile(config.getBranches()).matcher(currentBranch).find()) {
            MessageDialog.openError(shell, "Create Merge Request",
                    "Please create a dedicated branch for your merge request.\n"
                            + "Current branch is: " + currentBranch);
            return null;
        }

        String detectedTargetBranch = null;
        try {
	        // Auto-detect target branch
	        detectedTargetBranch = localRepo.findTargetBranch(config.getBranches());
        } catch (IOException e) {
            MessageDialog.openError(shell, "Create Merge Request",
                    "Cannot detect target branch for merge request.\n"
                            + "Please check your configuration and repository state.");
            return null;
        }

        String lastCommitMessage = null;
        try {
        	lastCommitMessage = localRepo.getLastCommitMessage();
        } catch (IOException e) {
            MessageDialog.openError(shell, "Create Merge Request",
                    "Cannot detect last commit.\n"
                            + "Please check your configuration and repository state.");
            return null;
        }

        // Show dialog for merge request parameters
        PullRequestDialog dialog = new PullRequestDialog(shell, localRepo.getBranch(), detectedTargetBranch, lastCommitMessage, localRepo.hasLFS());
        if (dialog.open() != Window.OK) {
            return null;
        }

        PullRequestCreator.Params params = dialog.getParams();

        // Run the merge request creation in a background job
        Job job = new Job("ecl1: Creating Gitlab Merge Request") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                PullRequestCreator creator = new PullRequestCreator(config, localRepo, params);
                IStatus status = creator.execute(monitor);

                // Show result in UI thread
                Display.getDefault().asyncExec(() -> {
                    Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    if (status.isOK()) {
                        MessageDialog.openInformation(activeShell, "Create Merge Request",
                                "Merge request created successfully.");
                    } else if (status.getSeverity() == IStatus.ERROR) {
                        MessageDialog.openError(activeShell, "Create Merge Request",
                                status.getMessage());
                    }
                });

                return status;
            }
        };
        job.setUser(true);
        job.schedule();

        return null;
    }

    /**
     * Gets the project directory from the current selection or active editor.
     */
    private File getProjectDirectory(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            Object element = structured.getFirstElement();

            if (element instanceof IResource) {
                IProject project = ((IResource) element).getProject();
                if (project != null && project.getLocation() != null) {
                    return project.getLocation().toFile();
                }
            } else if (element instanceof IProject) {
                IProject project = (IProject) element;
                if (project.getLocation() != null) {
                    return project.getLocation().toFile();
                }
            }
        }

        // Fallback: try to get from active editor
        try {
            org.eclipse.ui.IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null && editor.getEditorInput() != null) {
                IResource resource = editor.getEditorInput().getAdapter(IResource.class);
                if (resource != null && resource.getProject() != null
                        && resource.getProject().getLocation() != null) {
                    return resource.getProject().getLocation().toFile();
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }
}
