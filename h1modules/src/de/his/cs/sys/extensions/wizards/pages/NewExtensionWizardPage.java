package de.his.cs.sys.extensions.wizards.pages;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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
 * @company HIS eG
 * @author keunecke
 */
public class NewExtensionWizardPage extends WizardPage {

	private List projectList;
	private Text versionInputTextField;
	private Text projectNameText;
	private final PackageStructureStrategy strategy = new HISinOneStrategy();
	
	
	/**
	 * @param pageName
	 */
	public NewExtensionWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.LEFT);
		//Every component with this griddata grabs excess vertical and horizontal space
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true); 
		GridLayout gl = new GridLayout(2, false);
		composite.setLayout(gl);
		composite.setLayoutData(gridData);

		// Project name 
		Label pojectNameLabel = new Label(composite, SWT.NONE);
		pojectNameLabel.setText("Project name:");
		projectNameText = new Text(composite, SWT.BORDER);
		projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Version
		Label versionInputLabel = new Label(composite, SWT.LEFT);
		versionInputLabel.setText("Initial Extension Version");
		versionInputTextField = new Text(composite, SWT.LEFT );
		versionInputTextField.setText("0.0.1");
		versionInputTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false)); //Grab excess horizontal space
		
		// Projects
		Label projectChoiceLabel = new Label(composite, SWT.TOP);
		projectChoiceLabel.setText("Referenced Projects");
		projectList = new List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		projectList.setLayoutData(gridData); //Grab excess vertical and horizontal space
		java.util.List<String> references = new WorkspaceSupport().getPossibleProjectsToReference();
		int index = 0;
		for (String ref : references) {
			projectList.add(ref);
			if(HisConstants.WEBAPPS.equals(ref)) {
				index = references.indexOf(ref);
			}
		}
		projectList.select(index);

		// Project name listener
		projectNameText.addModifyListener(event -> {
			if(projectNameText.getText().contains("_")) {
				setMessage("HISInOne Extension must not contain the character \"_\" in its name.", WizardPage.ERROR);
				setPageComplete(false);
			}else if(references.contains(projectNameText.getText())){
				setMessage("A project with that name already exists in the workspace.", WizardPage.ERROR);
				setPageComplete(false);
			}else{
				setMessage(null);
				setPageComplete(true);
			}
		});
		
		Label warnAboutOldTemplatesLabel = new Label(composite, SWT.TOP);
		warnAboutOldTemplatesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true , false , 2, 1)); //Spans over two columns
		warnAboutOldTemplatesLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		IProject webapps = WebappsUtil.findWebappsProject();
		if ( webapps == null ) {
			warnAboutOldTemplatesLabel.setText("No webapps in workspace detected! The latest templates are located in webapps.\n"
					+ "Please import webapps into your workspace to use the latest templates.\n"
					+ "Falling back to templates from ecl1 that are likely outdated.");
		}

		setControl(composite);
	}
	
	/**
	 * Get project location as URI
	 * 
	 * @return location - URI
	 */
	public URI getLocationURI(){
		return WorkspaceFactory.getWorkspace().getRoot().getLocationURI();
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
	 * @return collection of project names to reference by the new extension project
	 */
	private Collection<String> getProjectsToReference() {
		Collection<String> result = new ArrayList<>();
		String[] selection = projectList.getSelection();
        result.addAll(Arrays.asList(selection));
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
		return new InitialProjectConfigurationChoices(getProjectsToReference(), projectNameText.getText(), getInitialVersion());
	}
}
