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

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;
import dk.dbc.invariant.InvariantUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * File system implementation of BinaryFile
 * <p>
 * Currently only gzip compression is supported for
 * operations with compression related functionality.
 * </p<
 */
public class BinaryFileFsImpl implements BinaryFile {
    public static final int BUFFER_SIZE = 8192;
    private final Path path;

    /**
     * Class constructor
     * @param path path to binary file
     * @throws NullPointerException if given null-valued path
     */
    public BinaryFileFsImpl(Path path) throws NullPointerException {
        this.path = InvariantUtil.checkNotNullOrThrow(path, "path");
    }

    /**
     * Writes content of given input stream to this file creating parent directories as needed
     * @param is input stream of bytes to be written
     * @throws NullPointerException if given null valued is argument
     * @throws IllegalStateException if trying to write to a file that already exists, or
     * on general failure to write file
     */
    @Override
    public void write(final InputStream is) throws IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullOrThrow(is, "is");
        if (Files.exists(path)) {
            throw new IllegalStateException("File already exists " + path);
        }
        createPathIfNotExists(path.getParent());
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
            final byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) > 0) {
                bos.write(buf, 0, bytesRead);
            }
            bos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + path, e);
        }
    }

    /**
     * Appends to this file
     * @param bytes bytes to be appended
     * @throws IllegalStateException if trying to append to a non-existing file,
     * or on general failure to append
     */
    @Override
    public void append(final byte[] bytes) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Attempt to append to non-existing file " + path);
        }
        if (bytes != null) {
            try (final BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(path.toFile(), true))) {
                bos.write(bytes, 0, bytes.length);
                bos.flush();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to append to file " + path, e);
            }
        }
    }

    /**
     * @return an OutputStream for writing to this file
     * @throws IllegalStateException if file already has content written or on general failure
     * to create OutputStream
     */
    @Override
    public OutputStream openOutputStream() throws IllegalStateException {
        if (Files.exists(path)) {
            throw new IllegalStateException("File already exists " + path);
        }
        createPathIfNotExists(path.getParent());
        try {
            return new FileOutputStream(path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Unable open OutputStream for file " + path, e);
        }
    }

    /**
     * Deletes this file (if it exists)
     * @throws IllegalStateException on general failure to delete existing file
     */
    @Override
    public void delete() throws IllegalStateException {
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to delete file " + path, e);
            }
        }
    }

    /**
     * Reads content of this file into given output stream
     * @param os output stream to which bytes are written
     * @throws NullPointerException if given null-valued os argument
     * @throws IllegalStateException if trying to read a file which does not exists, or on
     * general failure to read file
     */
    @Override
    public void read(final OutputStream os) throws IllegalArgumentException, IllegalStateException {
        read(os, false);
    }

    /**
     * Reads content of this file into given output stream,
     * decompressing it if decompress flag is set to true.
     * @param os output stream to which bytes are written
     * @param decompress on-the-fly decompression flag
     * @throws NullPointerException if given null-valued os argument
     * @throws IllegalStateException if trying to read a file which does not exists, or on
     * general failure to read file
     */
    @Override
    public void read(final OutputStream os, final boolean decompress)
            throws IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullOrThrow(os, "os");
        if (!Files.exists(path)) {
            throw new IllegalStateException("File does not exist " + path);
        }
        try (final BufferedInputStream bis = decompress && isCompressed() ?
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(path.toFile())))
                : new BufferedInputStream(new FileInputStream(path.toFile()))) {
            final byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + path, e);
        }
    }

    /* returns true if this file is gzip compressed */
    private boolean isCompressed() {
        try {
            final ContentInfoUtil infoFinder = new ContentInfoUtil();
            final ContentInfo info = infoFinder.findMatch(
                    path.toAbsolutePath().toString());
            return info != null && info.getContentType() == ContentType.GZIP;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return an InputStream for reading from this file
     * @throws IllegalStateException if file has no content, or on general failure to
     * create InputStream
     */
    @Override
    public InputStream openInputStream() throws IllegalStateException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("File does not exist " + path);
        }
        try {
            return new FileInputStream(path.toFile());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to open InputStream for file " + path, e);
        }
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public long size(boolean decompressed) {
        if (decompressed && isCompressed()) {
            return decompressedSize();
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /*
        Returns decompressed size of gzip file.

        The gzip format represents the uncompressed size modulo 2^32 in the
        last four bytes of the compressed file, meaning an incorrect
        decompressed size will be reported for files with an original size
        of 4 GiB or larger. The correct decompressed size will be the
        reported size plus a multiple of four GiB.
     */
    private long decompressedSize() {
        try {
            long size = -1;
            try (RandomAccessFile gz = new RandomAccessFile(
                    path.toFile(), "r")) {
                gz.seek(gz.length() - Integer.BYTES);
                final int n = gz.readInt();
                size = Integer.toUnsignedLong(Integer.reverseBytes(n));
            }
            return size;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    void createPathIfNotExists(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
