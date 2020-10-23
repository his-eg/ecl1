package net.sf.ecl1.commit.exporter.commitTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtil {

    //    private static final Logger logger = Logger.getGlobal();

    /**
     * 
     * If a git-repo is available in this workspace, this method returns its. 
     * 
     * @return
     * @throws IOException
     */
    @Deprecated
    public static Git getGit() {
        /* ----------------------
         * Locate and open git repo
         * ----------------------
         */
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        //        //BEGIN DEBUG PURPOSE
        //        SystemReader sr = SystemReader.getInstance();
        //        String output = sr.getenv(GIT_DIR_KEY);
        //        System.out.println("GIT_DIR: " + output);
        //        output = sr.getenv(GIT_WORK_TREE_KEY);
        //        System.out.println("GIT_WORK_TREE: " + output);
        //        //END DEBUG PURPOSE
        repoBuilder.readEnvironment();
        //        //BEGIN DEBUG PURPOSE
        //        output = new File("").getAbsoluteFile().toString();
        //        System.out.println("Start searching from the following path: " + output);
        //        //END DEBUG PURPOSE
        repoBuilder.findGitDir();
        repoBuilder.setMustExist(true);
        //        logger.info("Using the GIT-Repo at: " + repoBuilder.getGitDir().toString());
        Repository repo = null;
        try {
            repo = repoBuilder.build();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new Git(repo);
    }


    /**
     * Starts from the supplied file location and scans up through the parentdirectory tree until a Git repository is found.
     * 
     * @param iProject
     * @return
     */
    @Deprecated
    public static Git searchGitRepo(IProject iProject) {
        return searchGitRepo(iProject.getLocation().toString());
    }

    /**
     * Starts from the supplied path and scans up through the parentdirectory tree until a Git repository is found.
     * 
     * @param absolutePath
     * @return
     */
    public static Git searchGitRepo(String absolutePath) {
        File startPoint = new File(absolutePath);
        //System.out.println("Starting searching for git-repo at this location: " + startPoint.toString());
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        repoBuilder.findGitDir(startPoint);
        repoBuilder.setMustExist(true);
        Repository repo = null;
        try {
            repo = repoBuilder.build();
            //System.out.println("Found Git-Repo at: " + repo.getDirectory().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Git(repo);
    }



    public static List<String> getStagedChanges(Git git) {
        List<String> stagedChanges = new ArrayList<String>();
        Status status = null;
        try {
            status = git.status().call();
        } catch (NoWorkTreeException | GitAPIException e1) {
            e1.printStackTrace();
        }
        stagedChanges.addAll(status.getAdded());
        stagedChanges.addAll(status.getChanged());
        stagedChanges.addAll(status.getRemoved());
        return stagedChanges;
    }
}
