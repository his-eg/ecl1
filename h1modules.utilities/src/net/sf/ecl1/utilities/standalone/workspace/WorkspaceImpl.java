package net.sf.ecl1.utilities.standalone.workspace;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;


public class WorkspaceImpl implements IWorkspace {

    protected static final int TYPE_FILE = 0x1;
    protected static final int TYPE_FOLDER = 0x2;
    protected static final int TYPE_PROJECT = 0x4;
    protected static final int TYPE_ROOT = 0x8;

  
    private final Path customWorkspacePath;

    public WorkspaceImpl(){
        customWorkspacePath = null;
    }

    /** Constructor to set custom workspace path */
    public WorkspaceImpl(Path path){
        customWorkspacePath = path;
    }

    @Override
    public IWorkspaceRoot getRoot() {
        if(customWorkspacePath == null){
            return new WorkspaceRootImpl();
        }
        return new WorkspaceRootImpl(customWorkspacePath);
    }

    @Override
    public void setDescription(IWorkspaceDescription description) {
        throw new UnsupportedOperationException("Unimplemented method 'setDescription(IWorkspaceDescription description)'");
    }

    @Override
    public IWorkspaceDescription getDescription() {
        throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
    }

    @Override
    public IProjectDescription newProjectDescription(String projectName) {
        throw new UnsupportedOperationException("Unimplemented method 'newProjectDescription(String projectName)'");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new UnsupportedOperationException("Unimplemented method 'getAdapter(Class<T> adapter)'");
    }
    
    @Override
    public void addResourceChangeListener(IResourceChangeListener listener) {
        throw new UnsupportedOperationException("Unimplemented method 'addResourceChangeListener(IResourceChangeListener listener)'");
    }
    
    @Override
    public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
        throw new UnsupportedOperationException("Unimplemented method 'addResourceChangeListener(IResourceChangeListener listener, int eventMask)'");
    }
    
    @Override
    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'addSaveParticipant(Plugin plugin, ISaveParticipant participant)'");
    }
    
    @Override
    public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'addSaveParticipant(String pluginId, ISaveParticipant participant)'");
    }
    
    @Override
    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'build(int kind, IProgressMonitor monitor)'");
    }
    
    @Override
    public void build(IBuildConfiguration[] buildConfigs, int kind, boolean buildReferences, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'build(IBuildConfiguration[] buildConfigs, int kind, boolean buildReferences, IProgressMonitor monitor)'");
    }
    
    @Override
    public void checkpoint(boolean build) {
        throw new UnsupportedOperationException("Unimplemented method 'checkpoint(boolean build)'");
    }
    
    @Override
    public IProject[][] computePrerequisiteOrder(IProject[] projects) {
        throw new UnsupportedOperationException("Unimplemented method 'computePrerequisiteOrder(IProject[] projects)'");
    }
    
    @Override
    public ProjectOrder computeProjectOrder(IProject[] projects) {
        throw new UnsupportedOperationException("Unimplemented method 'computeProjectOrder(IProject[] projects)'");
    }
    
    @Override
    public IStatus copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public IStatus copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(IResource[] resources, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(IResource[] resources, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void deleteMarkers(IMarker[] markers) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteMarkers(IMarker[] markers)'");
    }
    
    @Override
    public void forgetSavedTree(String pluginId) {
        throw new UnsupportedOperationException("Unimplemented method 'forgetSavedTree(String pluginId)'");
    }
    
    @Override
    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        throw new UnsupportedOperationException("Unimplemented method 'getFilterMatcherDescriptors()'");
    }
    
    @Override
    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
        throw new UnsupportedOperationException("Unimplemented method 'getFilterMatcherDescriptor(String filterMatcherId)'");
    }
    
    @Override
    public IProjectNatureDescriptor[] getNatureDescriptors() {
        throw new UnsupportedOperationException("Unimplemented method 'getNatureDescriptors()'");
    }
    
    @Override
    public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
        throw new UnsupportedOperationException("Unimplemented method 'getNatureDescriptor(String natureId)'");
    }
    
    @Override
    public Map<IProject, IProject[]> getDanglingReferences() {
        throw new UnsupportedOperationException("Unimplemented method 'getDanglingReferences()'");
    }
    
    @Override
    public IResourceRuleFactory getRuleFactory() {
        throw new UnsupportedOperationException("Unimplemented method 'getRuleFactory()'");
    }
    
    @Override
    public ISynchronizer getSynchronizer() {
        throw new UnsupportedOperationException("Unimplemented method 'getSynchronizer()'");
    }
    
    @Override
    public boolean isAutoBuilding() {
        throw new UnsupportedOperationException("Unimplemented method 'isAutoBuilding()'");
    }
    
    @Override
    public boolean isTreeLocked() {
        throw new UnsupportedOperationException("Unimplemented method 'isTreeLocked()'");
    }
    
    @Override
    public IProjectDescription loadProjectDescription(IPath projectDescriptionFile) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'loadProjectDescription(IPath projectDescriptionFile)'");
    }
    
    @Override
    public IProjectDescription loadProjectDescription(InputStream projectDescriptionFile) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'loadProjectDescription(InputStream projectDescriptionFile)'");
    }
    
    @Override
    public IStatus move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public IStatus move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public IBuildConfiguration newBuildConfig(String projectName, String configName) {
        throw new UnsupportedOperationException("Unimplemented method 'newBuildConfig(String projectName, String configName)'");
    }
    
    @Override
    public void removeResourceChangeListener(IResourceChangeListener listener) {
        throw new UnsupportedOperationException("Unimplemented method 'removeResourceChangeListener(IResourceChangeListener listener)'");
    }
    
    @Override
    public void removeSaveParticipant(Plugin plugin) {
        throw new UnsupportedOperationException("Unimplemented method 'removeSaveParticipant(Plugin plugin)'");
    }
    
    @Override
    public void removeSaveParticipant(String pluginId) {
        throw new UnsupportedOperationException("Unimplemented method 'removeSaveParticipant(String pluginId)'");
    }
    
    @Override
    public void run(ICoreRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'run(ICoreRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void run(ICoreRunnable action, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'run(ICoreRunnable action, IProgressMonitor monitor)'");
    }
    
    @Override
    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'run(IWorkspaceRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'run(IWorkspaceRunnable action, IProgressMonitor monitor)'");
    }
    
    @Override
    public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'save(boolean full, IProgressMonitor monitor)'");
    }
    
    @Override
    public String[] sortNatureSet(String[] natureIds) {
        throw new UnsupportedOperationException("Unimplemented method 'sortNatureSet(String[] natureIds)'");
    }
    
    @Override
    public IStatus validateEdit(IFile[] files, Object context) {
        throw new UnsupportedOperationException("Unimplemented method 'validateEdit(IFile[] files, Object context)'");
    }
    
    @Override
    public IStatus validateFiltered(IResource resource) {
        throw new UnsupportedOperationException("Unimplemented method 'validateFiltered(IResource resource)'");
    }
    
    @Override
    public IStatus validateLinkLocation(IResource resource, IPath location) {
        throw new UnsupportedOperationException("Unimplemented method 'validateLinkLocation(IResource resource, IPath location)'");
    }
    
    @Override
    public IStatus validateLinkLocationURI(IResource resource, URI location) {
        throw new UnsupportedOperationException("Unimplemented method 'validateLinkLocationURI(IResource resource, URI location)'");
    }
    
    @Override
    public IStatus validateName(String segment, int typeMask) {
        throw new UnsupportedOperationException("Unimplemented method 'validateName(String segment, int typeMask)'");
    }
    
    @Override
    public IStatus validateNatureSet(String[] natureIds) {
        throw new UnsupportedOperationException("Unimplemented method 'validateNatureSet(String[] natureIds)'");
    }
    
    @Override
    public IStatus validatePath(String path, int typeMask) {
        throw new UnsupportedOperationException("Unimplemented method 'validatePath(String path, int typeMask)'");
    }
    
    @Override
    public IStatus validateProjectLocation(IProject project, IPath location) {
        throw new UnsupportedOperationException("Unimplemented method 'validateProjectLocation(IProject project, IPath location)'");
    }
    
    @Override
    public IStatus validateProjectLocationURI(IProject project, URI location) {
        throw new UnsupportedOperationException("Unimplemented method 'validateProjectLocationURI(IProject project, URI location)'");
    }
    
    @Override
    public IPathVariableManager getPathVariableManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getPathVariableManager()'");
    }
}