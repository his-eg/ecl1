package net.sf.ecl1.commit.exporter.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.commit.exporter.CommitExporterWizard;
import net.sf.ecl1.utilities.general.SwtUtil;


public class CommitExporterWizardApp {

    private static void open(){
        Display display = new Display();
        SwtUtil.bringShellToForeground(display);
        Image icon = new Image(display, CommitExporterWizardApp.class.getResourceAsStream("/ecl1_icon.png"));
        
        CommitExporterWizard wizard = new CommitExporterWizard();
        WizardDialog.setDefaultImage(icon);
        WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
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
