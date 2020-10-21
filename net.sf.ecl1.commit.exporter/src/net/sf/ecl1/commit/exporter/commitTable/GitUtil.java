package net.sf.ecl1.commit.exporter.commitTable;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtil {

    private static final Logger logger = Logger.getGlobal();

    /**
     * 
     * If a git-repo is available in this workspace, this method returns its. 
     * 
     * @return
     * @throws IOException
     */
    public static Git getGit() throws IOException {
        /* ----------------------
         * Locate and open git repo
         * ----------------------
         */
        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
        repoBuilder.readEnvironment();
        repoBuilder.findGitDir();
        repoBuilder.setMustExist(true);
        logger.info("Using the GIT-Repo at: " + repoBuilder.getGitDir().toString());
        Repository repo = repoBuilder.build();
        return new Git(repo);
    }
}
