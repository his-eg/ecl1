package net.sf.ecl1.importwizard;

import org.eclipse.jface.wizard.WizardPage;

/**
 * Base class for extension import wizard pages.
 * @author tneumann
 */
public abstract class ExtensionImportWizardPage extends WizardPage {
	
	public ExtensionImportWizardPage(String pageName) {
		super(pageName);
	}
	
	/**
	 * Update page just when it is entered.
	 */
	abstract void onEnterPage();
}
