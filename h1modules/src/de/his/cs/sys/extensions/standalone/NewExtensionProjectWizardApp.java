package de.his.cs.sys.extensions.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.his.cs.sys.extensions.wizards.NewExtensionProjectWizard;
import net.sf.ecl1.utilities.standalone.IconPaths;


public class NewExtensionProjectWizardApp {

    private static void open(){
        Display display = new Display();
        Image icon = new Image(display, IconPaths.ECL1_ICON); 

        NewExtensionProjectWizard wizard = new NewExtensionProjectWizard();
        WizardDialog.setDefaultImage(icon);
        WizardDialog dialog = new WizardDialog(null, wizard);
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
