package net.sf.ecl1.utilities.preferences.standalone;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.utilities.general.SwtUtil;
import net.sf.ecl1.utilities.preferences.HISinOneExtensionsPreferencePage;


public class PreferencesApp {

    private static void open() {
        PreferenceStore preferenceStore = StandalonePreferenceStore.getStore();
        Display display = new Display();
        SwtUtil.bringShellToForeground(display);
        Image icon = new Image(display, PreferencesApp.class.getResourceAsStream("/ecl1_icon.png"));

        PreferenceManager preferenceManager = new PreferenceManager();
        preferenceManager.addToRoot(new PreferenceNode("general", new HISinOneExtensionsPreferencePage(preferenceStore)));
        PreferenceDialog.setDefaultImage(icon);
        PreferenceDialog dialog = new PreferenceDialog(display.getActiveShell(), preferenceManager);
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

