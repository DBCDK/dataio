package dk.dbc.dataio.bfs.api;

import java.nio.file.Path;

/**
 * Access to binary files in store
 */
public interface BinaryFileStore {
    /**
     * Returns binary file representation associated with given path
     *
     * @param path binary file path
     * @return binary file representation
     */
    BinaryFile getBinaryFile(Path path);
}
