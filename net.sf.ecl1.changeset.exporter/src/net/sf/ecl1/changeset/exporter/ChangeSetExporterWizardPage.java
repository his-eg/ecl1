package net.sf.ecl1.changeset.exporter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.internal.core.subscribers.ChangeSet;

import com.google.common.collect.Maps;

public class ChangeSetExporterWizardPage extends WizardPage {

    private Table changeSetTable;

    private Collection<ChangeSet> changes = ChangeSetExportWizardPlugin.getDefault().getChangeSets();

    private Map<String, ChangeSet> itemToChangeMap = Maps.newHashMap();

    private TableItem selectedChangeTableItem;

    private StringFieldEditor hotfixTitle;

    private StringFieldEditor hotfixDescribtion;

    private StringFieldEditor hiszillaTickets;

    private StringFieldEditor hotfixSnippetTextEditor;

    public ChangeSetExporterWizardPage() {
        super("Change Set Exporter Wizard");
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageComposite = new Composite(parent, SWT.NONE);
        GridLayout oneColumnGrid = new GridLayout(1, false);
        pageComposite.setLayout(oneColumnGrid);

        hotfixTitle = new StringFieldEditor("title", "Title", pageComposite);
        hotfixDescribtion = new StringFieldEditor("description", "Description", pageComposite);
        hiszillaTickets = new StringFieldEditor("hiszilla", "Hiszilla", pageComposite);

        Label tableLabel = new Label(pageComposite, SWT.LEFT);
        tableLabel.setText("Change Sets");
        changeSetTable = new Table(pageComposite, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.SINGLE | SWT.FILL);
        changeSetTable.setLinesVisible(true);
        changeSetTable.setHeaderVisible(true);
        changeSetTable.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEvent(e);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleEvent(e);
            }
            /**
             * @param e
             */
            private void handleEvent(SelectionEvent e) {
                TableItem tableItem = (TableItem) e.item;
                selectedChangeTableItem = tableItem;
                setHotfixInformation(e);
            }
        });

        String[] headers = { "Name", "Comment" };
        for (String header : headers) {
            TableColumn c = new TableColumn(changeSetTable, SWT.NONE);
            c.setText(header);
            c.setWidth(400);
        }

        boolean containsEmptyChangeSets = false;
        for (ChangeSet change : changes) {
            if (change != null) {
                TableItem tableItem = new TableItem(changeSetTable, SWT.NONE);
                tableItem.setText(0, change.getName());
                tableItem.setText(1, change.getComment());
                itemToChangeMap.put(change.getName(), change);
                if (change.isEmpty()) {
                    containsEmptyChangeSets = true;
                }
            }
        }

        if(containsEmptyChangeSets) {
            setErrorMessage("There were empty change sets! Did you run CVS synchronization?");
        }

        for (int i = 0; i < headers.length; i++) {
            changeSetTable.getColumn(i).pack();
        }

        this.hotfixSnippetTextEditor = new StringFieldEditor("snippet", "Hotfix Snippet", pageComposite);

        setControl(pageComposite);
    }

    protected void setHotfixInformation(SelectionEvent e) {
        String hotfixSnippet = getHotfixDefinition().toXml();
        // add content to clipboard
        final Display display = getControl().getDisplay();
        final Clipboard cb = new Clipboard(display);
        TextTransfer textTransfer = TextTransfer.getInstance();
        cb.setContents(new Object[] { hotfixSnippet }, new Transfer[] { textTransfer });
        this.hotfixSnippetTextEditor.setStringValue(hotfixSnippet);
        setMessage("Hotfix XML snippet copied to clipboard!");
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                display.timerExec(3000, new Runnable() {
                    @Override
                    public void run() {
                        setMessage("");
                    }
                });
            }
        });
    }

    HotfixInformation getHotfixDefinition() {
        String title = hotfixTitle.getStringValue();
        String description = hotfixDescribtion.getStringValue();
        String hiszilla = hiszillaTickets.getStringValue();
        HotfixInformation hf = new HotfixInformation(title, description, hiszilla);
        ChangeSet selectedChangeSet = itemToChangeMap.get(selectedChangeTableItem.getText(1));
        if (selectedChangeSet != null) {
            List<IResource> resources = Arrays.asList(selectedChangeSet.getResources());
            for (IResource changedResource : resources) {
                IPath qisserver = new Path("qisserver/");
                IPath changedResourceProjectRelativePath = changedResource.getProjectRelativePath();
                // only resources from within qisserver are considered
                IPath path = changedResourceProjectRelativePath.makeRelativeTo(qisserver);
                String name = path.toString();
                if (qisserver.isPrefixOf(changedResourceProjectRelativePath)) {
                    hf.addFile(name);
                } else {
                    System.out.println("Ignored '" + name + "' for hotfix snippet.");
                }
            }
        }
        return hf;
    }

}
