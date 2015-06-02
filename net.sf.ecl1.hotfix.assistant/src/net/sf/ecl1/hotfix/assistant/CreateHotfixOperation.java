package net.sf.ecl1.hotfix.assistant;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class CreateHotfixOperation extends SynchronizeModelOperation {

    private final ISynchronizePageConfiguration configuration;

    private final Collection<IDiffElement> elements;

    protected CreateHotfixOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
        super(configuration, elements);
        this.configuration = configuration;
        this.elements = Arrays.asList(elements);
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        System.out.println("CreateHotfixOperation.run");
    }

}
