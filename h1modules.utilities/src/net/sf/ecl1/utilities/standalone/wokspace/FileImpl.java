package net.sf.ecl1.utilities.standalone.wokspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
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
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class FileImpl implements IFile{
    
    private final String path;
    private final File file;

    public FileImpl(String path) {
        this.path = path;
        this.file = new File(path);
    }

    @Override
    public IPath getFullPath() {
        return ProjectImpl.getFullPath(path);
    }

    @Override
    public int getType() {
        return WorkspaceImpl.TYPE_FILE;
    }

    @Override
    public InputStream getContents() throws CoreException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, FileImpl.class, "File not found at: " + file.getAbsolutePath()));
        }
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        try {
            Files.copy(source, file.toPath());
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, FileImpl.class,
             "File cound not be created at: " + file.getAbsolutePath() + "\n" + e.getMessage()));
        }
    }
    
    @Override
    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        file.delete();
    }

    @Override
    public IContainer getParent() {
        File parent = Paths.get(path).getParent().toFile();
        if(file.isFile()){
            return (IContainer) new FileImpl(parent.getAbsolutePath());
        }else{
            return (IContainer) new FolderImpl(parent.getAbsolutePath());
        }
    }

    @Override
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
    }

    @Override
    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'clearHistory'");
    }

    @Override
    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public IMarker createMarker(String type) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createMarker'");
    }

    @Override
    public IResourceProxy createProxy() {
        throw new UnsupportedOperationException("Unimplemented method 'createProxy'");
    }

    @Override
    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteMarkers'");
    }

    @Override
    public IMarker findMarker(long id) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMarker'");
    }

    @Override
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMarkers'");
    }

    @Override
    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'findMaxProblemSeverity'");
    }

    @Override
    public String getFileExtension() {
        throw new UnsupportedOperationException("Unimplemented method 'getFileExtension'");
    }

    @Override
    public long getLocalTimeStamp() {
        throw new UnsupportedOperationException("Unimplemented method 'getLocalTimeStamp'");
    }

    @Override
    public IPath getLocation() {
        throw new UnsupportedOperationException("Unimplemented method 'getLocation'");
    }

    @Override
    public URI getLocationURI() {
        throw new UnsupportedOperationException("Unimplemented method 'getLocationURI'");
    }

    @Override
    public IMarker getMarker(long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getMarker'");
    }

    @Override
    public long getModificationStamp() {
        throw new UnsupportedOperationException("Unimplemented method 'getModificationStamp'");
    }

    @Override
    public IPathVariableManager getPathVariableManager() {
        throw new UnsupportedOperationException("Unimplemented method 'getPathVariableManager'");
    }

    @Override
    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPersistentProperties'");
    }

    @Override
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPersistentProperty'");
    }

    @Override
    public IProject getProject() {
        throw new UnsupportedOperationException("Unimplemented method 'getProject'");
    }

    @Override
    public IPath getProjectRelativePath() {
        throw new UnsupportedOperationException("Unimplemented method 'getProjectRelativePath'");
    }

    @Override
    public IPath getRawLocation() {
        throw new UnsupportedOperationException("Unimplemented method 'getRawLocation'");
    }

    @Override
    public URI getRawLocationURI() {
        throw new UnsupportedOperationException("Unimplemented method 'getRawLocationURI'");
    }

    @Override
    public ResourceAttributes getResourceAttributes() {
        throw new UnsupportedOperationException("Unimplemented method 'getResourceAttributes'");
    }

    @Override
    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getSessionProperties'");
    }

    @Override
    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getSessionProperty'");
    }

    @Override
    public IWorkspace getWorkspace() {
        throw new UnsupportedOperationException("Unimplemented method 'getWorkspace'");
    }

    @Override
    public boolean isAccessible() {
        throw new UnsupportedOperationException("Unimplemented method 'isAccessible'");
    }

    @Override
    public boolean isDerived() {
        throw new UnsupportedOperationException("Unimplemented method 'isDerived'");
    }

    @Override
    public boolean isDerived(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isDerived'");
    }

    @Override
    public boolean isHidden() {
        throw new UnsupportedOperationException("Unimplemented method 'isHidden'");
    }

    @Override
    public boolean isHidden(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isHidden'");
    }

    @Override
    public boolean isLinked() {
        throw new UnsupportedOperationException("Unimplemented method 'isLinked'");
    }

    @Override
    public boolean isVirtual() {
        throw new UnsupportedOperationException("Unimplemented method 'isVirtual'");
    }

    @Override
    public boolean isLinked(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isLinked'");
    }

    @Override
    public boolean isLocal(int depth) {
        throw new UnsupportedOperationException("Unimplemented method 'isLocal'");
    }

    @Override
    public boolean isPhantom() {
        throw new UnsupportedOperationException("Unimplemented method 'isPhantom'");
    }

    @Override
    public boolean isSynchronized(int depth) {
        throw new UnsupportedOperationException("Unimplemented method 'isSynchronized'");
    }

    @Override
    public boolean isTeamPrivateMember() {
        throw new UnsupportedOperationException("Unimplemented method 'isTeamPrivateMember'");
    }

    @Override
    public boolean isTeamPrivateMember(int options) {
        throw new UnsupportedOperationException("Unimplemented method 'isTeamPrivateMember'");
    }

    @Override
    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'refreshLocal'");
    }

    @Override
    public void revertModificationStamp(long value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'revertModificationStamp'");
    }

    @Override
    public void setDerived(boolean isDerived) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDerived'");
    }

    @Override
    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setDerived'");
    }

    @Override
    public void setHidden(boolean isHidden) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setHidden'");
    }

    @Override
    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setLocal'");
    }

    @Override
    public long setLocalTimeStamp(long value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setLocalTimeStamp'");
    }

    @Override
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setPersistentProperty'");
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException("Unimplemented method 'setReadOnly'");
    }

    @Override
    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setResourceAttributes'");
    }

    @Override
    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setSessionProperty'");
    }

    @Override
    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setTeamPrivateMember'");
    }

    @Override
    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'touch'");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new UnsupportedOperationException("Unimplemented method 'getAdapter'");
    }

    @Override
    public boolean contains(ISchedulingRule rule) {
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    @Override
    public boolean isConflicting(ISchedulingRule rule) {
        throw new UnsupportedOperationException("Unimplemented method 'isConflicting'");
    }

    @Override
    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'appendContents'");
    }

    @Override
    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'appendContents'");
    }

    @Override
    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createLink'");
    }

    @Override
    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'createLink'");
    }

    @Override
    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public String getCharset() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getCharset'");
    }

    @Override
    public String getCharset(boolean checkImplicit) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getCharset'");
    }

    @Override
    public String getCharsetFor(Reader reader) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getCharsetFor'");
    }

    @Override
    public IContentDescription getContentDescription() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getContentDescription'");
    }

    @Override
    public InputStream getContents(boolean force) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getContents'");
    }

    @Override
    public int getEncoding() throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getEncoding'");
    }

    @Override
    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getHistory'");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public boolean isReadOnly() {
        throw new UnsupportedOperationException("Unimplemented method 'isReadOnly'");
    }

    @Override
    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public void setCharset(String newCharset) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setCharset'");
    }

    @Override
    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setCharset'");
    }

    @Override
    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setContents'");
    }

    @Override
    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setContents'");
    }

    @Override
    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setContents'");
    }

    @Override
    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setContents'");
    }

}
