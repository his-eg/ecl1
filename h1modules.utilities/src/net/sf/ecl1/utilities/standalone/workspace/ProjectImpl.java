package net.sf.ecl1.utilities.standalone.workspace;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import net.sf.ecl1.utilities.hisinone.WebappsUtil;

public class ProjectImpl implements IProject{

    private final String name;
    private final Path projectPath;

    public ProjectImpl(String name, IPath workspacePath) {
        this.name = name;
        this.projectPath = Paths.get(workspacePath.toOSString(), name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IFolder getFolder(String name) {
        // dont resolve absolute paths
        if(name.startsWith("/") || name.startsWith("\\")){
            name = name.substring(1);
        }
        return new FolderImpl(projectPath.resolve(name));
    }

    @Override
    public IPath getLocation() {
        return new PathImpl(projectPath);
    }

    @Override
    public boolean exists(IPath path) {
        return Files.exists(projectPath.resolve(path.toString()));
    }

    @Override
    public boolean exists() {
        return Files.exists(projectPath);
    }

    @Override
    public IWorkspace getWorkspace() {
        // always returns default workspace
        return new WorkspaceImpl();
    }

    @Override
    public int getType() {
        return WorkspaceImpl.TYPE_PROJECT;
    }

    @Override
    public IFile getFile(IPath path) {
        return new FileImpl(projectPath.resolve(path.toString()));
    }

    @Override
    public IFile getFile(String name) {
        return new FileImpl(projectPath.resolve(name));
    }

    @Override
    public IFolder getFolder(IPath path) {
        return new FolderImpl(projectPath.resolve(path.toString()));
    }

    @Override
    public IPath getFullPath() {
        return getFullPath(projectPath.toString());
    }

    protected static IPath getFullPath(String path){
        Path workspacePath = Paths.get(new WorkspaceRootImpl().getLocation().toString());
        Path projectPath = Paths.get(path);
        // standalone only knows about projects in workspace-folder, handle webapps manually
        IProject webapps = WebappsUtil.findWebappsProject();
        String webappsName;
        if(webapps != null){
            webappsName = webapps.getName();
            if(path.contains(webappsName)){
                while(!projectPath.startsWith(webappsName)){
                    projectPath = projectPath.subpath(1, projectPath.getNameCount());
                }
                return new PathImpl(projectPath.toString());
            }
        }
        // return path relative to workspace
        return new PathImpl(workspacePath.relativize(projectPath));
    }

    @Override
    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'create(IProjectDescription description, IProgressMonitor monitor)'");
    }

    @Override
    public void open(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'open(IProgressMonitor monitor)'");
    }

    @Override
    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'open(int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void create(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'create(IProgressMonitor monitor)'");
    }
    
    @Override
    public IResource findMember(String path) {
        throw new UnsupportedOperationException("Unimplemented method 'findMember(String path)'");
    }
    
    @Override
    public IResource findMember(String path, boolean includePhantoms) {
        throw new UnsupportedOperationException("Unimplemented method 'findMember(String path, boolean includePhantoms)'");
    }
    
    @Override
    public IResource findMember(IPath path) {
        throw new UnsupportedOperationException("Unimplemented method 'findMember(IPath path)'");
    }
    
    @Override
    public IResource findMember(IPath path, boolean includePhantoms) {
        throw new UnsupportedOperationException("Unimplemented method 'findMember(IPath path, boolean includePhantoms)'");
    }
    
    @Override
    public String getDefaultCharset() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultCharset()'");
    }
    
    @Override
    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultCharset(boolean checkImplicit)'");
    }
    
    @Override
    public IResource[] members() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'members()'");
    }
    
    @Override
    public IResource[] members(boolean includePhantoms) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'members(boolean includePhantoms)'");
    }
    
    @Override
    public IResource[] members(int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'members(int memberFlags)'");
    }
    
    @Override
    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findDeletedMembersWithHistory(int depth, IProgressMonitor monitor)'");
    }
    
    @Override
    public void setDefaultCharset(String charset) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultCharset(String charset)'");
    }
    
    @Override
    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultCharset(String charset, IProgressMonitor monitor)'");
    }
    
    @Override
    public IResourceFilterDescription createFilter(int type, FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createFilter(int type, FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public IResourceFilterDescription[] getFilters() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getFilters()'");
    }
    
    @Override
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept(IResourceProxyVisitor visitor, int memberFlags)'");
    }
    
    @Override
    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept(IResourceProxyVisitor visitor, int depth, int memberFlags)'");
    }
    
    @Override
    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept(IResourceVisitor visitor)'");
    }
    
    @Override
    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept(IResourceVisitor visitor, int depth, boolean includePhantoms)'");
    }
    
    @Override
    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept(IResourceVisitor visitor, int depth, int memberFlags)'");
    }
    
    @Override
    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'clearHistory(IProgressMonitor monitor)'");
    }
    
    @Override
    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IPath destination, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IPath destination, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IProjectDescription description, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public IMarker createMarker(String type) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createMarker(String type)'");
    }
    
    @Override
    public IResourceProxy createProxy() {
        throw new UnsupportedOperationException("Unimplemented method 'createProxy()'");
    }
    
    @Override
    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteMarkers(String type, boolean includeSubtypes, int depth)'");
    }
    
    @Override
    public IMarker findMarker(long id) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMarker(long id)'");
    }
    
    @Override
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMarkers(String type, boolean includeSubtypes, int depth)'");
    }
    
    @Override
    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)'");
    }
    
    @Override
    public String getFileExtension() {
        throw new UnsupportedOperationException("Unimplemented method 'getFileExtension()'");
    }
    
    @Override
    public long getLocalTimeStamp() {
        throw new UnsupportedOperationException("Unimplemented method 'getLocalTimeStamp()'");
    }
    
    @Override
    public URI getLocationURI() {
        throw new UnsupportedOperationException("Unimplemented method 'getLocationURI()'");
    }
    
    @Override
    public IMarker getMarker(long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getMarker(long id)'");
    }
    
    @Override
    public long getModificationStamp() {
        throw new UnsupportedOperationException("Unimplemented method 'getModificationStamp()'");
    }
    
    @Override
    public IPathVariableManager getPathVariableManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getPathVariableManager()'");
    }
    
    @Override
    public IContainer getParent() {
        throw new UnsupportedOperationException("Unimplemented method 'getParent()'");
    }
    
    @Override
    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPersistentProperties()'");
    }
    
    @Override
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPersistentProperty(QualifiedName key)'");
    }
    
    @Override
    public IProject getProject() {
        throw new UnsupportedOperationException("Unimplemented method 'getProject()'");
    }
    
    @Override
    public IPath getProjectRelativePath() {
        throw new UnsupportedOperationException("Unimplemented method 'getProjectRelativePath()'");
    }
    
    @Override
    public IPath getRawLocation() {
        throw new UnsupportedOperationException("Unimplemented method 'getRawLocation()'");
    }
    
    @Override
    public URI getRawLocationURI() {
        throw new UnsupportedOperationException("Unimplemented method 'getRawLocationURI()'");
    }
    
    @Override
    public ResourceAttributes getResourceAttributes() {
        throw new UnsupportedOperationException("Unimplemented method 'getResourceAttributes()'");
    }
    
    @Override
    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getSessionProperties()'");
    }
    
    @Override
    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getSessionProperty(QualifiedName key)'");
    }
    
    @Override
    public boolean isAccessible() {
        throw new UnsupportedOperationException("Unimplemented method 'isAccessible()'");
    }
    
    @Override
    public boolean isDerived() {
        throw new UnsupportedOperationException("Unimplemented method 'isDerived()'");
    }
    
    @Override
    public boolean isDerived(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isDerived(int options)'");
    }
    
    @Override
    public boolean isHidden() {
        throw new UnsupportedOperationException("Unimplemented method 'isHidden()'");
    }
    
    @Override
    public boolean isHidden(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isHidden(int options)'");
    }
    
    @Override
    public boolean isLinked() {
        throw new UnsupportedOperationException("Unimplemented method 'isLinked()'");
    }
    
    @Override
    public boolean isVirtual() {
        throw new UnsupportedOperationException("Unimplemented method 'isVirtual()'");
    }
    
    @Override
    public boolean isLinked(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isLinked(int options)'");
    }
    
    @Override
    public boolean isLocal(int depth) {
        throw new UnsupportedOperationException("Unimplemented method 'isLocal(int depth)'");
    }
    
    @Override
    public boolean isPhantom() {
        throw new UnsupportedOperationException("Unimplemented method 'isPhantom()'");
    }

    @Override
    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Unimplemented method 'isReadOnly()'");
    }
    
    @Override
    public boolean isSynchronized(int depth) {
        throw new UnsupportedOperationException("Unimplemented method 'isSynchronized(int depth)'");
    }
    
    @Override
    public boolean isTeamPrivateMember() {
        throw new UnsupportedOperationException("Unimplemented method 'isTeamPrivateMember()'");
    }
    
    @Override
    public boolean isTeamPrivateMember(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isTeamPrivateMember(int options)'");
    }
    
    @Override
    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IPath destination, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IPath destination, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)'");
    }
    
    @Override
    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'refreshLocal(int depth, IProgressMonitor monitor)'");
    }
    
    @Override
    public void revertModificationStamp(long value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'revertModificationStamp(long value)'");
    }
    
    @Override
    public void setDerived(boolean isDerived) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDerived(boolean isDerived)'");
    }
    
    @Override
    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDerived(boolean isDerived, IProgressMonitor monitor)'");
    }
    
    @Override
    public void setHidden(boolean isHidden) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setHidden(boolean isHidden)'");
    }
    
    @Override
    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setLocal(boolean flag, int depth, IProgressMonitor monitor)'");
    }
    
    @Override
    public long setLocalTimeStamp(long value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setLocalTimeStamp(long value)'");
    }
    
    @Override
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setPersistentProperty(QualifiedName key, String value)'");
    }    

    @Override
    public void setReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException("Unimplemented method 'setReadOnly(boolean readOnly)'");
    }
    
    @Override
    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setResourceAttributes(ResourceAttributes attributes)'");
    }
    
    @Override
    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setSessionProperty(QualifiedName key, Object value)'");
    }
    
    @Override
    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setTeamPrivateMember(boolean isTeamPrivate)'");
    }
    
    @Override
    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'touch(IProgressMonitor monitor)'");
    }
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new UnsupportedOperationException("Unimplemented method 'getAdapter(Class<T> adapter)'");
    }
    
    @Override
    public boolean contains(ISchedulingRule rule) {
        throw new UnsupportedOperationException("Unimplemented method 'contains(ISchedulingRule rule)'");
    }
    
    @Override
    public boolean isConflicting(ISchedulingRule rule) {
        throw new UnsupportedOperationException("Unimplemented method 'isConflicting(ISchedulingRule rule)'");
    }
    
    @Override
    public void build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor)'");
    }
    
    @Override
    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'build(int kind, IProgressMonitor monitor)'");
    }
    
    @Override
    public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'build(IBuildConfiguration config, int kind, IProgressMonitor monitor)'");
    }
    
    @Override
    public void close(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'close(IProgressMonitor monitor)'");
    }
    
    @Override
    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(boolean deleteContent, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getActiveBuildConfig()'");
    }
    
    @Override
    public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getBuildConfig(String configName)'");
    }
    
    @Override
    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getBuildConfigs()'");
    }
    
    @Override
    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getContentTypeMatcher()'");
    }
    
    @Override
    public IProjectDescription getDescription() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getDescription()'");
    }
    
    @Override
    public IProjectNature getNature(String natureId) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getNature(String natureId)'");
    }
    
    @Override
    public IPath getWorkingLocation(String id) {
        throw new UnsupportedOperationException("Unimplemented method 'getWorkingLocation(String id)'");
    }
    
    @Override
    public IProject[] getReferencedProjects() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getReferencedProjects()'");
    }
    
    @Override
    public void clearCachedDynamicReferences() {
        throw new UnsupportedOperationException("Unimplemented method 'clearCachedDynamicReferences()'");
    }
    
    @Override
    public IProject[] getReferencingProjects() {
        throw new UnsupportedOperationException("Unimplemented method 'getReferencingProjects()'");
    }
    
    @Override
    public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getReferencedBuildConfigs(String configName, boolean includeMissing)'");
    }
    
    @Override
    public boolean hasBuildConfig(String configName) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'hasBuildConfig(String configName)'");
    }
    
    @Override
    public boolean hasNature(String natureId) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'hasNature(String natureId)'");
    }
    
    @Override
    public boolean isNatureEnabled(String natureId) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'isNatureEnabled(String natureId)'");
    }
    
    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException("Unimplemented method 'isOpen()'");
    }
    
    @Override
    public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)'");
    }
    
    @Override
    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IProjectDescription description, boolean force, IProgressMonitor monitor)'");
    }
    
    @Override
    public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)'");
    }
    
    @Override
    public void setDescription(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDescription(IProjectDescription description, IProgressMonitor monitor)'");
    }
    
    @Override
    public void setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public String getDefaultLineSeparator() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultLineSeparator()'");
    }
}