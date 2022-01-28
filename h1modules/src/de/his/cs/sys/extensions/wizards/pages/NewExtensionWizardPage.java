package de.his.cs.sys.extensions.wizards.pages;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.his.cs.sys.extensions.wizards.pages.packages.HISinOneStrategy;
import de.his.cs.sys.extensions.wizards.pages.packages.PackageStructureStrategy;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.general.WorkspaceSupport;
import net.sf.ecl1.utilities.hisinone.HisConstants;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;

/**
 * Extended New Project Wizard Page asking for additional information on extensions
 * 
 * @company HIS eG
 * @author keunecke
 */
public class NewExtensionWizardPage extends WizardNewProjectCreationPage {

	private List projectList;
	
	private Text versionInputTextField;
	
	private PackageStructureStrategy strategy = new HISinOneStrategy();
	
	/**
	 * @param pageName
	 */
	public NewExtensionWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite) getControl();
		
		Composite composite = new Composite(control, SWT.LEFT);
		//Every component with this griddata grabs excess vertical and horizontal space
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true); 
		GridLayout gl = new GridLayout(2, false);
		composite.setLayout(gl);
		composite.setLayoutData(gridData);
		Label versionInputLabel = new Label(composite, SWT.LEFT);
		versionInputLabel.setText("Initial Extension Version");
		versionInputTextField = new Text(composite, SWT.LEFT );
		versionInputTextField.setText("0.0.1");
		versionInputTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false)); //Grab excess horizontal space
		
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
		
		Label warnAboutOldTemplatesLabel = new Label(composite, SWT.TOP);
		warnAboutOldTemplatesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true , false , 2, 1)); //Spans over two columns
		warnAboutOldTemplatesLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		IProject webapps = WebappsUtil.findWebappsProject();
		if ( webapps == null ) {
			warnAboutOldTemplatesLabel.setText("No webapps in workspace detected! The latest templates are located in webapps.\n"
					+ "Please import webapps into your workspace to use the latest templates.\n"
					+ "Falling back to templates from ecl1 that are likely outdated.");
		}

 

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
