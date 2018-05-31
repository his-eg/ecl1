package net.sf.ecl1.changeset.exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.ReleaseXmlUtil;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.TableLayout;
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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.internal.core.subscribers.ChangeSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.junit.Assert;

public class ChangeSetExporterWizardPage extends WizardPage {

	private static final ConsoleLogger logger = new ConsoleLogger(ChangeSetExportWizardPlugin.getDefault().getLog(), ChangeSetExportWizardPlugin.PLUGIN_ID);

    private Table changeSetTable;

    private Collection<ChangeSet> changes = ChangeSetExportWizardPlugin.getDefault().getChangeSets();

    private Map<String, ChangeSet> itemToChangeMap = Maps.newHashMap();

    private TableItem selectedChangeTableItem;

    private StringFieldEditor hotfixTitle;

    private StringFieldEditor hotfixDescription;

    private StringFieldEditor hiszillaTickets;

    private BooleanFieldEditor dbUpdateRequired;

    private StringFieldEditor hotfixSnippetTextEditor;

    private ArrayList<String> hotfixFileNames = null;
    
    private List releaseFilesList; // see outcommented code
    
    private SimplePropertyChangeListener propertyChangeListener;
    
    public ChangeSetExporterWizardPage() {
        super("Change Set Exporter Wizard");
        this.setTitle("Describe the hotfix and select a change set");
        propertyChangeListener = new SimplePropertyChangeListener(this);
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageComposite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, true);
        pageComposite.setLayout(gridLayout);
        
        hotfixTitle = new StringFieldEditor("title", "Title", pageComposite);
        // Get version from release.xml incremented by 1 for next hotfix
        String version = ReleaseXmlUtil.getIncrementedReleaseXmlVersionShortString();
        logger.debug("ChangesetExporter: version from release.xml incremented by 1 = " + version);;
        hotfixTitle.setStringValue("Hotfix " + version);
        hotfixTitle.setPropertyChangeListener(propertyChangeListener);

        hotfixDescription = new StringFieldEditor("description", "Description", pageComposite);
        hotfixDescription.setPropertyChangeListener(propertyChangeListener);
        
        hiszillaTickets = new StringFieldEditor("hiszilla", "Hiszilla", pageComposite);
        hiszillaTickets.setPropertyChangeListener(propertyChangeListener);

        // Explicit label and BooleanFieldEditor required to fix layout
        Label dbUpdateLabel = new Label(pageComposite, SWT.LEFT);
        dbUpdateLabel.setText("Database update required?");
        dbUpdateRequired = new BooleanFieldEditor("dbUpdate", "", pageComposite);
        dbUpdateRequired.fillIntoGrid(pageComposite, 1);
        dbUpdateRequired.setPropertyChangeListener(propertyChangeListener);

        Label tableLabel = new Label(pageComposite, SWT.LEFT);
        tableLabel.setText("Change Sets");
        changeSetTable = new Table(pageComposite, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.SINGLE | SWT.FILL);
        changeSetTable.setLinesVisible(true);
        changeSetTable.setHeaderVisible(true);
//        TableLayout tableLayout = new TableLayout();
//        tableLayout.computeSize(pageComposite, 200, 40, true);
//        changeSetTable.setLayout(new TableLayout()); // TODO
//        changeSetTable.computeSize(pageComposite, 200, 40, true);
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
            	// clear previous messages
                setMessage(null);
                setErrorMessage(null);
                // get files from changeSet
                checkSetHotfixFileNames();
                // if there was no error yet, check all the other required informations
                // and create hotfix snippet if everything is ok
                if (getErrorMessage() == null) {
                	checkSetHotfixInformation();
                }
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

        //Re-enable if adding snippet to release.xml is needed
        //Label releaseXmlListLabel = new Label(pageComposite, SWT.LEFT);
        //releaseXmlListLabel.setText("Release XMLs");
        //releaseFilesList = new List(pageComposite, SWT.BORDER | SWT.V_SCROLL);
        //Collection<IFile> releaseXmlFiles = ReleaseXmlUtil.getReleaseXmlFiles();
        //for (IFile releaseXml : releaseXmlFiles) {
        //    releaseFilesList.add(releaseXml.getName());
        //}

        this.hotfixSnippetTextEditor = new StringFieldEditor("snippet", "Hotfix Snippet", pageComposite);

        setControl(pageComposite);
    }

    protected void checkSetHotfixInformation() {
        if (!hasDescription()) {
        	setLogError("No description for hotfix provided!");
            return;
        }
        if (!hasHiszilla()) {
        	setLogError("No hiszilla tickets for hotfix provided!");
            return;
        }
        if (!hasSelectedChangeSet()) {
        	setLogError("No Change Set selected!");
            return;
        }
    	if (hotfixFileNames==null || hotfixFileNames.size()==0) {
            setLogError("Selected ChangeSet contains no files...");
            return;
    	}
    	
    	// looks good -> create hotfix snippet
        String title = hotfixTitle.getStringValue();
        String description = hotfixDescription.getStringValue();
        String hiszilla = hiszillaTickets.getStringValue();
        boolean isDbUpdateRequired = dbUpdateRequired.getBooleanValue();
        String hotfixSnippet = new HotfixInformation(title, description, hiszilla, isDbUpdateRequired, hotfixFileNames).toXml();
        logger.debug("Created hotfix snippet:\n" + hotfixSnippet);
        
        // add content to clipboard
        final Display display = getControl().getDisplay();
        final Clipboard cb = new Clipboard(display);
        TextTransfer textTransfer = TextTransfer.getInstance();
        cb.setContents(new Object[] { hotfixSnippet }, new Transfer[] { textTransfer });
        this.hotfixSnippetTextEditor.setStringValue(hotfixSnippet);
        setLogInfo("Hotfix XML snippet copied to clipboard!");
    }

    private void checkSetHotfixFileNames() {
    	this.hotfixFileNames = new ArrayList<>();
        Assert.assertNotNull(selectedChangeTableItem); // verified by ChangeSetExportWizard.finish()
        ChangeSet selectedChangeSet = itemToChangeMap.get(selectedChangeTableItem.getText(1));
        java.util.List<String> ignored = Lists.newLinkedList();
        if (selectedChangeSet != null) {
            java.util.List<IResource> resources = Arrays.asList(selectedChangeSet.getResources());
            if (resources != null) {
            	if (resources.size() > 0) {
	                for (IResource changedResource : resources) {
	                    IPath qisserver = new Path("qisserver/");
	                    IPath changedResourceProjectRelativePath = changedResource.getProjectRelativePath();
	                    // only resources from within qisserver are considered
	                    IPath path = changedResourceProjectRelativePath.makeRelativeTo(qisserver);
	                    String name = path.toString();
	                    if (qisserver.isPrefixOf(changedResourceProjectRelativePath)) {
	                    	hotfixFileNames.add(name);
	                    } else {
	                        ignored.add(name);
	                    }
	                }
            	} else {
            		logger.warn("resources is empty");
            	}
            } else {
            	logger.warn("resources is null");
            }
        } else {
        	logger.warn("selectedChangeSet is null"); // should not happen?
        }
        boolean filesIgnored = !ignored.isEmpty();
        int foundFilesCount = hotfixFileNames.size();
        if (foundFilesCount == 0) {
        	if (filesIgnored) {
            	setLogError("The selected changeset contains only files outside qisserver!");
        	} else {
            	setLogError("The selected changeset contains no files!");
        	}
        } else if (filesIgnored) {
        	setLogError("Skipped files outside qisserver in selected change set!");
        } else {
        	// delete previous error messages
            logger.debug("ChangeSet contains " + foundFilesCount + " files.");
        }
    }

    /**
     * @return true iff user has selected a change set
     */
    boolean hasSelectedChangeSet() {
        return selectedChangeTableItem != null;
    }

    /**
     * @return true iff user has entered a description
     */
    boolean hasDescription() {
        String desc = hotfixDescription.getStringValue();
        return  desc != null && !desc.isEmpty();
    }

    /**
     * @return true iff user has entered ticket numbers
     */
    boolean hasHiszilla() {
        String hiszilla = hiszillaTickets.getStringValue();
        return hiszilla != null && !hiszilla.isEmpty();
    }
    
    private void setLogInfo(String message) {
    	logger.info(message);
        setMessage(message);
    }

    private void setLogError(String message) {
    	// log debug instead error message to avoid spamming the "Error Log" view
    	logger.debug(message);
    	// message displayed in dialog window
        setErrorMessage(message);
        // delete eventually old hotfix snippet
        hotfixSnippetTextEditor.setStringValue(null);
    }
    
    /**
     * @param duration duration till removal in ms
     */
    void clearErrorAsync(final int duration) {
        final Display display = getControl().getDisplay();
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                display.timerExec(duration, new Runnable() {
                    @Override
                    public void run() {
                        setErrorMessage(null);
                    }
                });
            }
        });
    }
}
