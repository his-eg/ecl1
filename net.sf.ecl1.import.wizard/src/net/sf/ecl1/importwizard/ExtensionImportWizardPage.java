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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import de.his.cs.sys.extensions.wizards.utils.HISConstants;
import de.his.cs.sys.extensions.wizards.utils.RemoteProjectSearchSupport;
import de.his.cs.sys.extensions.wizards.utils.WorkspaceSupport;


public class ExtensionImportWizardPage extends WizardPage {

    // All extensions existing on repo server
    private final Set<String> remoteExtensions = new TreeSet<>();

    // All extensions existing in workspace
    private final Set<String> extensionsInWorkspace = new TreeSet<>();

    private List projectList;

    private Button openAfterImport;

    protected ExtensionImportWizardPage(String pageName) {
        super(pageName);
    }

    @Override
    public void createControl(Composite parent) {
        initRemoteExtensions();
        initExtensionsInWorkspace();
        GridLayout gl = new GridLayout(1, false);
        GridLayout gl2 = new GridLayout(2, false);

        parent.setLayout(gl);

        Composite openAfterImportComposite = new Composite(parent, SWT.BORDER | SWT.TOP);
        openAfterImportComposite.setLayout(gl2);
        Label question = new Label(openAfterImportComposite, SWT.TOP);
        question.setText("Open extensions after import?");
        openAfterImport = new Button(openAfterImportComposite, SWT.CHECK);
        openAfterImport.setSelection(true);

        Composite projectChoice = new Composite(parent, SWT.BORDER | SWT.TOP);
        projectChoice.setLayout(gl);

        Label infoText = new Label(projectChoice, SWT.TOP | SWT.LEFT | SWT.BORDER);
        infoText.setText("You can use Ctrl + A to select all extensions.");

        Label projectChoiceLabel = new Label(projectChoice, SWT.TOP);
        projectChoiceLabel.setText("Importable Projects");
        projectList = new List(projectChoice, SWT.MULTI | SWT.V_SCROLL);
        for (String remoteExtensionName : remoteExtensions) {
            if (!extensionsInWorkspace.contains(remoteExtensionName) && !remoteExtensionName.contains(HISConstants.WEBAPPS)) {
                projectList.add(remoteExtensionName);
            }
        }

        setControl(parent);
    }

    private void initRemoteExtensions() {
        remoteExtensions.addAll(new RemoteProjectSearchSupport().getProjects());
    }

    private void initExtensionsInWorkspace() {
        extensionsInWorkspace.addAll(new WorkspaceSupport().getPossibleProjectsToReference());
    }

    public Set<String> getSelectedExtensions() {
        return new TreeSet<String>(Arrays.asList(projectList.getSelection()));
    }

    public boolean openProjectsAfterImport() {
        return openAfterImport.getSelection();
    }

}
