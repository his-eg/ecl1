/**
 *
 */
package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * @author keunecke
 *
 */
public class ExtensionClassPathContentAssistProposalComputer implements IJavaCompletionProposalComputer {

    private String errorMessage = "";

    public ExtensionClassPathContentAssistProposalComputer() {
        // nop
    }

    @Override
    public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
        ArrayList<ICompletionProposal> result = new ArrayList<ICompletionProposal>();
        try {
            if (arg0 instanceof JavaContentAssistInvocationContext) {
                JavaContentAssistInvocationContext c = (JavaContentAssistInvocationContext) arg0;
                Map<String, IJavaCompletionProposal> map = computePossibleProposals(c.getProject());
                CharSequence prefix = c.computeIdentifierPrefix();
                System.out.println(prefix);
            }
        } catch (BadLocationException e) {
            errorMessage = e.getMessage();
        }
        return result;
    }

    private Map<String, IJavaCompletionProposal> computePossibleProposals(IJavaProject project) {
        Map<String, IJavaCompletionProposal> result = new HashMap<String, IJavaCompletionProposal>();
        try {
            IClasspathContainer container = JavaCore.getClasspathContainer(new Path(ExtensionClassPathContainer.NET_SF_ECL1_ECL1_CONTAINER_ID), project);
            IClasspathEntry[] entries = container.getClasspathEntries();
            System.out.println(entries);
            for (IClasspathEntry cpe : entries) {
                scanForClasses(cpe);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void scanForClasses(IClasspathEntry cpe) {
        if (cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
            scanLibraryEntry(cpe);
        }
        if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
            scanProjectEntry(cpe);
        }
    }

    private void scanLibraryEntry(IClasspathEntry cpe) {
        // TODO Auto-generated method stub

    }

    private void scanProjectEntry(IClasspathEntry cpe) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
        return new ArrayList<IContextInformation>();
    }

    @Override
    public String getErrorMessage() {
        if (!errorMessage.isEmpty()) {
            return errorMessage;
        }
        return ExtensionClassPathContentAssistProposalComputer.class.getSimpleName();
    }

    @Override
    public void sessionEnded() {

    }

    @Override
    public void sessionStarted() {

    }

}
