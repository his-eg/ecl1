package net.sf.ecl1.commit.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.Git;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import net.sf.ecl1.commit.exporter.git.CommitTableFactory;
import net.sf.ecl1.commit.exporter.git.DiffTool;
import net.sf.ecl1.commit.exporter.git.StagedChanges;
import net.sf.ecl1.utilities.general.ConsoleLogger;
import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.hisinone.ReleaseXmlUtil;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

public class CommitExporterWizardPage extends WizardPage {

	private static final ConsoleLogger logger = new ConsoleLogger(CommitExportWizardPlugin.getDefault().getLog(), CommitExportWizardPlugin.PLUGIN_ID, CommitExporterWizardPage.class.getSimpleName());

    private CheckboxTableViewer commitTable;

    private Git git;
    
    private DiffTool diffStorage = new DiffTool();

    private StringFieldEditor hotfixTitle;

    private StringFieldEditor hotfixDescription;

    private StringFieldEditor hiszillaTickets;

    private BooleanFieldEditor dbUpdateRequired;

    private Button checkAllSelected;

    private Button uncheckAllSelected;

    private StringFieldEditor hotfixSnippetTextEditor;

    private Set<String> addedOrModifiedFiles = new TreeSet<String>();
    
    private Set<String> deletedFiles = new TreeSet<String>();

    private SimplePropertyChangeListener propertyChangeListener;
    
    private boolean validate = false;
    
    public CommitExporterWizardPage() {
        super("Commit Exporter Wizard");
        this.setTitle("Describe the hotfix and select commits");
        propertyChangeListener = new SimplePropertyChangeListener(this);
    }
    
    @Override
    public void createControl(Composite parent) {
    	locateGit();

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

        Label tableLabel = new Label(pageComposite, SWT.LEFT);
        tableLabel.setText("Commits");
        
        commitTable = CommitTableFactory.createCommitTable(pageComposite);
        commitTable.addCheckStateListener(propertyChangeListener);
        /* ----------------------
         * Set the content provider for the table
         * ----------------------
         */
        if(git != null ) {
	        commitTable.setContentProvider(new ArrayContentProvider());
	        List<Object> allCommits = new ArrayList<>();
	        StagedChanges stagedChanges = new StagedChanges();
	        allCommits.add(stagedChanges);
	        allCommits.addAll(GitUtil.getLastCommits(git,100));
	        commitTable.setInput(allCommits);
	        commitTable.setChecked(stagedChanges, true);
        }

        new Label(pageComposite, SWT.NONE); //Needed to correctly align the following elements in the layout
        Composite processSelectButtonsComp = new Composite(pageComposite, SWT.LEFT);
        RowLayout rl_processSelectButtonsComp = new RowLayout();
        rl_processSelectButtonsComp.marginLeft = 0;
        processSelectButtonsComp.setLayout(rl_processSelectButtonsComp);
        checkAllSelected = new Button(processSelectButtonsComp, SWT.PUSH);
        checkAllSelected.setText("&Check all selected rows");
        checkAllSelected.setToolTipText("ALT + C");
        checkAllSelected.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selectedRows = commitTable.getStructuredSelection();
                for (Object o : selectedRows.toList()) {               	
                    commitTable.setChecked(o, true);
                }
                //Necessary, because commitTable.setChecked does not fire the check state listener... 
                propertyChangeListener.handleEvent(e); 
            }
        });

        uncheckAllSelected = new Button(processSelectButtonsComp, SWT.PUSH);
        uncheckAllSelected.setText("&Uncheck all selected rows");
        uncheckAllSelected.setToolTipText("ALT + U");
        uncheckAllSelected.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selectedRows = commitTable.getStructuredSelection();
                for (Object o : selectedRows.toList()) {
                    commitTable.setChecked(o, false);
                }
                //Necessary, because commitTable.setChecked does not fire the check state listener... 
                propertyChangeListener.handleEvent(e); 
            }
        });

        this.hotfixSnippetTextEditor = new StringFieldEditor("snippet", "Hotfix Snippet", pageComposite);

        setControl(pageComposite);
    }

    private void locateGit() {
    	IProject webappsProject = WebappsUtil.findWebappsProject();
        if (webappsProject == null) {
            setErrorMessage("Could not find webapps project in workspace! Commit Exporter will not work! Please add (exactly) one webapps project to your workspace.");
        } else {
            String webappsPath = webappsProject.getLocation().toString();
            logger.info("Starting search for git-repo at this location: " + webappsPath);
            git = GitUtil.searchGitRepo(webappsPath);
            if(git == null ) {
            	setErrorMessage("Found a webapps project, but no git repository. Commit Exporter will not work! Please make sure this version of webapps has a git repository.");
            } else {
                logger.info("Found git-repo at: " + git.getRepository().getDirectory().toString());
            }
        }
    }

    void createHotfix() {
        if (validUserInput()) {

            diffStorage.computeDiff(commitTable.getCheckedElements(), git);
        	addedOrModifiedFiles = diffStorage.getAddedOrModifiedFiles();
        	deletedFiles = diffStorage.getDeletedFiles();

            if (validate && addedOrModifiedFiles.isEmpty()) {
                setLogError("The selected commits contain no files or all modified files are outside of qisserver!");
                return;
            }

            String title = hotfixTitle.getStringValue();
            String description = hotfixDescription.getStringValue();
            String hiszilla = hiszillaTickets.getStringValue();
            boolean isDbUpdateRequired = dbUpdateRequired.getBooleanValue();
            String hotfixSnippet = new HotfixInformation(title, description, hiszilla, isDbUpdateRequired, addedOrModifiedFiles, deletedFiles).toXml();
            logger.debug("Created hotfix snippet:\n" + hotfixSnippet);

            // add content to clipboard
            final Display display = getControl().getDisplay();
            final Clipboard cb = new Clipboard(display);
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { hotfixSnippet }, new Transfer[] { textTransfer });
            this.hotfixSnippetTextEditor.setStringValue(hotfixSnippet);
            setLogInfo("Hotfix XML snippet copied to clipboard!");
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

    
    private boolean hasGit() {
    	if(git == null) {
    		return false;
    	} else {
    		return true;
    	}
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
            validate = hasGit() && hasTitle() && hasDescription() && hasHiszilla() && hasAtLeastOneCommitChecked();
        }
    }

    /**
     * Validates the user input
     */
    private boolean validUserInput() {

        checkSetValidationRequired();
        

        if(!hasGit()) {
            if (validate) setLogError("No Git repository found!");
            return false;
        }
        
        if (!hasTitle()) {
            if (validate) setLogError("No hotfix title provided!");
            return false;
        }
        if (!hasDescription()) {
            if (validate) setLogError("No description for hotfix provided!");
            return false;
        }
        if (!hasHiszilla()) {
            if (validate) setLogError("No hiszilla tickets for hotfix provided!");
            return false;
        }
        if (!hasAtLeastOneCommitChecked()) {
            if (validate) setLogError("No commit selected!");
            return false;
        }
        
        
        setMessage(null);
        setErrorMessage(null);
        return true;
    }

    private boolean hasAtLeastOneCommitChecked() {
        if (commitTable.getCheckedElements().length == 0) {
            return false;
        } else {
            return true;
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
}
