package net.sf.ecl1.utilities.preferences.standalone;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.utilities.preferences.HISinOneExtensionsPreferencePage;
import net.sf.ecl1.utilities.standalone.IconPaths;


public class PreferencesApp {

    private static void open() {
        PreferenceStore preferenceStore = StandalonePreferenceStore.getStore();
        Display display = new Display();
        Image icon = new Image(display, IconPaths.ECL1_ICON); 

        PreferenceManager preferenceManager = new PreferenceManager();
        preferenceManager.addToRoot(new PreferenceNode("general", new HISinOneExtensionsPreferencePage(preferenceStore)));
        PreferenceDialog.setDefaultImage(icon);
        PreferenceDialog dialog = new PreferenceDialog(null, preferenceManager);
        dialog.open();

        if (!icon.isDisposed()) {
            icon.dispose();
        }
        if (!display.isDisposed()) {
            display.dispose();
        }
        dialog.close();
    }

    public static void main(String[] args) {
        open();    
    }
}

