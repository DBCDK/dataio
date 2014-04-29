package dk.dbc.dataio.bfs.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Represents binary file to be read, written or deleted
 */
public interface BinaryFile {
    /**
     * Writes content of given input stream to this binary file representation
     * @param is input stream of bytes to be written
     */
    void write(final InputStream is);

    /**
     * Deletes this binary file representation
     */
    void delete();

    /**
     * Reads content of this binary file representation into given output stream
     * @param os output stream to which bytes are written
     */
    void read(final OutputStream os);

    /**
     * Returns path of this binary file representation
     */
    Path getPath();
}
