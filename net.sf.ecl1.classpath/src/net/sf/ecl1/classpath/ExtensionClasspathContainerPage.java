package net.sf.ecl1.classpath;

import java.util.Collection;
import java.util.List;

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import net.sf.ecl1.utilities.hisinone.ExtensionUtil;

/**
 * The ecl1 classpath container edit page.
 * @author keunecke
 */
public class ExtensionClasspathContainerPage extends WizardPage implements IClasspathContainerPage {

    /**
     * ClasspathContainer ID
     */
    private static final String NET_SF_ECL1_ECL1_CONTAINER_ID = "net.sf.ecl1.ECL1_CONTAINER";

    private static final String ECL1_CLASSPATH_CONTAINER = "ecl1 Classpath Container";

    private static final ExtensionUtil EXTENSION_UTIL = ExtensionUtil.getInstance();

    private IPath selection;

    private Table extensionsTable;

    /**
     * Default Constructor - sets title, page name, description
     */
    public ExtensionClasspathContainerPage() {
        super(ECL1_CLASSPATH_CONTAINER, ECL1_CLASSPATH_CONTAINER , null);
        setDescription(ECL1_CLASSPATH_CONTAINER);
        setPageComplete(true);
        this.selection = new Path(NET_SF_ECL1_ECL1_CONTAINER_ID);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

        composite.setLayout(new GridLayout(5, false));
        composite.setFont(parent.getFont());
        Label label = new Label(composite, SWT.LEFT);
        label.setText("Available Extensions");

        extensionsTable = new Table(composite, SWT.MULTI | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        extensionsTable.setLinesVisible(true);
        extensionsTable.setHeaderVisible(true);
        extensionsTable.setLayoutData(layoutData);
        extensionsTable.setSize(200, 600);

        String[] headers = { "Export?", "Name" };
        for (String header : headers) {
            TableColumn c = new TableColumn(extensionsTable, SWT.NONE);
            c.setText(header);
        }

        // before searching extensions, we search for webapps dynamically just in case it has been created after startup
    	EXTENSION_UTIL.findWebappsProject();
    	// search all extensions eligible to be added to the classpath container
        Collection<String> extensionNames = EXTENSION_UTIL.findAllExtensions().keySet();
        for (String extensionName : extensionNames) {
            TableItem tableItem = new TableItem(extensionsTable, SWT.NONE);
            tableItem.setText(1, extensionName);
        }

        for (int i = 0; i < headers.length; i++) {
            extensionsTable.getColumn(i).pack();
        }

        updateExtensionsTextfieldFromPath();
        setControl(composite);
    }

    @Override
    public boolean finish() {
        return true;
    }

    @Override
    public IClasspathEntry getSelection() {
        IPath path = new Path(NET_SF_ECL1_ECL1_CONTAINER_ID);
        Collection<String> extensionsSelected = getSelectedExtensions();
        if (!extensionsSelected.isEmpty()) {
            path = path.append(Joiner.on(",").join(extensionsSelected));
        }
        return JavaCore.newContainerEntry(path, true);
    }

    private Collection<String> getSelectedExtensions() {
        Collection<String> result = Sets.newHashSet();
        TableItem[] items = extensionsTable.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                result.add(item.getText(1));
            }
        }
        return result;
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
        if(extensionsTable != null && segments != null && segments.length > 1) {
            List<String> split = Splitter.on(",").splitToList(segments[1]);
            TableItem[] items = extensionsTable.getItems();
            for (TableItem item : items) {
                if (split.contains(item.getText(1))) {
                    item.setChecked(true);
                }
            }
        }
    }

}
