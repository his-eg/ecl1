/**
 *
 */
package net.sf.ecl1.classpath;

import static net.sf.ecl1.classpath.ClasspathContainerConstants.NET_SF_ECL1_ECL1_CONTAINER_ID;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * @author keunecke
 *
 */
public class ExtensionClasspathContainerPage extends WizardPage
implements IClasspathContainerPage {

    private IPath selection;

    private static final String ECL1_CLASSPATH_CONTAINER = "ecl1 Classpath Container";

    private Text extensionsTextList;

    /**
     * Default Constructor - sets title, page name, description
     */
    public ExtensionClasspathContainerPage() {
        super(ECL1_CLASSPATH_CONTAINER, ECL1_CLASSPATH_CONTAINER , null);
        setDescription(ECL1_CLASSPATH_CONTAINER);
        setPageComplete(true);
        this.selection = new Path(ClasspathContainerConstants.NET_SF_ECL1_ECL1_CONTAINER_ID);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout(5, false));
        composite.setFont(parent.getFont());
        Label label = new Label(composite, SWT.LEFT);
        label.setText("Available Extensions");
        extensionsTextList = new Text(composite, SWT.FILL );
        extensionsTextList.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));
        updateExtensionsTextfieldFromPath();
        setControl(composite);
    }

    @Override
    public boolean finish() {
        if (extensionsTextList.getText().isEmpty()) {
            setErrorMessage("Extensions to be exported needs to be configured.");
            return false;
        }
        Collection<String> nonExistingExtensionsConfigured = checkForNonExistingExtensions();
        if (!nonExistingExtensionsConfigured.isEmpty()) {
            setErrorMessage("The Extensions " + nonExistingExtensionsConfigured + " do not exist in workspace.");
            return true;
        }
        return true;
    }

    private Collection<String> checkForNonExistingExtensions() {
        Collection<String> result = Lists.newArrayList();
        Iterable<String> configuredExtensions = Splitter.on(",").split(extensionsTextList.getText());
        for (String extension : configuredExtensions) {
            boolean existsAsJar = new ExtensionUtil().doesExtensionJarExist(extension);
            boolean existsAsProject = new ExtensionUtil().doesExtensionProjectExist(extension);
            if (!existsAsJar && !existsAsProject) {
                result.add(extension);
                System.out.println("'" + extension + "' does neither exist as project nor as extension jar.");
            }
        }
        return result;
    }

    @Override
    public IClasspathEntry getSelection() {
        IPath path = new Path(NET_SF_ECL1_ECL1_CONTAINER_ID);
        String extensionsCommaSeparated = extensionsTextList.getText();
        if(extensionsCommaSeparated != null && !extensionsCommaSeparated.isEmpty()) {
            path = path.append(extensionsCommaSeparated);
        }
        return JavaCore.newContainerEntry(path, true);
    }

    @Override
    public void setSelection(IClasspathEntry arg0) {
        if(arg0 != null) {
            this.selection = arg0.getPath();
            updateExtensionsTextfieldFromPath();
        }
    }

    private void updateExtensionsTextfieldFromPath() {
        String[] segments = this.selection.segments();
        if(extensionsTextList != null && segments != null && segments.length > 1) {
            extensionsTextList.setText(segments[1]);
        }
    }

}
