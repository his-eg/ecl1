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

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

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
        // register job to be started immediately as another thread
    	job.schedule();
    	// TODO: since we do not know when the job finishes we can not evaluate it's return status here, only return true?
        return true;
    }
}
