package net.sf.ecl1.utilities.general;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import net.sf.ecl1.utilities.Activator;
import net.sf.ecl1.utilities.hisinone.WebappsUtil;
import net.sf.ecl1.utilities.logging.ICommonLogger;
import net.sf.ecl1.utilities.logging.LoggerFactory;

public class GitUtil {

    private static final ICommonLogger logger = LoggerFactory.getLogger(GitUtil.class.getSimpleName(), Activator.PLUGIN_ID, Activator.getDefault());

	/**
     * Starts from the supplied path and scans up through the parentdirectory tree until a Git repository is found.
     * <br><br>
     * WARNING: 
     * As discovered in ticket #256448 this method does not work, if it is
     * executed in a directory that was created with the "git worktree" command. 
     * Therefore this method should no longer be used. 
     * <br><br>
     * Use {@link org.eclipse.jgit.api.Git#open(File)} instead
     * 
     * @param absolutePath
     * @return
     */
    @Deprecated
	public static Git searchGitRepo(String absolutePath) {
        File startPoint = new File(absolutePath);
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        repoBuilder.findGitDir(startPoint);
        if(repoBuilder.getGitDir() == null) {
        	//Could not find gitdir...
        	return null;
        }
        repoBuilder.setMustExist(true);
        Repository repo = null;
        try {
            repo = repoBuilder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Git(repo);
    }   
    
    	

    /**
     * Returns the last i commits of this repository. 
     * The output is given in reverse chronological order.
     * 
     * @see <a href="https://git-scm.com/docs/git-log">https://git-scm.com/docs/git-log</a>
     * 
     * 
     * @param git
     * @return
     */
    public static List<RevCommit> getLastCommits(Git git, int i) {
        List<RevCommit> commits = new ArrayList<RevCommit>();
        try {
            for (RevCommit r : git.log().setMaxCount(i).call()) {
                commits.add(r);
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return commits;
    }

    /**
     * 
     * Returns all commits of this repository. 
     * The output is given in reverse chronological order.
     * 
     * Warning: Might be slow for big repositories. 
     * 
     * @param git
     * @return
     */
    public static List<RevCommit> getAllCommits(Git git) {
        List<RevCommit> commits = new ArrayList<RevCommit>();
        try {
            for (RevCommit r : git.log().all().call()) {
                commits.add(r);
            }
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }
        return commits;
    }

	public static final String UNKNOWN_BRANCH = "Unknown_branch";
	public static final String MASTER = "master";
	/** The local master branch is called HEAD on the remote jenkins server */
	public static final String REMOTE_ALIAS_FOR_LOCAL_MASTER_BRANCH = "HEAD";

	public static String getCheckedOutBranchOfWebapps() {
	        IProject webappsProject = WebappsUtil.findWebappsProject();
	        if(webappsProject != null) {
	        	String webappsPath = webappsProject.getLocation().toString();
	            try (Git git = Git.open(new File(webappsPath))){
					String branch = git.getRepository().getFullBranch();
					//Remove "refs/heads/" from branch name
					branch = branch.substring(branch.lastIndexOf("/")+1);
					if(branch.equals(MASTER)) {
						branch = REMOTE_ALIAS_FOR_LOCAL_MASTER_BRANCH;
					}
					return branch;
				} catch (IOException e) {
					logger.info("Could not open git repository. Therefore I could not determine the branch of the repository.\n "
							+ "Exception was: " + e.getMessage());
			        return UNKNOWN_BRANCH;
				}
	        }
	        return UNKNOWN_BRANCH;
		}

}
