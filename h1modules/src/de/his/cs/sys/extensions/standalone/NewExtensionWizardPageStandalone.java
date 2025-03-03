package de.his.cs.sys.extensions.standalone;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import de.his.cs.sys.extensions.wizards.pages.packages.HISinOneStrategy;
import de.his.cs.sys.extensions.wizards.pages.packages.PackageStructureStrategy;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.WorkspaceSupport;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Extended New Project Wizard Page asking for additional information on extensions
 * 
 */
public class NewExtensionWizardPageStandalone extends WizardPage {

	private List projectList;
	
	private Text versionInputTextField;
	
	private final PackageStructureStrategy strategy = new HISinOneStrategy();

	private Text projectNameText;
    private Button useDefaultLocationCheckbox;
    private Text locationText;
    private Button browseButton;
	
	/**
	 * @param pageName
	 */
	public NewExtensionWizardPageStandalone(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.LEFT);
		//Every component with this griddata grabs excess vertical and horizontal space
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true); 
		GridLayout gl = new GridLayout(4, false);
		composite.setLayout(gl);
		composite.setLayoutData(gridData);

        // Project name 
        Label pojectNameLabel = new Label(composite, SWT.NONE);
		pojectNameLabel.setText("Project name:");
        projectNameText = new Text(composite, SWT.BORDER);
        projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		projectNameText.addModifyListener(event -> {
			if(projectNameText.getText().contains("_")) {
				setMessage("HISInOne Extension must not contain the character \"_\" in its name.");
			}else{
				setMessage(null);
			}
		});

        // Checkbox for "Use default location"
        useDefaultLocationCheckbox = new Button(composite, SWT.CHECK);
        useDefaultLocationCheckbox.setText("Use default location");
		useDefaultLocationCheckbox.setSelection(true);
        useDefaultLocationCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
		String defaultPath = WorkspaceFactory.getWorkspace().getRoot().getLocation().toString();
        useDefaultLocationCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean useDefault = useDefaultLocationCheckbox.getSelection();
                locationText.setEnabled(!useDefault);
                browseButton.setEnabled(!useDefault);
                if (useDefault) {
                    locationText.setText(defaultPath);
                }else{
					locationText.setText("");
				}
            }
        });

        // Location
        Label locationLabel = new Label(composite, SWT.NONE);
		locationLabel.setText("Location:");
        locationText = new Text(composite, SWT.BORDER);
        locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		locationText.setText(defaultPath);
		locationText.setEnabled(false);

        // Browse button
        browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog directoryDialog = new DirectoryDialog(composite.getShell());
                String selectedDirectory = directoryDialog.open();
                if (selectedDirectory != null) {
                    locationText.setText(selectedDirectory);
                }
            }
        });

		// Version
		Label versionInputLabel = new Label(composite, SWT.LEFT);
		versionInputLabel.setText("Initial Extension Version");
		versionInputTextField = new Text(composite, SWT.LEFT );
		versionInputTextField.setText("0.0.1");
		versionInputTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		// Projects
		Label projectChoiceLabel = new Label(composite, SWT.TOP);
		projectChoiceLabel.setText("Referenced Projects");
		projectList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		projectList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		java.util.List<String> references = new WorkspaceSupport().getPossibleProjectsToReference();
		// Add webapps manually(if exists), because standalone doesnt recognize it in workspace
		IProject webapps = WebappsUtil.findWebappsProject();
		if(webapps != null){
			references.add(webapps.getName());
		}
		int index = 0;
		for (String ref : references) {
			projectList.add(ref);
			if(HisConstants.WEBAPPS.equals(ref)) {
				index = references.indexOf(ref);
			}
		}
		projectList.select(index);
		
		Label warnAboutOldTemplatesLabel = new Label(composite, SWT.TOP);
		warnAboutOldTemplatesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true , false , 4, 1)); 
		warnAboutOldTemplatesLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		if ( webapps == null ) {
			warnAboutOldTemplatesLabel.setText("No webapps in workspace detected! The latest templates are located in webapps.\n"
					+ "Please import webapps into your workspace to use the latest templates.\n"
					+ "Falling back to templates from ecl1 that are likely outdated.");
		}

		setControl(composite);
	}
	
	/**
	 * Get used project location as URI
	 * 
	 * @return location - URI
	 */
	public URI getLocationURI(){
		return Paths.get(locationText.getText()).toUri();
	}

	/**
	 * Get the initial version of the new extension
	 * 
	 * @return initial version string
	 */
	private String getInitialVersion(){
		return versionInputTextField.getText();
	}

	/**
	 * Get the project name
	 * 
	 * @return project name
	 */
	private String getProjectName(){
		return projectNameText.getText();
	}
	
	/**
	 * @return collection of project names to reference by the new extension project
	 */
	private Collection<String> getProjectsToReference() {
		Collection<String> result = new ArrayList<String>();
		String[] selection = projectList.getSelection();
		for (String project : selection) {
			result.add(project);
		}
		return result;
	}
	
	/**
	 * @return the package structure strategy
	 */
	public PackageStructureStrategy getStrategy() {
		return this.strategy;
	}
	
	/**
	 * @return get the initial choices done by the user
	 */
	public InitialProjectConfigurationChoices getInitialConfiguration() {
		return new InitialProjectConfigurationChoices(getProjectsToReference(), getProjectName(), getInitialVersion());
	}
}
