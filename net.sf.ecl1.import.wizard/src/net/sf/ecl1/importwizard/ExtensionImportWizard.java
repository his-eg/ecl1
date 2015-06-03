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

import java.util.Collection;

import net.sf.ecl1.utilities.preferences.ExtensionToolsPreferenceConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard for import of extension projects listed on a jenkins
 *
 * @author keunecke
 */
public class ExtensionImportWizard extends Wizard implements IImportWizard {

    ExtensionImportWizardPage mainPage;

    public ExtensionImportWizard() {
        super();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        Collection<String> extensionsToImport = mainPage.getSelectedExtensions();
        boolean openProjectsAfterImport = mainPage.openProjectsAfterImport();

        try {
            String reposerver = Activator.getDefault().getPreferenceStore().getString(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE);
            new ProjectFromGitImporter(reposerver, openProjectsAfterImport).importProjects(extensionsToImport);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Extension Import Wizard"); //NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ExtensionImportWizardPage("Extension Import"); //NON-NLS-1
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        super.addPages();
        mainPage.setDescription("Extension Import Wizard");
        addPage(mainPage);
    }

}
