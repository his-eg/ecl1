package net.sf.ecl1.changeset.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Sets;

/**
 * The activator class controls the plug-in life cycle
 */
public class ChangeSetExportWizardPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "net.sf.ecl1.changeset.exporter"; //$NON-NLS-1$

    // The shared instance
    private static ChangeSetExportWizardPlugin plugin;

    /**
     * The constructor
     */
    public ChangeSetExportWizardPlugin() {
        // nop
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static ChangeSetExportWizardPlugin getDefault() {
        return plugin;
    }

    /**
     * Retrieve all present change sets
     *
     * @return all user defined change sets
     */
    public Collection<ChangeSet> getChangeSets() {
        Collection<ChangeSet> changeSets = Sets.newHashSet();
        ActiveChangeSetManager manager = CVSUIPlugin.getPlugin().getChangeSetManager();
        List<ChangeSet> changeSetsFromManager = Arrays.asList(manager.getSets());
        changeSets.addAll(changeSetsFromManager);
        return changeSets;
    }

    /**
     * Find release xml files in workspace
     * @return
     */
    public static Collection<IFile> getReleaseXmlFiles() {
        Collection<IFile> result = Sets.newHashSet();
        IFolder releaseFilesFolder = getReleaseXmlFolder();
        if (releaseFilesFolder.exists()) {
            try {
                List<IResource> releaseFileResources = Arrays.asList(releaseFilesFolder.members());
                for (IResource releaseFileResource : releaseFileResources) {
                    result.add((IFile) releaseFileResource);
                }
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * @param webapps
     * @return
     */
    private static IFolder getReleaseXmlFolder() {
        return getWebapps().getFolder("qisserver/WEB-INF/conf/service/patches/hisinone");
    }


    /**
     * Get a specific release.xml file
     *
     * @param file name
     * @return IFile
     */
    public static IFile getReleaseXmlFile(String file) {
        IFile releaseFile = getReleaseXmlFolder().getFile(file);
        if(releaseFile.exists()){
            return releaseFile;
        }
        return null;
    }

    /**
     * Utility method for determining webapps project branch and respective version number
     *
     * examples:
     * - HISinOne_VERSION_07 --> 7.0
     * - HISinOne_VERSION_06_RELEASE_01 --> 6.1
     * @return
     */
    public static String getVersionShortString() {
        IProject webapps = getWebapps();
        IFile file = webapps.getFile("CVS/Tag");
        if (!file.exists()) {
            return "HEAD";
        }
        String contents = readContent(file);
        if (contents != null) {
            String reducedBranch = contents.trim().replace("THISinOne_", "");
            List<String> branchNameComponents = Arrays.asList(reducedBranch.split("_"));
            String majorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(1)));
            String minorVersion = "0";
            if (branchNameComponents.size() > 2) {
                minorVersion = Integer.toString(Integer.parseInt(branchNameComponents.get(3)));
            }
            return majorVersion + "." + minorVersion;
        }
        return "UNKNOWN_VERSION";
    }

    private static String readContent(IFile file) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(file.getLocationURI()));
            return new String(encoded, Charset.defaultCharset()).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static IProject getWebapps() {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        List<IProject> projects = Arrays.asList(ws.getRoot().getProjects());
        for (IProject project : projects) {
            if (isWebapps(project)) {
                return project;
            }
        }
        return null;
    }

    private static boolean isWebapps(IProject project) {
        return project.exists(new Path("qisserver"));
    }

}
