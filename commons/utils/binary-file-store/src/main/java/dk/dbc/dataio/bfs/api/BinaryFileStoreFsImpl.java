package dk.dbc.dataio.bfs.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Access to binary files stored in file system
 */
public class BinaryFileStoreFsImpl implements BinaryFileStore {
    private final Path base;

    /**
     * Class constructor
     * @param base base path of file system store
     * @throws IllegalArgumentException if given null-valued base argument, or if
     * given base path is non-absolute
     */
    public BinaryFileStoreFsImpl(Path base) throws IllegalArgumentException {
        if (base == null) {
            throw new IllegalArgumentException("Value of base parameter can not be null");
        }
        if (!base.isAbsolute()) {
            throw new IllegalArgumentException(String.format(
                    "Unable to initialize binary file store - base path is not absolute: %s", base));
        }
        this.base = Paths.get(base.toString());
    }

    /**
     * Returns file system binary file representation associated with given path
     * @param path binary file path relative to base path specified in constructor
     * @return binary file representation
     * @throws IllegalArgumentException if given null-valued path argument, or if
     * given absolute path
     */
    @Override
    public BinaryFile getBinaryFile(Path path) throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("Value of path parameter can not be null");
        }
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Value of path parameter can not be absolute path " + path);
        }
        return new BinaryFileFsImpl(base.resolve(path));
    }
}
