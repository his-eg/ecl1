package net.sf.ecl1.importwizard;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;

import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.preferences.PreferenceWrapper;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Imports projects from a git repo into local workspace
 *
 * @author keunecke
 */
public class ProjectFromGitImporter {
	
    private static final ICommonLogger logger = LoggerFactory.getLogger(ProjectFromGitImporter.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);

    // base path of the source repo configured in preferences, e.g. "ssh://git@git.his.de/"
    private final String baseRepositoryPath;

    private final boolean openProjectOnCreation;

    /**
     * Create a new ProjectFromGitImporter with a source repository base path
     *
     * @param basePath
     */
    public ProjectFromGitImporter(String basePath, boolean openProjectOnCreation) {
        if (!basePath.endsWith("/")) {
            baseRepositoryPath = basePath + "/";
        } else {
            baseRepositoryPath = basePath;
        }
        this.openProjectOnCreation = openProjectOnCreation;
    }

    /**
     * Import an extension into local eclipse workspace
     *
     * @param extensionToImport name to the extension
     * 
     * @throws CoreException
     */
    public void importProject(String extensionToImport) throws CoreException {
        
        // Compute full repository URL depending on configuration
        String fullRepositoryPath = getFullRepositoryPath(extensionToImport);
    	logger.debug("Extension " + extensionToImport + ": fullRepositoryPath = " + fullRepositoryPath);
    	if (fullRepositoryPath != null) {
            IWorkspace workspace = WorkspaceFactory.getWorkspace();
            IPath workspacePath = workspace.getRoot().getLocation();
            IPath extensionPath = workspacePath.append(extensionToImport);
            File extensionFolder = extensionPath.toFile();
            String branch = PreferenceWrapper.getBuildServerView();

            boolean standalone = false;
            if(!net.sf.ecl1.utilities.Activator.isRunningInEclipse()){
                setupStandaloneSsh();
                standalone = true;
            }
            
            try {
                try {
                    CloneCommand clone = Git.cloneRepository();
                    if (branch != null && !"HEAD".equals(branch)) {
                        clone.setBranch(branch);
                    }
                    clone.setDirectory(extensionFolder).setURI(fullRepositoryPath).setCloneAllBranches(true);
                    clone.call();
                } catch (GitAPIException e) {
            		logger.error2(e.getMessage(), e);
                }
                
                if(standalone){
                    // skip eclipse specific code
                    return;
                }

                IProject extensionProject = workspace.getRoot().getProject(extensionToImport);
                IProjectDescription description = extensionProject.getWorkspace().newProjectDescription(extensionProject.getName());

                extensionProject.create(description, null);

                if (openProjectOnCreation) {
                    extensionProject.open(null);
                }

            } catch (CoreException e1) {
                throw e1;
            } finally {
                if (extensionFolder.exists()) {
                    extensionFolder.delete();
                }
            }
    	}
    }
    
    private String getFullRepositoryPath(String extensionToImport) {
    	logger.debug("Extension " + extensionToImport + ": baseRepositoryPath = " + baseRepositoryPath);

		// git lab style, e.g. ssh://git@gitlab.his.de/h1/cs/sys/cs.sys.build.utilities
		int c1Pos = extensionToImport.indexOf('.');
		if (c1Pos < 0) {
	    	logger.error2("Extension " + extensionToImport + ": Illegal extension name! An extension name must have at least 2 dot-separated segments!");
	    	return null;
		}
		String segment1 = extensionToImport.substring(0, c1Pos);
		String segment2;
		int c2Pos = extensionToImport.indexOf('.', c1Pos+1);
		if (c2Pos < 0) {
			// Some extensions like pm.hrm or rt.rtt have only 2 segments...
			segment2 = extensionToImport.substring(c1Pos+1);
		} else {
			segment2 = extensionToImport.substring(c1Pos+1, c2Pos);
		}
		// Create new URL according to https://hiszilla.his.de/hiszilla/show_bug.cgi?id=194146
		return baseRepositoryPath + "h1/" + segment1 + "/" + segment2 + "/" + extensionToImport;
    }

    /**
     * Sets up standalone SSH authentication for JGit.  
     * In Eclipse, SSH authentication is handled automatically via preferences
     */
    private void setupStandaloneSsh(){
        File sshDir = new File(FS.DETECTED.userHome(), ".ssh");
		SshdSessionFactory sshdSessionFactory = new SshdSessionFactoryBuilder()
				.setPreferredAuthentications("publickey")
				.setHomeDirectory(FS.DETECTED.userHome())
				.setSshDirectory(sshDir).build(new JGitKeyCache());
		SshSessionFactory.setInstance(sshdSessionFactory);
    }
}
