package dk.dbc.dataio.bfs.api;

import dk.dbc.invariant.InvariantUtil;

import java.nio.file.Path;

/**
 * Access to binary files stored in file system
 */
public class BinaryFileStoreFsImpl implements BinaryFileStore {
    private final Path base;

    /**
     * Class constructor
     *
     * @param base base path of file system store
     * @throws NullPointerException     if given null-valued base argument
     * @throws IllegalArgumentException if given base path is non-absolute
     */
    public BinaryFileStoreFsImpl(Path base) throws NullPointerException, IllegalArgumentException {
        this.base = InvariantUtil.checkNotNullOrThrow(base, "base");
        if (!this.base.isAbsolute()) {
            throw new IllegalArgumentException(String.format(
                    "Unable to initialize binary file store - base path is not absolute: %s", base));
        }
    }

    /**
     * Returns file system binary file representation associated with given path
     *
     * @param path binary file path relative to base path specified in constructor
     * @return binary file representation
     * @throws NullPointerException     if given null-valued path argument
     * @throws IllegalArgumentException if given absolute path
     */
    @Override
    public BinaryFile getBinaryFile(Path path) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(path, "path");
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Value of path parameter can not be absolute path " + path);
        }
        return new BinaryFileFsImpl(base.resolve(path));
    }
}
