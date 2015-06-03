package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * HISinOne-Extension-Tools preferences page
 */

public class HISinOneExtensionsPreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

    public HISinOneExtensionsPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Preferences for HISinOne-Extension-Tools");
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors() {
        addField(new StringFieldEditor(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE, "GIT Server:", getFieldEditorParent()));
        addField(new StringFieldEditor(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE, "Build Server:", getFieldEditorParent()));
        addField(new StringFieldEditor(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, "Search view on Build Server (Branches):", getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
        //nop
    }

}