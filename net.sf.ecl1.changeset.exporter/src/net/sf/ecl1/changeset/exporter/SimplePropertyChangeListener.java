package net.sf.ecl1.changeset.exporter;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public class SimplePropertyChangeListener implements IPropertyChangeListener {
	private ChangeSetExporterWizardPage page;
	public SimplePropertyChangeListener(ChangeSetExporterWizardPage page) {
		this.page = page;
	}
	@Override
	public void propertyChange(PropertyChangeEvent changeEvent) {
    	// clear previous messages
        page.setMessage(null);
        page.setErrorMessage(null);
		page.checkSetHotfixInformation();
	}
}