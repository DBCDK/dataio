/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * Appends content to this binary file representation
     * @param bytes bytes to be appended
     */
    void append(final byte[] bytes);

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
     * Reads content of this binary file representation into given output stream,
     * decompressing it if decompress flag is set to true. Currently only gzip
     * compression is supported.
     * @param os output stream to which bytes are written
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
     * @return true if the file exists, false if the file does not exist or
     * its existence cannot be determined
     */
    boolean exists();
}
