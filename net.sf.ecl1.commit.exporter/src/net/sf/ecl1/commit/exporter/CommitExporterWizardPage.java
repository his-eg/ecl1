package net.sf.ecl1.commit.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.junit.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.sf.ecl1.commit.exporter.commitTable.CommitTableFactory;
import net.sf.ecl1.commit.exporter.commitTable.GitUtil;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.hisinone.ReleaseXmlUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

public class CommitExporterWizardPage extends WizardPage {

	private static final ConsoleLogger logger = new ConsoleLogger(CommitExportWizardPlugin.getDefault().getLog(), CommitExportWizardPlugin.PLUGIN_ID, CommitExporterWizardPage.class.getSimpleName());

    private CheckboxTableViewer commitTable;

    private Table changeSetTable;

    private Git git;

    private Collection<ChangeSet> changes = CommitExportWizardPlugin.getDefault().getChangeSets();

    // a map from change set titles to change sets
    private Map<String, ChangeSet> itemToChangeMap = Maps.newHashMap();

    // the selected change set table element
    private TableItem selectedChangeTableItem;

    private StringFieldEditor hotfixTitle;

    private StringFieldEditor hotfixDescription;

    private StringFieldEditor hiszillaTickets;

    private BooleanFieldEditor dbUpdateRequired;

    private Button includeStageChanges;

    private Button checkAllSelected;

    private Button uncheckAllSelected;

    private StringFieldEditor hotfixSnippetTextEditor;

    private ArrayList<String> hotfixFileNames = null;
    
    private List releaseFilesList; // see outcommented code
    
    private SimplePropertyChangeListener propertyChangeListener;
    
    private boolean validate = false;
    
    public CommitExporterWizardPage() {
        super("Commit Exporter Wizard");
        this.setTitle("Describe the hotfix and select commits");
        propertyChangeListener = new SimplePropertyChangeListener(this);
    }

    @Override
    public void createControl(Composite parent) {
        String webappsPath = WebappsUtil.findWebappsProject().getLocation().toString();
        logger.info("Starting search for git-repo at this location: " + webappsPath);
        git = GitUtil.searchGitRepo(webappsPath);
        logger.info("Found git-repo at: " + git.getRepository().getDirectory().toString());


        Composite pageComposite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(2, true);
        pageComposite.setLayout(gridLayout);
        
        hotfixTitle = new StringFieldEditor("title", "Title", pageComposite);
        // Get version from release.xml incremented by 1 for next hotfix
        String version = ReleaseXmlUtil.getIncrementedReleaseXmlVersionShortString();
        logger.debug("CommitExporter: version from release.xml incremented by 1 = " + version);
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
        //dbUpdateRequired.fillIntoGrid(pageComposite, 1);
        dbUpdateRequired.setPropertyChangeListener(propertyChangeListener);

        Label inlcudeStagedChangesLabel = new Label(pageComposite, SWT.LEFT);
        inlcudeStagedChangesLabel.setText("Include Staged Changes in Hotfix?");
        Button includeStageChanges = new Button(pageComposite, SWT.CHECK);
        includeStageChanges.setSelection(true);

        Label tableLabel = new Label(pageComposite, SWT.LEFT);
        tableLabel.setText("Commits");
        
        commitTable = CommitTableFactory.createCommitTable(pageComposite);
        /* ----------------------
         * Set the content provider for the table
         * ----------------------
         */
        //We choose an ArrayContentProvider, because our data is not mutable
        commitTable.setContentProvider(new ArrayContentProvider());
        //TODO: Must later be replaced a something that does not collect all commits, but only when called (lazy)
        List<RevCommit> allCommits = new ArrayList<>();

        try {
            for (RevCommit r : git.log().all().call()) {
                allCommits.add(r);
            }
        } catch (GitAPIException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        commitTable.setInput(allCommits);


        //                changeSetTable = new Table(pageComposite, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.SINGLE | SWT.FILL);
        //        changeSetTable.setLinesVisible(true);
        //        changeSetTable.setHeaderVisible(true);
        //        changeSetTable.addSelectionListener(new SelectionListener() {
        //            @Override
        //            public void widgetSelected(SelectionEvent e) {
        //                handleEvent(e);
        //            }
        //            @Override
        //            public void widgetDefaultSelected(SelectionEvent e) {
        //                handleEvent(e);
        //            }
        //            /**
        //             * @param e
        //             */
        //            private void handleEvent(SelectionEvent event) {
        //                TableItem tableItem = (TableItem) event.item;
        //                selectedChangeTableItem = tableItem;
        //            	// clear previous messages
        //                setMessage(null);
        //                setErrorMessage(null);
        //                // get files from changeSet
        //                try {
        //                	checkSetHotfixFileNames();
        //                } catch (Exception e) {
        //                	// program errors shall be written to the Error Log view, too
        //                	String msg = "Reading file names from the selected Change Set failed! ";
        //                	logger.error2(msg + e.getMessage(), e);
        //                    setErrorMessage(msg + "See the console logs for details.");
        //                    hotfixSnippetTextEditor.setStringValue(null);
        //                }
        //                // if there was no error yet, check all the other required informations
        //                // and create hotfix snippet if everything is ok
        //                if (getErrorMessage() == null) {
        //                	checkSetHotfixInformation();
        //                }
        //            }
        //        });
        //
        //        String[] headers = { "Name", "Comment" };
        //        for (String header : headers) {
        //            TableColumn c = new TableColumn(changeSetTable, SWT.NONE);
        //            c.setText(header);
        //            c.setWidth(400);
        //        }
        //
        //        boolean containsEmptyChangeSets = false;
        //        for (ChangeSet change : changes) {
        //            if (change != null) {
        //                TableItem tableItem = new TableItem(changeSetTable, SWT.NONE);
        //                tableItem.setText(0, change.getName());
        //                tableItem.setText(1, change.getComment());
        //                itemToChangeMap.put(change.getName(), change);
        //                if (change.isEmpty()) {
        //                    containsEmptyChangeSets = true;
        //                }
        //            }
        //        }
        //
        //        if(containsEmptyChangeSets) {
        //            setLogError("There were empty change sets! Did you run CVS synchronization?");
        //        }
        //
        //        for (int i = 0; i < headers.length; i++) {
        //            changeSetTable.getColumn(i).pack();
        //        }

        //Re-enable if adding snippet to release.xml is needed
        //Label releaseXmlListLabel = new Label(pageComposite, SWT.LEFT);
        //releaseXmlListLabel.setText("Release XMLs");
        //releaseFilesList = new List(pageComposite, SWT.BORDER | SWT.V_SCROLL);
        //Collection<IFile> releaseXmlFiles = ReleaseXmlUtil.getReleaseXmlFiles();
        //for (IFile releaseXml : releaseXmlFiles) {
        //    releaseFilesList.add(releaseXml.getName());
        //}

        new Label(pageComposite, SWT.NONE); //Needed to correctly align the following elements in the layout
        Composite processSelectButtonsComp = new Composite(pageComposite, SWT.LEFT);
        RowLayout rl_processSelectButtonsComp = new RowLayout();
        rl_processSelectButtonsComp.marginLeft = 0;
        processSelectButtonsComp.setLayout(rl_processSelectButtonsComp);
        checkAllSelected = new Button(processSelectButtonsComp, SWT.PUSH);
        checkAllSelected.setText("Check all selected rows");
        checkAllSelected.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selectedRows = commitTable.getStructuredSelection();
                for (Object o : selectedRows.toList()) {
                    commitTable.setChecked(o, true);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub

            }
        });

        uncheckAllSelected = new Button(processSelectButtonsComp, SWT.PUSH);
        uncheckAllSelected.setText("Uncheck all selected rows");
        uncheckAllSelected.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selectedRows = commitTable.getStructuredSelection();
                for (Object o : selectedRows.toList()) {
                    commitTable.setChecked(o, false);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub

            }
        });

        this.hotfixSnippetTextEditor = new StringFieldEditor("snippet", "Hotfix Snippet", pageComposite);

        setControl(pageComposite);
    }

    void checkSetHotfixInformation() {
    	checkSetValidationRequired();
    	
        if (!hasTitle()) {
        	if (validate) setLogError("No hotfix title provided!");
            return;
        }
        if (!hasDescription()) {
        	if (validate) setLogError("No description for hotfix provided!");
            return;
        }
        if (!hasHiszilla()) {
        	if (validate) setLogError("No hiszilla tickets for hotfix provided!");
            return;
        }
        if (!hasSelectedChangeSet()) {
        	if (validate) setLogError("No ChangeSet selected!");
            return;
        }
    	if (hotfixFileNames==null || hotfixFileNames.size()==0) {
    		if (validate) setLogError("The selected ChangeSet contains no files!");
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
    	// Validation is always on when reading files from Change Set...
    	
    	// Get the change set corresponding to the selected table item:
        // * itemToChangeMap is a map from titles to change sets
        // * The change set title is given by selectedChangeTableItem.getText() or getText(0), the comment by getText(1)
        Assert.assertNotNull(selectedChangeTableItem); // verified by ChangeSetExportWizard.finish()
        logger.debug("selectedChangeTableItem = " + selectedChangeTableItem);
        String changeSetTitle = selectedChangeTableItem.getText();
        logger.debug("changeSetTitle = " + changeSetTitle);
        logger.debug("itemToChangeMap = " + itemToChangeMap);
        ChangeSet selectedChangeSet = itemToChangeMap.get(changeSetTitle);
        
        // Check all files
    	this.hotfixFileNames = new ArrayList<>();
        java.util.List<String> ignored = Lists.newLinkedList();
        if (selectedChangeSet != null) {
            java.util.List<IResource> resources = Arrays.asList(selectedChangeSet.getResources());
            if (resources != null) {
            	if (resources.size() > 0) {
	                for (IResource changedResource : resources) {
	                    IPath qisserver = new Path("qisserver/");
	                    IPath changedResourceProjectRelativePath = changedResource.getProjectRelativePath();
	                    // only resources inside qisserver are considered
	                    IPath path = changedResourceProjectRelativePath.makeRelativeTo(qisserver);
	                    String name = path.toString();
	                    if (qisserver.isPrefixOf(changedResourceProjectRelativePath)) {
	                    	hotfixFileNames.add(name);
	                    } else {
	                        ignored.add(name);
	                    }
	                }
            	} else {
            		logger.debug("resources is empty");
            	}
            } else {
            	logger.debug("resources is null");
            }
        } else {
        	logger.debug("selectedChangeSet is null");
        }
        boolean filesIgnored = !ignored.isEmpty();
        int foundFilesCount = hotfixFileNames.size();
        if (foundFilesCount == 0) {
        	if (filesIgnored) {
            	setLogError("The selected ChangeSet contains only files outside qisserver!");
        	} else {
            	setLogError("The selected ChangeSet contains no files!");
        	}
        } else if (filesIgnored) {
        	setLogError("Skipped files outside qisserver in selected ChangeSet!");
        } else {
        	// delete previous error messages
            logger.debug("ChangeSet contains " + foundFilesCount + " files.");
        }
    }

    /**
     * @return true iff user has entered a hotfix name
     */
    private boolean hasTitle() {
        String title = hotfixTitle.getStringValue();
        return  title != null && !title.isEmpty();
    }

    /**
     * @return true iff user has entered a description
     */
    private boolean hasDescription() {
        String desc = hotfixDescription.getStringValue();
        return  desc != null && !desc.isEmpty();
    }

    /**
     * @return true iff user has entered ticket numbers
     */
    private boolean hasHiszilla() {
        String hiszilla = hiszillaTickets.getStringValue();
        return hiszilla != null && !hiszilla.isEmpty();
    }

    /**
     * @return true iff user has selected a change set
     */
    private boolean hasSelectedChangeSet() {
        return selectedChangeTableItem != null;
    }

    /**
     * Turn on validation when the finish button was clicked.
     */
    void setValidationRequired() {
    	validate = true;
    }

    /**
     * Turn on validation when all data was provided
     */
    private void checkSetValidationRequired() {
    	if (!validate) {
    		validate = hasTitle() && hasDescription() && hasHiszilla() && hasSelectedChangeSet();
    	}
    }

    private void setLogInfo(String message) {
    	logger.info(message);
        setMessage(message);
    }

    private void setLogError(String message) {
    	// validation errors are logged as debug messages only
    	logger.debug(message);
    	// message displayed in dialog window
        setErrorMessage(message);
        // delete eventually old hotfix snippet
        hotfixSnippetTextEditor.setStringValue(null);
    }
    
    /**
     * @param duration duration till removal in ms
     */
    private void clearErrorAsync(final int duration) {
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
