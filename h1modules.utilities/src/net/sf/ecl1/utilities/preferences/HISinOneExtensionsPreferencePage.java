package net.sf.ecl1.utilities.preferences;

import h1modules.utilities.utils.Activator;
import net.sf.ecl1.utilities.general.NetUtil;

import java.util.Collection;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * HISinOne-Extension-Tools preferences page
 */
public class HISinOneExtensionsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private StringFieldEditor gitServer;
    private StringFieldEditor buildServer;    
    private StringFieldEditor buildServerView;
    private StringFieldEditor templateRootUrls;

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
        buildServerView = new StringFieldEditor(ExtensionToolsPreferenceConstants.BUILD_SERVER_VIEW_PREFERENCE, "Search view on Build Server (Branches):", getFieldEditorParent());
        templateRootUrls = new StringFieldEditor(ExtensionToolsPreferenceConstants.TEMPLATE_ROOT_URLS, "URLs for new project templates (comma-separated):", getFieldEditorParent());
        addField(gitServer);
        addField(buildServer);
        addField(buildServerView);
        addField(templateRootUrls);
        // Loglevel Combobox
		final String[][] logLevels = new String[4][2];
		logLevels[0][0] = logLevels[0][1] = "DEBUG";
		logLevels[1][0] = logLevels[1][1] = "INFO";
		logLevels[2][0] = logLevels[2][1] = "WARN";
		logLevels[3][0] = logLevels[3][1] = "ERROR";
		addField(new ComboFieldEditor(ExtensionToolsPreferenceConstants.LOG_LEVEL_PREFERENCE, "Log-Level", logLevels, getFieldEditorParent()));
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
        // TODO: validate template root URLs
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
