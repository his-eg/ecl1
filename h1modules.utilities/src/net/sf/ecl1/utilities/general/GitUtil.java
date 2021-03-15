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

import net.sf.ecl1.utilities.hisinone.WebappsUtil;

public class GitUtil {

    /**
     * Starts from the supplied path and scans up through the parentdirectory tree until a Git repository is found.
     * 
     * @param absolutePath
     * @return
     */
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

}
