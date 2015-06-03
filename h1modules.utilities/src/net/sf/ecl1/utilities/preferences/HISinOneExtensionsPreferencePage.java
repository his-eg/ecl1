package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class HISinOneExtensionsPreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

    public HISinOneExtensionsPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("A demonstration of a preference page implementation");
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors() {
        addField(new StringFieldEditor(PreferenceConstants.GIT_SERVER_PREFERENCE, "GIT Server:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.BUILD_SERVER_PREFERENCE, "Build Server:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, "Search view on Build Server:", getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

}