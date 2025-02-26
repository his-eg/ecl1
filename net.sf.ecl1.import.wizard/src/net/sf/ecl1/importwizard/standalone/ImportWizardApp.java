package net.sf.ecl1.importwizard.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sf.ecl1.importwizard.ExtensionImportWizard;


public class ImportWizardApp {

    private static void open(){
        Display display = new Display();
        Shell shell = new Shell(display);

        ExtensionImportWizard wizard = new ExtensionImportWizard();
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
