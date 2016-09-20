/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.sf.ecl1.importwizard;

import h1modules.utilities.utils.Activator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Wizard for import of extension projects listed on a jenkins.
 *
 * @author keunecke
 */
public class ExtensionImportWizard extends Wizard implements IImportWizard {

	private static final String WINDOW_TITLE = "Extension Import Wizard";

    private static final String ERROR_MESSAGE_EXISTING_FOLDER = "Workspace contains folders named like extensions you want to import. Delete them first: %s";
    private static final String ERROR_MESSAGE_DELETE_FAILED = "Some extensions in your workspace could not be deleted: %s";

    /** Data used throughout the extension import wizard. */
    ExtensionImportWizardModel model;
    
    ExtensionImportWizardPage1_Selection page1;
    ExtensionImportWizardPage2_Confirmation page2;

    public ExtensionImportWizard() {
        super();
        model = new ExtensionImportWizardModel();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(WINDOW_TITLE);
        setNeedsProgressMonitor(true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();
        page1 = new ExtensionImportWizardPage1_Selection(model);
        addPage(page1);
        page2 = new ExtensionImportWizardPage2_Confirmation(model);
        addPage(page2);
        //System.out.println("pageCount = " + this.getPageCount());
    }

    /**
     * Enable the "finish"-button only if page 2 is the current wizard page.
     * @return true if the "finish"-button shall be enabled
     */
    // Implementation note: This is a better solution than using the super implementation and
    // calling setPageComplete() to control the "finish"-button.
    @Override
    public boolean canFinish() {
    	IWizardPage currentPage = this.getContainer().getCurrentPage();
    	boolean canFinish = (currentPage instanceof ExtensionImportWizardPage2_Confirmation);
    	System.out.println("currentPage = " + currentPage.getName() + ", canFinish = " + canFinish);
    	return canFinish;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        Collection<String> extensionsToImport = new HashSet<String>(model.getSelectedExtensions()); // copy
        extensionsToImport.addAll(model.getDeepDependencyExtensions());
        boolean openProjectsAfterImport = page2.openProjectsAfterImport();
        Collection<String> existingFolders = checkForExistingFolders(extensionsToImport);
        String extensionsString = Joiner.on(",").join(existingFolders);

        if (!existingFolders.isEmpty()) {
            if (page2.deleteFolders()) {
            	ArrayList<String> extensionsWithDeleteErrors = new ArrayList<String>();
                for (String extension : existingFolders) {
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
                }
                if (!extensionsWithDeleteErrors.isEmpty()) {
                	// set error message
                    page2.setErrorMessage(String.format(ERROR_MESSAGE_DELETE_FAILED, extensionsWithDeleteErrors));
                    return false; // TODO: Try import anyway?
                }
            } else {
            	page2.setErrorMessage(String.format(ERROR_MESSAGE_EXISTING_FOLDER, extensionsString));
                return false;
            }
        }

        try {
            String reposerver = Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE);
            new ProjectFromGitImporter(reposerver, openProjectsAfterImport).importProjects(extensionsToImport);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return true;
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
}
