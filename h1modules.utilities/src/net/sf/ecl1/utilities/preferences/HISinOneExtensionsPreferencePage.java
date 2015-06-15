package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;

import java.util.Collection;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * HISinOne-Extension-Tools preferences page
 */

public class HISinOneExtensionsPreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {

    private StringFieldEditor gitServer;

    private StringFieldEditor buildServer;

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
        gitServer = new StringFieldEditor(ExtensionToolsPreferenceConstants.GIT_SERVER_PREFERENCE, "GIT Server:", getFieldEditorParent());
        buildServer = new StringFieldEditor(ExtensionToolsPreferenceConstants.BUILD_SERVER_PREFERENCE, "Build Server:", getFieldEditorParent());
        addField(gitServer);
        addField(buildServer);
        addField(new StringFieldEditor(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, "Search view on Build Server (Branches):", getFieldEditorParent()));
    }



    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        validateUrls();
        super.performApply();
    }

    /**
     * validate git server and build server url
     */
    private void validateUrls() {
        String buildServerValue = buildServer.getStringValue();
        Collection<String> errors = Lists.newArrayList();
        if (!buildServerValue.endsWith("/")) {
            buildServer.setStringValue(buildServerValue + "/");
        }
        if(!NetUtil.canOpenSocket(buildServerValue)) {
            errors.add("Cannot reach Build Server '" + buildServerValue + "'");
        }
        String gitServerValue = gitServer.getStringValue();
        if (!gitServerValue.endsWith("/")) {
            gitServer.setStringValue(gitServerValue + "/");
        }
        if (!NetUtil.canOpenSocket(gitServerValue)) {
            errors.add("Cannot reach Git Server '" + gitServerValue + "'");
        }
        if(!errors.isEmpty()) {
            setErrorMessage(Joiner.on("\n").join(errors));
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        validateUrls();
        String errorMessage = getErrorMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return false;
        }
        return super.performOk();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
        //nop
    }

}