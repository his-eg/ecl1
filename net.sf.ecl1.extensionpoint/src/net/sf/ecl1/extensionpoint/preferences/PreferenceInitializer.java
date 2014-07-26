package net.sf.ecl1.extensionpoint.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import net.sf.ecl1.extensionpoint.Constants;
import net.sf.ecl1.extensionpoint.ExtensionPointBuilderPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ExtensionPointBuilderPlugin.getDefault().getPreferenceStore();
		store.setDefault(Constants.LOGGING_PREFERENCE, false);
	}

}
