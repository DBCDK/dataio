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
     *
     * @param is input stream of bytes to be written
     */
    void write(final InputStream is);

    /**
     * Appends content to this binary file representation
     *
     * @param bytes bytes to be appended
     */
    void append(final byte[] bytes);

    /**
     * @return an OutputStream for writing to this file
     */
    OutputStream openOutputStream();

    /**
     * @param append boolean which if true allows for appending on an existing file
     * @return an OutputStream for writing to this file
     */
    OutputStream openOutputStream(boolean append);

    /**
     * Deletes this binary file representation
     */
    void delete();

    /**
     * Reads content of this binary file representation into given output stream
     *
     * @param os output stream to which bytes are written
     */
    void read(final OutputStream os);

    /**
     * Reads content of this binary file representation into given output stream,
     * decompressing it if decompress flag is set to true. Currently only gzip
     * compression is supported.
     *
     * @param os         output stream to which bytes are written
     * @param decompress on-the-fly decompression flag
     */
    void read(final OutputStream os, final boolean decompress);

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
     *
     * @return true if the file exists, false if the file does not exist or
     * its existence cannot be determined
     */
    boolean exists();

    /**
     * @param decompressed report uncompressed size for compressed
     *                     file if true
     * @return size of this file in bytes
     */
    long size(final boolean decompressed);
}
