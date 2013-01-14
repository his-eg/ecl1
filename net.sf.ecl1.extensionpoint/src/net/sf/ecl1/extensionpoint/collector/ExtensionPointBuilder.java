package net.sf.ecl1.extensionpoint.collector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.ecl1.extensionpoint.collector.manager.ExtensionPointManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Project Builder to collect and process extension point and contribution information
 *  
 * @author keunecke
 */
public class ExtensionPointBuilder extends IncrementalProjectBuilder {

    private static final String EXTENSION_EXTENDED_POINTS_PROPERTY = "extension.extended-points";

    private static final String EXTENSION_ANT_PROPERTIES_FILE = "extension.ant.properties";

    /** Builder ID constant */
    public static final String BUILDER_ID = "net.sf.ecl1.extensionpoint.extensionPointBuilder";

    private ExtensionPointVisitor visitor;

    protected IProject[] build(int kind, @SuppressWarnings("rawtypes")
    Map args, IProgressMonitor monitor)
			throws CoreException {
        visitor = new ExtensionPointVisitor(JavaCore.create(getProject()));
        ExtensionPointManager.get().clear();
        fullBuild(monitor);
        outputContributions();
		return null;
	}

    private void outputContributions() {
        try {
            Collection<String> contributors = visitor.getContributors();
            IFile file = getProject().getFile(EXTENSION_ANT_PROPERTIES_FILE);
            if (file.exists()) {
                String joined = Joiner.on(",").join(contributors);
                Properties props = new Properties();
                props.load(file.getContents());
                String oldContribs = props.getProperty(EXTENSION_EXTENDED_POINTS_PROPERTY);
                Iterable<String> split = Splitter.on(",").split(oldContribs);
                boolean contributionsChanged = this.haveContributionsChanged(contributors, split);
                if (contributionsChanged) {
                    props.remove(EXTENSION_EXTENDED_POINTS_PROPERTY);
                    props.setProperty(EXTENSION_EXTENDED_POINTS_PROPERTY, joined);
                    InputStream source = createNewContentForProperties(props);
                    file.setContents(source, IFile.FORCE, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private InputStream createNewContentForProperties(Properties props) {
        SortedMap<Object, Object> tree = convertToSortedMap(props);
        StringBuilder propStringBuilder = new StringBuilder();
        for (Object prop : tree.keySet()) {
            // build properties string
        }
        InputStream source = new ByteArrayInputStream(propStringBuilder.toString().getBytes());
        return source;
    }

    private SortedMap<Object, Object> convertToSortedMap(Properties props) {
        SortedMap<Object, Object> tree = new TreeMap<Object, Object>();
        for (Object prop : props.keySet()) {
            tree.put(prop, props.get(prop));
        }
        return tree;
    }

    private boolean haveContributionsChanged(Iterable<String> newContributors, Iterable<String> oldContributors) {
        List<String> newList = Lists.newArrayList(newContributors);
        List<String> oldList = Lists.newArrayList(oldContributors);
        return !oldList.equals(newList);
    }

    /**
     * Perform a full build
     * 
     * @param monitor
     * @throws CoreException
     */
	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
            getProject().accept(visitor);
		} catch (CoreException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
		}
	}

}
