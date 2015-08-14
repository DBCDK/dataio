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
     * @return an OutputStream for writing to this file
     */
    OutputStream openOutputStream();

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
     * @return an InputStream for reading from this file
     */
    InputStream openInputStream();

    /**
     * @return path of this binary file representation
     */
    Path getPath();

    /**
     * Tests whether a file exists
     * @return true if the file exists, false if the file does not exist or
     * its existence cannot be determined
     */
    boolean exists();
}
