package net.sf.ecl1.utilities.standalone.wokspace;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IPath;

public class PathImpl implements IPath {
    private final String path;

    public PathImpl(String path) {
        this.path = path;
    }

    @Override
    public String toOSString() {
        //converts to os-string
        return Paths.get(path).toString();
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public File toFile() {
        return new File(path);
    }

    @Override
    public IPath append(String path) {
        return new PathImpl(Paths.get(this.path, path).toString());
    }

    @Override
    public IPath append(IPath path) {
        return new PathImpl(Paths.get(this.path, path.toString()).toString());
    }

    @Override
    public IPath clone() {
        return new PathImpl(path);
    }

    @Override
    public String lastSegment() {
        // getFileName() returns last segment can be a dir
        return Paths.get(path).getFileName().toString();
    }

    @Override
    public int segmentCount() {
        return Paths.get(path).getNameCount();
    }

    @Override
    public IPath removeFirstSegments(int count) {
        Path newPath = Paths.get(path);
        newPath = newPath.subpath(count, newPath.getNameCount());
        return new PathImpl(newPath.toString());
    }

    @Override
    public IPath removeFileExtension() {
        String fileName = lastSegment();
        int dotIndex = fileName.lastIndexOf('.');
        // make sure file doesnt start with .
        if(dotIndex > 0) {
            return new PathImpl(Paths.get(path).resolveSibling(fileName.substring(0, dotIndex)).toString());
        }
        return new PathImpl(path);
    }

    @Override
    public IPath addFileExtension(String extension) {
        throw new UnsupportedOperationException("Unimplemented method 'addFileExtension(String extension)'");
    }
    
    @Override
    public IPath addTrailingSeparator() {
        throw new UnsupportedOperationException("Unimplemented method 'addTrailingSeparator()'");
    }
    
    @Override
    public String getDevice() {
        throw new UnsupportedOperationException("Unimplemented method 'getDevice()'");
    }
    
    @Override
    public String getFileExtension() {
        throw new UnsupportedOperationException("Unimplemented method 'getFileExtension()'");
    }
    
    @Override
    public boolean hasTrailingSeparator() {
        throw new UnsupportedOperationException("Unimplemented method 'hasTrailingSeparator()'");
    }
    
    @Override
    public boolean isAbsolute() {
        throw new UnsupportedOperationException("Unimplemented method 'isAbsolute()'");
    }
    
    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Unimplemented method 'isEmpty()'");
    }
    
    @Override
    public boolean isPrefixOf(IPath anotherPath) {
        throw new UnsupportedOperationException("Unimplemented method 'isPrefixOf(IPath anotherPath)'");
    }
    
    @Override
    public boolean isRoot() {
        throw new UnsupportedOperationException("Unimplemented method 'isRoot()'");
    }
    
    @Override
    public boolean isUNC() {
        throw new UnsupportedOperationException("Unimplemented method 'isUNC()'");
    }
    
    @Override
    public boolean isValidPath(String path) {
        throw new UnsupportedOperationException("Unimplemented method 'isValidPath(String path)'");
    }
    
    @Override
    public boolean isValidSegment(String segment) {
        throw new UnsupportedOperationException("Unimplemented method 'isValidSegment(String segment)'");
    }
    
    @Override
    public IPath makeAbsolute() {
        throw new UnsupportedOperationException("Unimplemented method 'makeAbsolute()'");
    }
    
    @Override
    public IPath makeRelative() {
        throw new UnsupportedOperationException("Unimplemented method 'makeRelative()'");
    }
    
    @Override
    public IPath makeRelativeTo(IPath base) {
        throw new UnsupportedOperationException("Unimplemented method 'makeRelativeTo(IPath base)'");
    }
    
    @Override
    public IPath makeUNC(boolean toUNC) {
        throw new UnsupportedOperationException("Unimplemented method 'makeUNC(boolean toUNC)'");
    }
    
    @Override
    public int matchingFirstSegments(IPath anotherPath) {
        throw new UnsupportedOperationException("Unimplemented method 'matchingFirstSegments(IPath anotherPath)'");
    }
    
    @Override
    public IPath removeLastSegments(int count) {
        throw new UnsupportedOperationException("Unimplemented method 'removeLastSegments(int count)'");
    }
    
    @Override
    public IPath removeTrailingSeparator() {
        throw new UnsupportedOperationException("Unimplemented method 'removeTrailingSeparator()'");
    }
    
    @Override
    public String segment(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'segment(int index)'");
    }
    
    @Override
    public String[] segments() {
        throw new UnsupportedOperationException("Unimplemented method 'segments()'");
    }
    
    @Override
    public IPath setDevice(String device) {
        throw new UnsupportedOperationException("Unimplemented method 'setDevice(String device)'");
    }
    
    @Override
    public String toPortableString() {
        throw new UnsupportedOperationException("Unimplemented method 'toPortableString()'");
    }
    
    @Override
    public IPath uptoSegment(int count) {
        throw new UnsupportedOperationException("Unimplemented method 'uptoSegment(int count)'");
    }
}