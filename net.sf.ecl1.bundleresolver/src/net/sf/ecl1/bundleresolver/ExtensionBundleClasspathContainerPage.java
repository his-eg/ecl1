/**
 * 
 */
package net.sf.ecl1.bundleresolver;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Page for the configuration of an extension bundle classpath container 
 * 
 * @author keunecke
 */
public class ExtensionBundleClasspathContainerPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

    private IJavaProject project;

    private Text directoryText;

    /**
     * Default constructor
     */
    public ExtensionBundleClasspathContainerPage() {
        super(Constants.CONTAINER_PAGE_NAME);
    }

    @Override
    public void initialize(IJavaProject arg0, IClasspathEntry[] arg1) {
        this.project = arg0;
    }

    @Override
    public boolean finish() {
        // check if the directory stored in directoryText exists
        // return true if it is present
        String directory = directoryText.getText();
        IPath path = this.project.getPath().append(directory);
        boolean exists = path.toFile().exists();
        if (!exists) {
            setErrorMessage(String.format(Constants.PAGE_ERROR_MESSAGE_TEMPLATE, directory, project.getElementName()));
        }
        return exists;
    }

    @Override
    public IClasspathEntry getSelection() {
        // TODO
        return null;
    }

    @Override
    public void setSelection(IClasspathEntry arg0) {
        this.directoryText.setText(arg0.getPath().removeFirstSegments(2).toString());
    }

    @Override
    public void createControl(Composite parent) {
        Composite extSelectionGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        extSelectionGroup.setLayout(layout);
        extSelectionGroup.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        new Label(extSelectionGroup, SWT.NONE).setText(Constants.DIRECTORY_LABEL);
        directoryText = new Text(extSelectionGroup, SWT.BORDER);
        directoryText.setText(Constants.DEFAULT_BUNDLE_DIRECTORY);
        setControl(extSelectionGroup);
    }

}
