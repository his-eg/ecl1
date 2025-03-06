package net.sf.ecl1.commit.exporter.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sf.ecl1.commit.exporter.CommitExporterWizard;


public class CommitExporterWizardApp {

    private static void open(){
        Display display = new Display();
        Shell shell = new Shell(display);

        CommitExporterWizard wizard = new CommitExporterWizard();
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
