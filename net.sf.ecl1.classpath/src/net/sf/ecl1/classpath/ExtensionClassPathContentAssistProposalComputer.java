/**
 *
 */
package net.sf.ecl1.classpath;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
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
        //TODO maybe implement init stuff
    }

    @Override
    public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext arg0, IProgressMonitor arg1) {
        try {
            CharSequence prefix = arg0.computeIdentifierPrefix();
        } catch (BadLocationException e) {
            errorMessage = e.getMessage();
        }
        return new ArrayList<ICompletionProposal>();
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
