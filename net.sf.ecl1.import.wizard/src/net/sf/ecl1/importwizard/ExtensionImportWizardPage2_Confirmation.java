package net.sf.ecl1.importwizard;

import java.util.Set;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Extension import configuration wizard, page 2 handling dependent extensions.
 * 
 * @author tneumann#his.de
 */
public class ExtensionImportWizardPage2_Confirmation extends WizardPage {
	
    //private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	private static final String PAGE_NAME = "page2";
	private static final String PAGE_DESCRIPTION = "Extension Import - Dependencies";

	// top-level container for this page
	private Composite container;

	private Label userSelectedInfo;
	private Table userSelectedTable;
	private TableColumn userSelectedTableColumn;
	
	private Label dependentInfo;
	private Table dependentTable;
	private TableColumn dependentTableColumn;

    private Button openAfterImport;

    private Button deleteExistingFolders;

    // Extension Import Wizard data model
    private ExtensionImportWizardModel model;
    
    /**
     * Create second ExtensionImportWizardPage, containing the confirmation dialog.
     * @param model
     */
    protected ExtensionImportWizardPage2_Confirmation(ExtensionImportWizardModel model) {
        super(PAGE_NAME);
        this.setDescription(PAGE_DESCRIPTION);
        this.model = model;
    }
    
    @Override
    public void createControl(Composite parent) {
    	//logger.log("create controls for page 2");
        
        container = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        container.setLayout(gl);
        
        Label branchInfo = new Label(container, SWT.TOP);
        branchInfo.setText("Branch: " + model.getBranch());

    	// show user-selected extensions
        userSelectedInfo = new Label(container, SWT.TOP);
        userSelectedInfo.setText("Selected extensions:");
        
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        userSelectedTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        userSelectedTable.setLinesVisible(true);
        userSelectedTable.setHeaderVisible(true);
        userSelectedTable.setLayoutData(layoutData);
        userSelectedTable.setSize(200, 600);
        userSelectedTableColumn = new TableColumn(userSelectedTable, SWT.NONE);
        userSelectedTableColumn.setText("Name");

        // Show required dependent extensions
        dependentInfo = new Label(container, SWT.TOP);
        dependentInfo.setText("Additionally required extensions:");

        dependentTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        dependentTable.setLinesVisible(true);
        dependentTable.setHeaderVisible(true);
        dependentTable.setLayoutData(layoutData);
        dependentTable.setSize(200, 600);
        dependentTableColumn = new TableColumn(dependentTable, SWT.NONE);
        dependentTableColumn.setText("Name");
        
        GridLayout gl2 = new GridLayout(2, false);
        Composite openAfterImportComposite = new Composite(container, SWT.BORDER | SWT.CENTER); // was TOP
        openAfterImportComposite.setLayout(gl2);
        openAfterImport = new Button(openAfterImportComposite, SWT.CHECK);
        openAfterImport.setText("Open extensions after import?");
        openAfterImport.setToolTipText("Should wizard open extension projects after import?");
        openAfterImport.setSelection(true);

        deleteExistingFolders = new Button(openAfterImportComposite, SWT.CHECK);
        deleteExistingFolders.setText("Delete folders?");
        deleteExistingFolders.setToolTipText("Should wizard delete existing folders named like extensions for import?");

        setControl(container);
        setPageComplete(false);
    }
    
    void onEnterPage() {
    	//logger.log("create data for page 2");
        
        userSelectedTable.removeAll(); // avoid entries being added several times if the back-button is used
    	Set<String> userSelectedExtensions = model.getSelectedExtensions();
        for (String userSelectedExtension : userSelectedExtensions) {
        	TableItem tableItem = new TableItem(userSelectedTable, SWT.NONE);
        	tableItem.setText(0, userSelectedExtension); // first column has index 0
        }
        userSelectedTableColumn.pack();
        
        // Find extension dependencies recursively
        model.findTotalRequiredExtensions();

        dependentTable.removeAll();
        for (String dependentExtension : model.getTotalRequiredExtensions()) {
        	TableItem tableItem = new TableItem(dependentTable, SWT.NONE);
        	tableItem.setText(0, dependentExtension); // first column has index 0
        }
        dependentTableColumn.pack();
        
        setPageComplete(true);
    }

    @Override
    public IWizardPage getPreviousPage() {
		return ((ExtensionImportWizard) this.getWizard()).page1;
    }
    
    /**
     * User selection if extensions should be opened after import
     *
     * @return true iff user wants extensions to be opened after import
     */
    public boolean openProjectsAfterImport() {
        return openAfterImport.getSelection();
    }

    /**
     * User selection if folders with names like extensions for import should be deleted directly
     *
     * @return true iff user wants to have folders deleted
     */
    public boolean deleteFolders() {
        return deleteExistingFolders.getSelection();
    }
}
