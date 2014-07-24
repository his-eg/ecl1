/**
 * 
 */
package net.sf.ecl1.classpath;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * @author keunecke
 *
 */
public class ExtensionClasspathContainerPage extends WizardPage 
implements IClasspathContainerPage {
	
	private IClasspathEntry selection;
	
	private static final String ECL1_CLASSPATH_CONTAINER = "ecl1 Classpath Container";

	/**
     * Default Constructor - sets title, page name, description
     */
    public ExtensionClasspathContainerPage() {
        super(ECL1_CLASSPATH_CONTAINER, ECL1_CLASSPATH_CONTAINER , null);
        setDescription(ECL1_CLASSPATH_CONTAINER);
        setPageComplete(true);
        this.selection = JavaCore.newContainerEntry(new Path(ExtensionClassPathContainer.NET_SF_ECL1_ECL1_CONTAINER_ID));
    }

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());
        Text text = new Text(composite, SWT.NONE);
        text.setText(ECL1_CLASSPATH_CONTAINER);
        setControl(composite);
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public IClasspathEntry getSelection() {
		return this.selection;
	}

	@Override
	public void setSelection(IClasspathEntry arg0) {
		this.selection = JavaCore.newContainerEntry(new Path(ExtensionClassPathContainer.NET_SF_ECL1_ECL1_CONTAINER_ID));
	}

}
