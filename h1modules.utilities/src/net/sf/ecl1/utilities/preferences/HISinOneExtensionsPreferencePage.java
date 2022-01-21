package net.sf.ecl1.utilities.preferences;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.general.GitUtil;
import net.sf.ecl1.utilities.general.NetUtil;

import java.util.Collection;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
    private BooleanFieldEditor automaticBranchDetection;
    private BooleanFieldEditor displaySummaryOfGitPull;

    public HISinOneExtensionsPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getPreferences());
        setDescription("Preferences for HISinOne-Extension-Tools");
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent event) {
    	
    	//Enable/disable manual setting of branch
    	if(event.getSource().equals(automaticBranchDetection)) {
    		boolean newValue = ((Boolean) event.getNewValue()).booleanValue();
    		buildServerView.setEnabled(!newValue, getFieldEditorParent());
    		
    		//Update branch immediately if automatic detection is set to true by user
    		if(newValue) {
    			getPreferenceStore().setValue(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY, GitUtil.getCheckedOutBranchOfWebapps());
    			buildServerView.load();
    		}
    	}
    	
    	super.propertyChange(event);
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors() {
        gitServer = new StringFieldEditor(PreferenceWrapper.GIT_SERVER_PREFERENCE_KEY, "GIT Server:", getFieldEditorParent());
        buildServer = new StringFieldEditor(PreferenceWrapper.BUILD_SERVER_PREFERENCE_KEY, "Build Server:", getFieldEditorParent());
        automaticBranchDetection = new BooleanFieldEditor(PreferenceWrapper.DETECT_BRANCH_AUTOMATICALLY, "Detect branch of webapps automatically? ", BooleanFieldEditor.SEPARATE_LABEL, getFieldEditorParent());
        buildServerView = new StringFieldEditor(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY, "Branch of webapps: ", getFieldEditorParent());
        buildServerView.setEnabled(!getPreferenceStore().getBoolean(PreferenceWrapper.DETECT_BRANCH_AUTOMATICALLY), getFieldEditorParent());
        templateRootUrls = new StringFieldEditor(PreferenceWrapper.TEMPLATE_ROOT_URLS_PREFERENCE_KEY, "URLs for new project templates (comma-separated):", getFieldEditorParent());
        displaySummaryOfGitPull = new BooleanFieldEditor(PreferenceWrapper.DISPLAY_SUMMARY_OF_GIT_PULL, "Display summary of git batch pull?", BooleanFieldEditor.SEPARATE_LABEL, getFieldEditorParent());
        addField(gitServer);
        addField(buildServer);
        addField(automaticBranchDetection);
        addField(buildServerView);
        addField(templateRootUrls);
        addField(displaySummaryOfGitPull);
        // Loglevel Combobox
		final String[][] logLevels = new String[4][2];
		logLevels[0][0] = logLevels[0][1] = "DEBUG";
		logLevels[1][0] = logLevels[1][1] = "INFO";
		logLevels[2][0] = logLevels[2][1] = "WARN";
		logLevels[3][0] = logLevels[3][1] = "ERROR";
		addField(new ComboFieldEditor(PreferenceWrapper.LOG_LEVEL_PREFERENCE_KEY, "Log-Level", logLevels, getFieldEditorParent()));
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
    	//This is needed to update the value for the branch in case the user has switched branches since the last time 
    	//when this setting page was open
    	getPreferenceStore().setValue(PreferenceWrapper.BUILD_SERVER_VIEW_PREFERENCE_KEY,PreferenceWrapper.getBuildServerView());
    }
}
