package net.sf.ecl1.utilities.preferences.standalone;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.PreferenceStore;

import net.sf.ecl1.utilities.preferences.PreferenceInitializer;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;

public class StandalonePreferenceStore {

    private static PreferenceStore preferenceStore;

    public static PreferenceStore getSelectedStore(){
        if(preferenceStore == null){
            initializeStore();
        }
        return preferenceStore;
    }

    private static void initializeStore(){
        File eclipseFile = new File(PreferenceWrapper.ECLIPSE_STORE_PATH);
        String selectedStore;
        PreferenceStore initalStore;
        boolean storeExists = true;

        // load inital store to get selected store value
        if(eclipseFile.exists()){
            initalStore = new PreferenceStore(PreferenceWrapper.ECLIPSE_STORE_PATH);
        }else{
            initalStore = new PreferenceStore(PreferenceWrapper.STANDALONE_STORE_PATH);
        }
        try {
            initalStore.load();
        } catch (IOException e) {
            // ignore, no store was created before
            storeExists = false;
        }   

        if(storeExists){
            selectedStore = initalStore.getString(PreferenceWrapper.SELECTED_STORE);
        }else{
            // default to standalone store
            selectedStore = PreferenceWrapper.SELECT_STANDALONE;
        }

        if(selectedStore.equals(PreferenceWrapper.SELECT_ECLIPSE) || selectedStore.isEmpty()){
            preferenceStore = new PreferenceStore(PreferenceWrapper.ECLIPSE_STORE_PATH);
        }else{
            createStoreFileIfNotExists();
            preferenceStore = new PreferenceStore(PreferenceWrapper.STANDALONE_STORE_PATH);
            preferenceStore.setValue(PreferenceWrapper.SELECTED_STORE, PreferenceWrapper.SELECT_STANDALONE);
            try {
                preferenceStore.save();
            } catch (IOException e) {
                //TODO log
            }
        }
    
        try {
            preferenceStore.load();
            new PreferenceInitializer().initializeDefaultPreferencesStandalone(preferenceStore);
        } catch (IOException e) {
            //TODO log
            System.err.println("Could not load preferences: " + e.getMessage());
        }
    }
    
    /**
     * Switches saved preference store.
     * @param newStore store name
     */
    public static void switchStore(String newStore) {
        String newStorePath;

        if(newStore.equals(PreferenceWrapper.SELECT_ECLIPSE)){
            newStorePath = PreferenceWrapper.ECLIPSE_STORE_PATH;
        }else{
            newStorePath = PreferenceWrapper.STANDALONE_STORE_PATH;
            createStoreFileIfNotExists();
        }
        
        preferenceStore = new PreferenceStore(newStorePath);
        try {
            preferenceStore.load();
            preferenceStore.setValue(PreferenceWrapper.SELECTED_STORE, newStore);
            preferenceStore.save();
        } catch (IOException e) {
            //TODO log
            System.err.println("Could not switch preference store: " + e.getMessage());
        }

        syncSelectedStorePreference();
    }

    private static void syncSelectedStorePreference() {
        PreferenceStore eclipseStore = new PreferenceStore(PreferenceWrapper.ECLIPSE_STORE_PATH);
        PreferenceStore standaloneStore = new PreferenceStore(PreferenceWrapper.STANDALONE_STORE_PATH);

        try {
            eclipseStore.load();
            standaloneStore.load();

            String selectedStore = preferenceStore.getString(PreferenceWrapper.SELECTED_STORE);
            eclipseStore.setValue(PreferenceWrapper.SELECTED_STORE, selectedStore);
            standaloneStore.setValue(PreferenceWrapper.SELECTED_STORE, selectedStore);

            eclipseStore.save();
            standaloneStore.save();
        } catch (IOException e) {
            //TODO log
            System.err.println("Could not sync selectedStore preference: " + e.getMessage());
        }
    }

    private static void createStoreFileIfNotExists(){
        File file = new File(PreferenceWrapper.STANDALONE_STORE_PATH);
        if(!file.exists()){
            try {
                file.createNewFile();
                System.out.println("created new store at:" + file.getAbsolutePath());
            } catch (IOException e) {//TODO log
                System.err.println("Could not create preference file: " + e.getMessage());
            }
        } 
    }
}
