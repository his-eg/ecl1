package net.sf.ecl1.utilities.standalone.workspace;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;


public class FolderImpl implements IFolder {

    private final Path path;

    public FolderImpl(String path) {
        this.path = Paths.get(path);
    }

    public FolderImpl(Path path) {
        this.path = path;
    }

    @Override
    public boolean exists() {
        File folder = path.toFile();
        return (folder.exists() && folder.isDirectory());
    }

    @Override
    public boolean exists(IPath path) {
        File folder = path.toFile();
        return (folder.exists() && folder.isDirectory());
    }

    @Override
    public IPath getLocation() {
        return new PathImpl(path);
    }

    @Override
    public IPath getRawLocation() {
       return getLocation();
    }

    @Override
    public URI getLocationURI() {
        return path.toUri();
    }

    @Override
    public IContainer getParent() {
        String workspace = new WorkspaceRootImpl().getLocation().toString();
        String parent = path.getParent().toString();
        if(!parent.equals(workspace)){
            return new FolderImpl(parent);
        }
        // parent is workspace root
        return null;
    }

    @Override
    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, FolderImpl.class, "Error creating directories at: " + path.toString()));
        } 
    }

    @Override
    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
       create(false, local, monitor);
    }

    @Override
    public IPath getFullPath() {
        return ProjectImpl.getFullPath(path.toString());
    }

    @Override
    public IResource[] members() {
        File folder = path.toFile();
        List<IResource> children = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            // Get all files and subdirectories
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        children.add(new FolderImpl(file.getAbsolutePath()));
                    } else {
                        children.add(new FileImpl(file.getAbsolutePath()));
                    }
                }
            }
        }
        return children.toArray(new IResource[0]);
    }

    @Override
    public int getType() {
        // cant get type for project
        if(path.toString().equals(new WorkspaceRootImpl().getLocation().toString())){
            return WorkspaceImpl.TYPE_ROOT;
        }
        File folder = path.toFile();
        if(folder.exists() && folder.isDirectory()){
            return WorkspaceImpl.TYPE_FOLDER;
        }else if(folder.exists() && folder.isFile()){
            return WorkspaceImpl.TYPE_FILE;
        }
        // no type
        return 0;
    }
    
    @Override
    public IFile getFile(String name) {
        return new FileImpl(path.resolve(name));
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
    public IFile getFile(IPath path) {
        throw new UnsupportedOperationException("Unimplemented method 'getFile(IPath path)'");
    }

    @Override
    public IFolder getFolder(IPath path) {
        throw new UnsupportedOperationException("Unimplemented method 'getFolder(IPath path)'");
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
    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'clearHistory(IProgressMonitor monitor)'");
    }

    @Override
    @Deprecated
    public void setDefaultCharset(String charset) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultCharset(String charset)'");
    }

    @Override
    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDefaultCharset(String charset, IProgressMonitor monitor)'");
    }

    @Override
    public IResourceFilterDescription createFilter(int type, FileInfoMatcherDescription matcherDescription,
            int updateFlags, IProgressMonitor monitor) throws CoreException {
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
    public IMarker getMarker(long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getMarker(long id)'");
    }
    
    @Override
    public long getModificationStamp() {
        throw new UnsupportedOperationException("Unimplemented method 'getModificationStamp()'");
    }
    
    @Override
    public String getName() {
        throw new UnsupportedOperationException("Unimplemented method 'getName()'");
    }
    
    @Override
    public IPathVariableManager getPathVariableManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getPathVariableManager()'");
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
    public IWorkspace getWorkspace() {
        throw new UnsupportedOperationException("Unimplemented method 'getWorkspace()'");
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
    @Deprecated
    public boolean isLocal(int depth) {
        throw new UnsupportedOperationException("Unimplemented method 'isLocal(int depth)'");
    }
    
    @Override
    public boolean isPhantom() {
        throw new UnsupportedOperationException("Unimplemented method 'isPhantom()'");
    }
    
    @Override
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createLink(URI location, int updateFlags, IProgressMonitor monitor)'");
    }
    
    @Override
    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete(boolean force, boolean keepHistory, IProgressMonitor monitor)'");
    }
    
    @Override
    public IFolder getFolder(String name) {
        throw new UnsupportedOperationException("Unimplemented method 'getFolder(String name)'");
    }
    
    @Override
    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)'");
    }
}
