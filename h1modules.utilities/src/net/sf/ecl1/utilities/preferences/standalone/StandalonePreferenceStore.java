package net.sf.ecl1.utilities.preferences.standalone;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.PreferenceStore;

import net.sf.ecl1.utilities.preferences.PreferenceInitializer;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

public class StandalonePreferenceStore {

    private static PreferenceStore preferenceStore;

    public static PreferenceStore getStore(){
        if(preferenceStore == null){
            initializeStore();
        }
        return preferenceStore;
    }

    private static void initializeStore(){
        String path = PreferenceWrapper.getEclipseStorePath();
        File file = new File(path);
        if(!file.exists()){
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                // no log because logger initialisation depends on this store
                System.out.println("Created new preference store at: " + file.getAbsolutePath());
            } catch (IOException e) {
                // no log because logger initialisation depends on this store
                System.err.println("Exception creating new preference store at: " + file.getAbsolutePath() + "\n" + e.getMessage());      
            }
        }

        preferenceStore = new PreferenceStore(path);

        try {
            preferenceStore.load();
            new PreferenceInitializer().initializeDefaultPreferencesStandalone(preferenceStore);
        } catch (IOException e) {
            // no log because logger initialisation depends on this store
            System.err.println("Could not load preferences at: " + file.getAbsolutePath() +"\n" + e.getMessage());      
        }
    }
}
