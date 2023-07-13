package net.sf.ecl1.importwizard;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.sf.ecl1.importwizard.ExtensionImportWizardModel.Extension;
import net.sf.ecl1.utilities.general.GitUtil;

/**
 * Extension import configuration wizard page.
 *
 * @author keunecke
 */
public class ExtensionImportWizardPage1_Selection extends WizardPage {
	
	//private static final ConsoleLogger logger = ConsoleLogger.getEcl1Logger();

	private static final String PAGE_NAME = "page1";
	private static final String PAGE_DESCRIPTION = "Extension Import - Selection";

    private Table projectTable;
	
    // Extension Import Wizard data model
    private ExtensionImportWizardModel model;
    
    /**
     * Create first ExtensionImportWizardPage, containing the extension selection dialog.
     * @param model
     */
    protected ExtensionImportWizardPage1_Selection(ExtensionImportWizardModel model) {
        super(PAGE_NAME);
        this.setDescription(PAGE_DESCRIPTION);
        this.model = model;
    }

    @Override
    public void createControl(Composite parent) {
    	//logger.log("create controls for page 1");
        
    	Composite container = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, true);
        container.setLayout(gl);

        Label branchInfo = new Label(container, SWT.TOP);
        branchInfo.setText("Branch: " + model.getBranch());
        
        if(model.getBranch() == GitUtil.UNKNOWN_BRANCH) {
        	setErrorMessage("Could not determine branch of git repository of webapps project. Either there is no repo or you are on a linked work tree.");
        }

        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        Composite projectChoice = new Composite(container, SWT.BORDER | SWT.TOP);
        projectChoice.setLayout(new GridLayout(1, false));
        projectChoice.setLayoutData(layoutData);

        //Filter
        Group filterGroup = new Group(projectChoice, SWT.SHADOW_ETCHED_IN);
        filterGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        filterGroup.setText("Filter by expression");
        filterGroup.setLayout(new GridLayout(2, false));
        
        Text filterExpression = new Text(filterGroup, SWT.NONE);
        filterExpression.setMessage("Use commas to chain expressions. Example: cm.exa.,cm.app.");
        GridData filterExpressionGridData = new GridData(GridData.FILL_HORIZONTAL);
        filterExpressionGridData.horizontalSpan = 2;
        filterExpression.setLayoutData(filterExpressionGridData);
        filterExpression.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				
				projectTable.removeAll();
				
				//Show all extensions if filter text is empty
				if(filterExpression.getText().trim().isEmpty()) {
					for(Extension extension : model.getSelectableExtensions()) {
			        	TableItem tableItem = new TableItem(projectTable, SWT.NONE);
			        	tableItem.setChecked(extension.isChecked());
			        	tableItem.setText(extension.getName());
					}
				}
				
				//Determine which extensions to show after filtering
				String[] filters = filterExpression.getText().split(",");
				outerLoop: for(Extension extension : model.getSelectableExtensions()) {
					for(String filter : filters) {
						if (filter.trim().isEmpty()) {
							continue;
						}
						if (extension.getName().startsWith(filter.trim())) {
				        	TableItem tableItem = new TableItem(projectTable, SWT.NONE);
				        	tableItem.setChecked(extension.isChecked());
				        	tableItem.setText(extension.getName());
							continue outerLoop;
						}
					}
				}
			}
		});

        final Button selectAllButton = new Button(filterGroup, SWT.PUSH);
        selectAllButton.setText("&Select all");
        selectAllButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSelect(true);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleSelect(true);
            }
            
        });
        
        final Button deselectAllButton = new Button(filterGroup, SWT.PUSH);
        deselectAllButton.setText("&Deselect all");
        deselectAllButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSelect(false);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleSelect(false);
            }
        });
        
        Composite tableComposite = new Composite(projectChoice, SWT.NONE);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        projectTable = new Table(tableComposite, SWT.MULTI | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        projectTable.setLinesVisible(true);
        projectTable.setHeaderVisible(true);
        //Update model when TableItem get checked/unchecked
        projectTable.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleEvent(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				handleEvent(e);
			}
			
			private void handleEvent(SelectionEvent e) {
				if (e.detail == SWT.CHECK) {
					for(Extension extension : model.getSelectableExtensions()) {
						TableItem tableItem = (TableItem) e.item;
						if (extension.getName().equals(tableItem.getText())) {
							extension.setChecked(tableItem.getChecked());
						}
					}
				}
			}
			
		});
        
        TableColumn column = new TableColumn(projectTable, SWT.NONE);
        column.setText("Name");
        tableColumnLayout.setColumnData(column, new ColumnWeightData(1));

        for(Extension e : model.getSelectableExtensions()) {
        	TableItem tableItem = new TableItem(projectTable, SWT.NONE);
        	tableItem.setChecked(false);
        	tableItem.setText(e.getName());
        }

        setControl(container);
        setPageComplete(true); // enable "next"-button
    }
    
    /**
     * 
     * Called from the "Select all" and "Deselect all" button. 
     * Either select or deselect all currently displayed extensions in 
     * the table. 
     * 
     * @param selected
     */
    private void handleSelect(boolean selected) {
    	outerLoop: for(TableItem tableItem : projectTable.getItems()) {
    		//Update UI
    		tableItem.setChecked(selected);
    		for(Extension e : model.getSelectableExtensions()) {
    			if (e.getName().equals(tableItem.getText())) {
    				//Update model
    				e.setChecked(selected);
    				continue outerLoop;
    			}
    		}
    	}
    }
    
    /**
     * Dynamically prepare data for page 2 from results of this page, and return page 2.
     * @return complete page 2
     */
    // Implementation note: Overriding the getPreviousPage() and getNextPage() methods to permit data processing
    // between page transitions is simpler than overriding Wizard.getNextPage(), because we don't need case distinctions.
    @Override
    public IWizardPage getNextPage() {
    	ExtensionImportWizardPage2_Confirmation page2 = ((ExtensionImportWizard) this.getWizard()).page2;
		page2.onEnterPage();
    	return page2;
    }

}
