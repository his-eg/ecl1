package net.sf.ecl1.commit.exporter.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * 
 * Util class for creating diffs from commits (and possibly from StagedChanges)
 * 
 * @author sohrt
 */
public class DiffUtil {

	/**
	 * This method expects within the given Object[]-array either RevCommits or StagedChanges. 
	 * 
	 * This method returns all files that have been added or modified by the given RevCommits/StagedChanges.  
	 * 
	 * @param checkedElements
	 * @param git
	 * @return
	 */
	public static List<String> getAddedOrModifiedFiles(Object[] checkedElements, Git git) {
	    /*
	     * Sorting with the Collator is necessary, because otherwise the strings would be
	     * sorted by their unicode value. This would result in unexpected behavior, since every CAPITAL letter
	     * has a lower unicode value than every lower-case letter. This means
	     * that a file named e.java is sorted after W.java, which is something a human would not expect
	     * (or at least I don't expect it). By using the Collator, the strings are sorted as
	     * most humans would expect them to be (this means that basically the case is ignored).
	     */
	    Set<String> addedOrModifiedFiles = new TreeSet<>(Collator.getInstance());
	
	    if (checkedElements.length == 0) {
	        return new ArrayList<String>();
	    }
	
	    /*
	     * Check if StagedChanges have been checked by the user
	     * 
	     */
	    int i = 0;
	    boolean stagedChanges = false;
	    if (checkedElements[0] instanceof StagedChanges) {
	        stagedChanges = true;
	        i = 1; //Skip the first element in the 'handle commits' step, because it is of the type 'StagedChanges'
	    }
	
	    /* 
	     * Handle commits 
	     */
	    List<RevCommit> checkedCommits = new ArrayList<RevCommit>();
	    for (; i < checkedElements.length; i++) {
	        checkedCommits.add((RevCommit) checkedElements[i]);
	    }
	    addedOrModifiedFiles.addAll(DiffUtil.getAddedOrModifiedFilesByCommits(checkedCommits, git.getRepository()));
	
	
	    /*
	     * Handle StagedChanges.
	     */
	    if (stagedChanges) {
	        addedOrModifiedFiles = DiffUtil.getAddedOrModifiedFilesByStagedChanges(git, addedOrModifiedFiles);
	    }
	
	    
	
	    
	    /*
	     * Every file that does not start with qisserver is removed from the list. 
	     * Strip qisserver/ from the start of every remaining file.
	     */
	    String qisserver = "qisserver/";
	    List<String> returnList = addedOrModifiedFiles.stream().filter(s -> s.startsWith(qisserver)).map(s -> s.substring(qisserver.length())).collect(Collectors.toList());
	    
	    return returnList;
	
	}

	/**
	 * Returns a list of files that have been modified/added by the given commits. 
	 * Since a commit is just a description of a state of the repository, a commit always needs another commit to create a diff. 
	 * This method always makes a diff against the parent(s) of the commits. 
	 * 
	 * Goes through the commits in chronological order. 
	 * 
	 * If a file is deleted by a later commit, the file is deleted from the resulting file set. 
	 * 
	 * @param commits This method expects that the commits are given in reverse chronological order (latest file first)
	 * @param repo
	 * @return
	 */
	public static Set<String> getAddedOrModifiedFilesByCommits(List<RevCommit> commits, Repository repo) {
	
	    //Note: A TreeSet is used here to automatically sort the filenames alphabetically. 
	    Set<String> addedOrChangedFiles = new TreeSet<>();
	
	    //Sort commits in chronological order (oldest first)
	    Collections.reverse(commits);
	
	    ObjectReader reader = repo.newObjectReader();
	    CanonicalTreeParser currentTreeIter = new CanonicalTreeParser();
	    CanonicalTreeParser parentTreeIter = new CanonicalTreeParser();
	
	    DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
	    df.setRepository(repo);
	
	
	    RevTree currentTreeId;
	
	
	    for (RevCommit r : commits) {
	        currentTreeId = r.getTree();
	        /*
	         * Special case: The initial commit has no parents. Instead of creating a diff between
	         * the current commit and its parent, we simply print out all files associated with the
	         * tree of this commit. 
	         */
	        if (r.getParentCount() == 0) {
	            /*
	             * Taken from here 
	             * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/GetFileAttributes.java
	             * and
	             * https://git-scm.com/docs/git-ls-tree
	             * and
	             * https://stackoverflow.com/questions/19941597/use-jgit-treewalk-to-list-files-and-folders
	             */
	            TreeWalk treeWalk = new TreeWalk(repo);
	            try {
	                treeWalk.addTree(currentTreeId);
	                treeWalk.setRecursive(false);
	                while (treeWalk.next()) {
	                    if (treeWalk.isSubtree()) {
	                        treeWalk.enterSubtree();
	                    } else {
	                        addedOrChangedFiles.add(treeWalk.getPathString());
	                    }
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	            treeWalk.close();
	
	        } else {
	            /*
	             * 'Normal' case, if a commit has at least one parent. Create the diffs against all parents of this commit.
	             * Idea from here: 
	             * https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowBranchDiff.java
	             * 
	             */
	            RevCommit[] parents = r.getParents();
	            for (int i = 0; i < parents.length; i++) {
	                RevCommit parent = r.getParent(i);
	                RevTree parentTreeId = parent.getTree();
	
	                List<DiffEntry> entries = null;
	
	                //Get all DiffEntries between the current commit and this parent
	                try {
	                    currentTreeIter.reset(reader, currentTreeId);
	                    parentTreeIter.reset(reader, parentTreeId);
	                    entries = df.scan(parentTreeIter, currentTreeIter);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	
	
	                for (DiffEntry entry : entries) {
	
	                    //Note: Deleted files have dev/null as their new path.
	                    if (entry.getNewPath() == DiffEntry.DEV_NULL) {
	                        addedOrChangedFiles.remove(entry.getOldPath());
	                    } else {
	                        addedOrChangedFiles.add(entry.getNewPath());
	                    }
	
	                }
	
	            }
	
	        }
	
	    }
	    df.close();
	    return addedOrChangedFiles;
	}

	/**
	 * Modifies the given set of files by taking into account changes from the StagedChanges. 
	 * Returns the modified set after completing the modifications. 
	 * 
	 * Here's what happens: 
	 * Every file that was added or modified in the StagedChanges is added to the set. 
	 * Every file that was removed in the StagedChanges is removed from the set. 
	 * 
	 * @param git
	 * @param files
	 * @return
	 */
	public static Set<String> getAddedOrModifiedFilesByStagedChanges(Git git, Set<String> files) {
	
	    List<DiffEntry> stagedChanges = null;
	    try {
	        stagedChanges = git.diff().setCached(true).call();
	    } catch (GitAPIException e) {
	        e.printStackTrace();
	    }
	    if (stagedChanges == null) {
	        return files;
	    }
	    
	    for (DiffEntry e : stagedChanges) {
	        //Note: Deleted files have dev/null as their new path.
	        if (e.getNewPath() == DiffEntry.DEV_NULL) {
	            files.remove(e.getOldPath());
	        } else {
	            files.add(e.getNewPath());
	        }
	    }
	    return files;
	}

}