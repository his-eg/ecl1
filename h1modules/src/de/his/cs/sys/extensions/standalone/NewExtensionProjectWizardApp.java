package de.his.cs.sys.extensions.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class NewExtensionProjectWizardApp {

    private static void open(){
        Display display = new Display();
        Shell shell = new Shell(display);

        NewExtensionProjectWizardStandalone wizard = new NewExtensionProjectWizardStandalone();
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();

        if (!shell.isDisposed()) {
            shell.close();
        }
        display.dispose();
    }

    public static void main(String[] args) {
       open();
    }
}
