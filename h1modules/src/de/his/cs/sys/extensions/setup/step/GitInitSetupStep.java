package de.his.cs.sys.extensions.setup.step;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;

import de.his.cs.sys.extensions.Activator;
import de.his.cs.sys.extensions.steps.ProjectSetupStep;
import net.sf.ecl1.utilities.general.InitialProjectConfigurationChoices;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;
import net.sf.ecl1.utilities.standalone.workspace.WorkspaceFactory;

/**
 * Initialize a git repository in the project root
 * 
 * @author keunecke
 */
public class GitInitSetupStep implements ProjectSetupStep {

    private static final ICommonLogger logger = LoggerFactory.getLogger(GitInitSetupStep.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault() != null ? Activator.getDefault().getLog() : null);
  
    @Override
    public void performStep(IProject project, InitialProjectConfigurationChoices choices) {
    	String remoteURI = calculateRemoteURI(project);
    	if (remoteURI == null ) {
			logger.info("Project name was not created according to the HIS naming convention" +
					"\nBecause the naming convention was violeted, no git repo will be created and no attempt" +  
					" will be made to connect to gilab.his.de!" + 
					"\nNaming convention can be found here: https://wiki.his.de/mediawiki/index.php/DV-Konzept_HISinOne-Extension#Namenskonvention");
    		return;
    	}
    	
    	IPath rawLocation = project.getFullPath();
    	File ws = WorkspaceFactory.getWorkspace().getRoot().getLocation().toFile();
    		
		try {
			//Git init
			Git gitRepo = Git.init()
					.setDirectory(new File(ws, rawLocation.lastSegment()))
					.call();
			
			//Add remote
			gitRepo.remoteAdd()
				.setUri(new URIish(remoteURI))
				.setName("origin")
				.call();
			
			//Associate local branch with remote branch to configure push/pull
			//Taken from here: https://stackoverflow.com/a/27825315
			StoredConfig config = gitRepo.getRepository().getConfig();
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master", "remote", "origin");
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, "master" , "merge", "refs/heads/master");
			config.save();
			
			//Pull
			gitRepo.pull().call();
			
			logger.info("Successfully pulled local git repo and associated the repo with the following remote address: " + remoteURI);
			
		} catch (GitAPIException | URISyntaxException | IOException  e) {
			logger.error2("Couldn't setup local git repo."
				+ "\nTherefore no git repo was created at all. Please setup git repo manually."
				+ "\nOne possible reason for this could be that the remote git repo does not exist (yet)."
				+ "\nThis was the exception: " + e.getCause(), e);
		} 
    	
    }

	private String calculateRemoteURI(IProject project) {
		String projectName = project.getName();
		String[] splittedSegments = projectName.split("\\.");
		if (splittedSegments.length < 3) {
			return null;
		}
		return "ssh://git@gitlab.his.de/h1/" + splittedSegments[0] + "/" + splittedSegments[1] + "/" + projectName;
	}
}
