package net.sf.ecl1.commit.exporter.playground;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sf.ecl1.commit.exporter.commitTable.CommitTableFactory;
import net.sf.ecl1.commit.exporter.commitTable.GitUtil;
import net.sf.ecl1.commit.exporter.commitTable.SelectableRevCommit;



/**
 * 
 * The purpose of this class is to speed up the development of the commit exporter. 
 * 
 * How does this work?
 * Starting an eclipse instance with new changes in the commitExporter-Plugins takes a long time ( >1min ), because
 * the new eclipse instance starts in debug mode. 
 * 
 * Starting this class as a normal Java app is instantaneous. 
 * 
 * I commited this class, because it might be valuable to other people as well. 
 * 
 * 
 * @author sohrt@his.de
 *
 */
public class Playground {


    
    
    //private static Logger logger = Logger.getLogger(Playground.class.getName());

    private static final Logger logger = Logger.getGlobal();

    public static void main(String[] args) throws NoHeadException, GitAPIException, IOException {





        /* ----------------------
         * Greate a display and a shell
         * ----------------------
         */

        Display display = new Display();

        Shell shell = new Shell(display);

        // the layout manager handle the layout
        // of the widgets in the container
        //        shell.setLayout(new GridLayout(2, true));
        shell.setLayout(new FillLayout());


        //        Button test = new Button(shell, SWT.PUSH | SWT.TOP);
        //        test.setText("BLAB");


        /* ----------------------
         * Create the table that displays everything
         * ----------------------
         */

        TableViewer tableViewer = CommitTableFactory.createCommitTable(shell);
        
        

        /* ----------------------
         * Set the content provider for the table
         * ----------------------
         */
        //We choose an ArrayContentProvider, because our data is not mutable
        tableViewer.setContentProvider(new ArrayContentProvider());

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        tableViewer.getTable().setLayoutData(gridData);

        //TODO: Must later be replaced a something that does not collect all commits, but only when called (lazy)
        List<SelectableRevCommit> allCommits = new ArrayList<>();
        for (RevCommit r : GitUtil.getGit().log().all().call()) {
            allCommits.add(new SelectableRevCommit(r));
        }
        tableViewer.setInput(allCommits);


        
        /* ----------------------
         * Create button that exports all selected commits to log
         * ----------------------
         */

        Button exportButton = new Button(shell, SWT.PUSH);
        exportButton.setText("Export all selected commits to console");
        exportButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    printChangedFiles(allCommits, GitUtil.getGit().getRepository());
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                //                int ctr = 0;
                //                for (SelectableRevCommit c : allCommits) {
                //                    if (c.isSelected()) {
                //                        //printRevCommit(c.getCommit());
                //                        try {
                //                            printChangedFiles(c.getCommit(), getGit().getRepository());
                //                        } catch (IOException e1) {
                //                            // TODO Auto-generated catch block
                //                            e1.printStackTrace();
                //                        }
                //                        ctr++;
                //                    }
                //                }
                //                System.out.println("Printed the following number of commits: " + ctr);

            }



            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
            }
        });

        
        //        Button bla = new Button(shell, SWT.PUSH);
        //        bla.setText("BABDASFSAFDSAFDDF DF DF");

        /* ----------------------
         * This reads commands from the os until the shell is closed
         * ----------------------
         */

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();

        //printAllGitCommits();
    }




    private static void printChangedFiles(List<SelectableRevCommit> allCommits, Repository repo) throws IncorrectObjectTypeException, IOException {
        /**
         * All files that show up in the commits. A file shows up in a commit, if 
         * it was added / deleted / moved etc. 
         * 
         * This is a set to avoid duplicates if files have been touched by multiple commits. 
         */
        Set<String> touchedFiles = new HashSet<>();
        
        ObjectReader reader = repo.newObjectReader();
        CanonicalTreeParser currentTreeIter = new CanonicalTreeParser();
        CanonicalTreeParser parentTreeIter = new CanonicalTreeParser();

        DiffFormatter df = new DiffFormatter(new ByteArrayOutputStream());
        df.setRepository(repo);

        for (SelectableRevCommit sr : allCommits) {
            if (sr.isSelected()) {
                RevCommit r = sr.getCommit();
                RevCommit parent = r.getParent(0); //Might be problematic, when there are more than two parents...

                RevTree currentTreeId = r.getTree();
                RevTree parentTreeId = parent.getTree();

                currentTreeIter.reset(reader, currentTreeId);
                parentTreeIter.reset(reader, parentTreeId);

                List<DiffEntry> entries = df.scan(parentTreeIter, currentTreeIter);
                df.close();

                for (DiffEntry entry : entries) {
                    touchedFiles.add(entry.getNewPath());
                }

            }
        }

        for (String file : touchedFiles) {
            System.out.println(file);
        }

        df.close();
        
    }




    public static void printRevCommit(RevCommit c) {
        System.out.println(c.getId().abbreviate(7).name() + " | " + c.getShortMessage() + " | " + c.getAuthorIdent().getEmailAddress());
    }

    public static void printAllGitCommits() throws NoHeadException, GitAPIException, IOException {
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


        /* ----------------------
         * Print all commits to console
         * ----------------------
         */
        Git git = new Git(repo);
        Iterable<RevCommit> commits = git.log().all().call();
        int count = 0;
        for (RevCommit commit : commits) {
            System.out.println(commit.getId().abbreviate(7).name() + " | " + commit.getShortMessage() + " | " + commit.getAuthorIdent().getEmailAddress());
            count++;
        }
        System.out.println("---------------------------");
        System.out.println(count + " commits in the repository.");
        System.out.println("---------------------------");



        /* ----------------------
         * Access staged changes
         * ----------------------
         */
        Status status = git.status().call();

        Set<String> added = status.getAdded();
        for (String add : added) {
            System.out.println("Added: " + add);
        }

        Set<String> changed = status.getChanged();
        for (String change : changed) {
            System.out.println("Change: " + change);
        }

        git.close();
    }

}
