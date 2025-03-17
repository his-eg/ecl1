package net.sf.ecl1.commit.exporter.standalone;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import net.sf.ecl1.commit.exporter.CommitExporterWizard;
import net.sf.ecl1.utilities.general.SwtUtil;
import net.sf.ecl1.utilities.standalone.IconPaths;


public class CommitExporterWizardApp {

    private static void open(){
        Display display = new Display();
        SwtUtil.bringShellToForeground(display);
        Image icon = new Image(display, IconPaths.getEcl1IconPath()); 
        
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
