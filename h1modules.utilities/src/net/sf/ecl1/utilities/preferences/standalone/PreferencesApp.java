package net.sf.ecl1.utilities.preferences.standalone;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sf.ecl1.utilities.preferences.HISinOneExtensionsPreferencePage;


public class PreferencesApp {

    private static void openPreferencePage() {
        PreferenceStore preferenceStore = StandalonePreferenceStore.getSelectedStore();
        Display display = new Display();
        Shell shell = new Shell(display);

        PreferenceManager preferenceManager = new PreferenceManager();
        preferenceManager.addToRoot(new PreferenceNode("general", new HISinOneExtensionsPreferencePage(preferenceStore)));

        PreferenceDialog dialog = new PreferenceDialog(shell, preferenceManager);
        dialog.setPreferenceStore(preferenceStore);
        dialog.open();

        if (!shell.isDisposed()) {
            shell.close();
        }
        display.dispose();
    }

    public static void main(String[] args) {
        openPreferencePage();    
    }
}

