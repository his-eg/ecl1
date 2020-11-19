package net.sf.ecl1.commit.exporter.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
 * This class computes the addedOrModified-list and the removed-list from the user checked commits. 
 * 
 * @author sohrt
 */
public class DiffTool {

    /**
     * All files that have been added or modified by the user checked commits. 
     */
	private Set<String> addedOrModifiedFiles = new TreeSet<>();
	
	/** Deletes files by user checked commits. */
	private Set<String> deletedFiles = new TreeSet<>();
	
	private boolean diffComputed = false;
	
	
	public Set<String> getAddedOrModifiedFiles() {
		if(diffComputed) {
			return addedOrModifiedFiles;
		} else {
			return new TreeSet<String>();
		}
	}

	public Set<String> getDeletedFiles() {
		if(diffComputed) {
			return deletedFiles;
		} else {
			return new TreeSet<String>();
		}
	}

	/**
	 * This method returns all files that have been added or modified by the given RevCommits/StagedChanges.
	 * 
     * his method expects within the given Object[]-array either RevCommits or StagedChanges. 
	 * 
	 * @param checkedElements must by either RevCommits or StagedChanges
	 * @param git The git-repo the RevCommits or StagedChanges belong to
	 */
	public void computeDiff(Object[] checkedElements, Git git) {
		/*
		 * Delete previous diff
		 * 
	     */
	    addedOrModifiedFiles = new TreeSet<>();
	    deletedFiles = new TreeSet<String>();
	
	    if (checkedElements.length == 0) {
	        return;
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
	    computeCommits(checkedCommits, git.getRepository());
	
	
	    /*
	     * Handle StagedChanges.
	     */
	    if (stagedChanges) {
	        computeStagedChanges(git);
	    }
	
	    
	
	    
	    /*
	     * This does two things: 
	     * 1. Every file that does not start with qisserver is removed from the list. 
	     * 2. Strip qisserver/ from the start of every remaining file.
	     */
	    String qisserver = "qisserver/";
	    addedOrModifiedFiles = addedOrModifiedFiles.stream().filter(s -> s.startsWith(qisserver))
	    		.map(s -> s.substring(qisserver.length()))
	    		.collect(Collectors.toCollection(TreeSet::new));
	    
	    deletedFiles = deletedFiles.stream().filter(s -> s.startsWith(qisserver))
	    		.map(s -> s.substring(qisserver.length()))
	    		.collect(Collectors.toCollection(TreeSet::new));
	    
		this.diffComputed = true;
	
	}

	/**
	 * Computes all files that have been modified/added by the given commits and
	 * all files that haven been deleted. 
	 * 
	 * Since a commit is just a description of a state of the repository, a commit always needs another commit to create a diff. 
	 * This method always makes a diff against the parent(s) of the commits. 
	 * 
	 * Goes through the commits in chronological order. 
	 * 
	 * If a file is deleted by a later commit, the file is deleted from the resulting file set. The deletion is
	 * also saved in this object. 
	 * 
	 * @param commits This method expects that the commits are given in reverse chronological order (latest file first)
	 * @param repo
	 */
	private void computeCommits(List<RevCommit> commits, Repository repo) {
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
	                        this.addedOrModifiedFiles.add(treeWalk.getPathString());
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
	                    	/*
	                    	 * This if-clause prevents removedEntries to show up in the final result, 
	                    	 * if they only existed within the git range that the user has checked.
	                    	 * 
	                    	 * An example: 
	                    	 * If I created a file in a hotfix-commit but then realized I don't need
	                    	 * this file in the next hotfix-commit anymore, the file is excluded from the resultSet. 
	                    	 * The file only shortly existed during the hotfix-creation-phase, but was not part of the 
	                    	 * hotfix that was eventually delivered to the customers. 
	                    	 * 
	                    	 */
	                    	if(this.addedOrModifiedFiles.remove(entry.getOldPath()) == false) {
	                    		this.deletedFiles.add(entry.getOldPath());
	                    	}
	                    } else {
	                    	this.addedOrModifiedFiles.add(entry.getNewPath());
	                    }
	
	                }
	
	            }
	
	        }
	
	    }
	    df.close();
	}

	/**
	 * Computes all files that have been modified/added by StagedChanges and
	 * all files that haven been deleted. 
	 * 
	 * 
	 * @param git
	 */
	private void computeStagedChanges(Git git) {
	
	    List<DiffEntry> stagedChanges = null;
	    try {
	        stagedChanges = git.diff().setCached(true).call();
	    } catch (GitAPIException e) {
	        e.printStackTrace();
	    }
	    if (stagedChanges == null) {
	        return;
	    }
	    
	    for (DiffEntry e : stagedChanges) {
	        //Note: Deleted files have dev/null as their new path.
	        if (e.getNewPath() == DiffEntry.DEV_NULL) {
	        	if(addedOrModifiedFiles.remove(e.getOldPath()) == false) {
	        		this.deletedFiles.add(e.getOldPath());
	        	}
	        } else {
	            addedOrModifiedFiles.add(e.getNewPath());
	        }
	    }

	}

}
