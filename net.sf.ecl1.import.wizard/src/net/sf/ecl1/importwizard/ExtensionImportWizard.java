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
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

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
    
    /**
     * Finish the extension import.
     * This is implemented such that progress monitoring is available.
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
    	// variables to be used in the Job class implementation must be final
        Collection<String> extensionsToImport = new HashSet<String>(model.getSelectedExtensions()); // copy
        extensionsToImport.addAll(model.getDeepDependencyExtensions());
        boolean openProjectsAfterImport = page2.openProjectsAfterImport();
        boolean deleteFolders = page2.deleteFolders();

        ExtensionImportJob job = new ExtensionImportJob(extensionsToImport, openProjectsAfterImport, deleteFolders);
    	job.schedule();
    	List<String> errorMessages = job.getErrorMessages();
    	// TODO: How to display error messages delivered by the job? Before we used page2.setErrorMessage(String)
    	// TODO: Can we get the Status return by Job.run() ?
        return errorMessages.isEmpty(); // no errors means success 
    }
}
