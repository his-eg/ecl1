package net.sf.ecl1.importwizard;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.hisinone.HisConstants;

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
        projectChoice.setLayout(gl);
        projectChoice.setLayoutData(layoutData);

        //Filter
        Group filterGroup = new Group(projectChoice, SWT.SHADOW_ETCHED_IN);
        filterGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        filterGroup.setText("Select by expression");
        filterGroup.setLayout(new GridLayout(1, false));
        
        Text regexInput = new Text(filterGroup, SWT.NONE);
        regexInput.setMessage("Use commas to chain expressions. Example: cm.exa.,cm.app.");
        GridData regexInputGridData = new GridData(GridData.FILL_HORIZONTAL);
        regexInput.setLayoutData(regexInputGridData);
        regexInput.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				
				if (regexInput.getText().isBlank()) {
					for(TableItem item : projectTable.getItems()) {
						item.setChecked(false);
					}
					return;
				}
				
				String[] filters = regexInput.getText().split(",");
				outerLoop: for(TableItem item : projectTable.getItems()) {
					for(String filter : filters) {
						if (filter.isBlank()) {
							continue;
						}
						if(item.getText().startsWith(filter.strip())) {
							item.setChecked(true);
							continue outerLoop;
						} 
						item.setChecked(false);
					}
				}
			}
		});

        final Button selectAllButton = new Button(projectChoice, SWT.CHECK);
        selectAllButton.setText("Select all");
        selectAllButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSelect(selectAllButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                handleSelect(selectAllButton.getSelection());
            }

            private void handleSelect(boolean selection) {
                for (TableItem item : projectTable.getItems()) {
                    item.setChecked(selection);
                }
            }
        });
        
        Composite tableComposite = new Composite(projectChoice, SWT.NONE);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        projectTable = new Table(tableComposite, SWT.MULTI | SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        projectTable.setLinesVisible(true);
        projectTable.setHeaderVisible(true);
        
        TableColumn column = new TableColumn(projectTable, SWT.NONE);
        column.setText("Name");
        tableColumnLayout.setColumnData(column, new ColumnWeightData(1));

        Set<String> extensionsInWorkspace = model.getExtensionsInWorkspace();
        for (String remoteExtensionName : model.getRemoteExtensions()) {
        	//Extension projects always have at least two dots (.) in their names. We filter out everything that does not have
        	//two dots in its name...
        	Pattern p = Pattern.compile(".+\\..+\\..+");
        	Matcher m = p.matcher(remoteExtensionName);       	
            if (!extensionsInWorkspace.contains(remoteExtensionName) && m.matches()) {
                TableItem tableItem = new TableItem(projectTable, SWT.NONE);
                tableItem.setChecked(false);
                tableItem.setText(0, remoteExtensionName);
            }
        }

        setControl(container);
        setPageComplete(true); // enable "next"-button
    }
    
    /**
     * Dynamically prepare data for page 2 from results of this page, and return page 2.
     * @return complete page 2
     */
    // Implementation note: Overriding the getPreviousPage() and getNextPage() methods to permit data processing
    // between page transitions is simpler than overriding Wizard.getNextPage(), because we don't need case distinctions.
    @Override
    public IWizardPage getNextPage() {
    	this.initSelectedExtensions();
    	ExtensionImportWizardPage2_Confirmation page2 = ((ExtensionImportWizard) this.getWizard()).page2;
		page2.onEnterPage();
    	return page2;
    }

    /**
     * Get user selected extensions to import from page 1
     */
    private void initSelectedExtensions() {
        Set<String> selectedExtensions = new TreeSet<String>();
        TableItem[] items = projectTable.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                String text = item.getText(0);
                selectedExtensions.add(text);
            }
        }
        model.setSelectedExtensions(selectedExtensions);
    }
}
