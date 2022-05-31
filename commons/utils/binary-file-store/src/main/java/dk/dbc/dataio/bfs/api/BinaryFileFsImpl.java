package dk.dbc.dataio.bfs.api;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import dk.dbc.invariant.InvariantUtil;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

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
 * </p>
 */
public class BinaryFileFsImpl implements BinaryFile {
    public static final int BUFFER_SIZE = 8192;

    private enum Compression {BZIP2, GZIP, RAW}

    private final Path path;

    /**
     * Class constructor
     *
     * @param path path to binary file
     * @throws NullPointerException if given null-valued path
     */
    public BinaryFileFsImpl(Path path) throws NullPointerException {
        this.path = InvariantUtil.checkNotNullOrThrow(path, "path");
    }

    /**
     * Writes content of given input stream to this file creating parent directories as needed
     *
     * @param is input stream of bytes to be written
     * @throws NullPointerException  if given null valued is argument
     * @throws IllegalStateException if trying to write to a file that already exists, or
     *                               on general failure to write file
     */
    @Override
    public void write(final InputStream is) throws IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullOrThrow(is, "is");
        if (Files.exists(path)) {
            throw new IllegalStateException("File already exists " + path);
        }
        createPathIfNotExists(path.getParent());
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
            final byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) > 0) {
                bos.write(buf, 0, bytesRead);
            }
            bos.flush();
        } catch (IOException e) {
            String error = "Unable to write file " + path;
            try {
                Files.deleteIfExists(path);
            } catch (IOException ioException) {
                error += " - filesystem not cleansed: " + ioException.getMessage();
            }
            throw new IllegalStateException(error, e);
        }
    }

    /**
     * Appends to this file
     *
     * @param bytes bytes to be appended
     * @throws IllegalStateException if trying to append to a non-existing file,
     *                               or on general failure to append
     */
    @Override
    public void append(final byte[] bytes) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Attempt to append to non-existing file " + path);
        }
        if (bytes != null) {
            try (BufferedOutputStream bos = new BufferedOutputStream(
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
     *                               to create OutputStream
     */
    @Override
    public OutputStream openOutputStream() throws IllegalStateException {
        return openOutputStream(false);
    }

    /**
     * @param append boolean which if true allows for appending on an existing file
     * @return an OutputStream for writing to this file
     * @throws IllegalStateException if file already has content written and append is false or on general failure
     *                               to create OutputStream
     */
    @Override
    public OutputStream openOutputStream(boolean append) throws IllegalStateException {
        if (!append && Files.exists(path)) {
            throw new IllegalStateException("File already exists " + path);
        }
        createPathIfNotExists(path.getParent());
        try {
            return new FileOutputStream(path.toFile(), append);
        } catch (IOException e) {
            throw new IllegalStateException("Unable open OutputStream for file " + path, e);
        }
    }

    /**
     * Deletes this file (if it exists)
     *
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
     *
     * @param os output stream to which bytes are written
     * @throws NullPointerException  if given null-valued os argument
     * @throws IllegalStateException if trying to read a file which does not exists, or on
     *                               general failure to read file
     */
    @Override
    public void read(final OutputStream os) throws IllegalArgumentException, IllegalStateException {
        read(os, false);
    }

    /**
     * Reads content of this file into given output stream,
     * decompressing it if decompress flag is set to true.
     *
     * @param os         output stream to which bytes are written
     * @param decompress on-the-fly decompression flag
     * @throws NullPointerException  if given null-valued os argument
     * @throws IllegalStateException if trying to read a file which does not exists, or on
     *                               general failure to read file
     */
    @Override
    public void read(final OutputStream os, final boolean decompress)
            throws IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullOrThrow(os, "os");
        if (!Files.exists(path)) {
            throw new IllegalStateException("File does not exist " + path);
        }
        try (InputStream is = createInputStreamForReading(decompress)) {
            final byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file " + path, e);
        }
    }

    private InputStream createInputStreamForReading(final boolean decompress)
            throws IOException {
        final Compression compression = getCompression();
        if (!decompress || compression == Compression.RAW) {
            return new BufferedInputStream(new FileInputStream(path.toFile()));
        }
        switch (compression) {
            case BZIP2:
                return new BZip2CompressorInputStream(new FileInputStream(path.toFile()));
            case GZIP:
                return new GZIPInputStream(new BufferedInputStream(new FileInputStream(path.toFile())));
            default:
                return new BufferedInputStream(new FileInputStream(path.toFile()));
        }
    }

    private Compression getCompression() {
        try {
            final ContentInfoUtil infoFinder = new ContentInfoUtil();
            final ContentInfo info = infoFinder.findMatch(
                    path.toAbsolutePath().toString());
            if (info == null) {
                return Compression.RAW;
            }
            switch (info.getContentType()) {
                case BZIP2:
                    return Compression.BZIP2;
                case GZIP:
                    return Compression.GZIP;
                default:
                    return Compression.RAW;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return an InputStream for reading from this file
     * @throws IllegalStateException if file has no content, or on general failure to
     *                               create InputStream
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
        final Compression compression = getCompression();
        if (decompressed) {
            switch (compression) {
                case BZIP2:
                    return -4_348_520;  // Unfortunately the bz2 format does not contain
                // any information about the uncompressed size in
                // its metadata, so we simply return the negative
                // value of the bz2 magic number to indicate to the
                // caller that the returned size is not available.
                // bz2 magic number BZh == HEX 42 5A 68 == DEC 4.348.520
                case GZIP:
                    return getGzipDecompressedSize();
            }
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
    private long getGzipDecompressedSize() {
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
