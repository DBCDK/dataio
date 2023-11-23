package dk.dbc.dataio.bfs.api;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import static junitx.framework.FileAssert.assertBinaryEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class BinaryFileFsImplTest {
    private static final byte[] DATA = "8 bytes!".getBytes();

    @Rule
    public TemporaryFolder mountPoint = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_pathArgIsNull_throws() {
        new BinaryFileFsImpl(null);
    }

    @Test
    public void constructor_pathArgIsValid_returnsNewInstance() throws IOException {
        final Path filePath = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        assertThat(binaryFileFs, is(notNullValue()));
        assertThat(binaryFileFs.getPath(), is(filePath));
    }

    @Test
    public void write_isArgIsNull_throws() throws IOException {
        final Path filePath = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        try {
            binaryFileFs.write(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void write_pathAlreadyExists_throws() throws IOException {
        final Path existingFile = mountPoint.newFile().toPath();
        writeFile(existingFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(existingFile);
        try {
            binaryFileFs.write(getInputStreamForFile(existingFile));
            fail("No Exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void write_writesData() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        writeFile(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        Files.delete(destinationFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        binaryFileFs.write(getInputStreamForFile(sourceFile));
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    @Test
    public void append_pathDoesNotExist_throws() {
        final Path file = Paths.get("file");
        final BinaryFileFsImpl binaryFile = new BinaryFileFsImpl(file);
        try {
            binaryFile.append(new byte[]{});
            fail("No IllegalStateException thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void append() throws IOException {
        final Path destinationFile = mountPoint.newFile().toPath();
        Files.delete(destinationFile);
        final BinaryFileFsImpl binaryFile = new BinaryFileFsImpl(destinationFile);
        binaryFile.write(new ByteArrayInputStream("foo".getBytes()));
        binaryFile.append("bar".getBytes());
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        binaryFile.read(sink);
        assertThat(new String(sink.toByteArray()), is("foobar"));
    }

    @Test
    public void openOutputStream_pathAlreadyExists_throws() throws IOException {
        final Path existingFile = mountPoint.newFile().toPath();
        writeFile(existingFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(existingFile);
        try {
            binaryFileFs.openOutputStream();
            fail("No Exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void openOutputStream_returnsStreamForWriting() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        writeFile(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        Files.delete(destinationFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        try (OutputStream os = binaryFileFs.openOutputStream()) {
            FileUtils.copyFile(sourceFile.toFile(), os);
        }
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    @Test
    public void openOutputStream_returnsStreamForWriting_withAppend() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        writeFile(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        writeFile(destinationFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        try (OutputStream os = binaryFileFs.openOutputStream(true)) {
            FileUtils.copyFile(sourceFile.toFile(), os);
        }
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    @Test
    public void delete_pathDoesNotExist_returns() {
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(Paths.get("/no/such/file"));
        binaryFileFs.delete();
    }

    @Test
    public void delete_pathDoesExist_deletesFile() throws IOException {
        final Path filePath = mountPoint.newFile().toPath();
        assertThat(Files.exists(filePath), is(true));
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        binaryFileFs.delete();
        assertThat(Files.exists(filePath), is(false));
    }

    @Test
    public void exists_pathExists_returnsTrue() throws IOException {
        final Path filePath = mountPoint.newFile().toPath();
        assertThat(Files.exists(filePath), is(true));
    }

    @Test
    public void exists_pathDoesNotExist_returnsFalse() throws IOException {
        final Path filePath = mountPoint.newFile().toPath();
        Files.delete(filePath);
        assertThat(Files.exists(filePath), is(false));
    }

    @Test
    public void read_osArgIsNull_throws() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try {
            binaryFileFs.read(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void read_pathDoesNotExist_throws() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        Files.delete(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try {
            binaryFileFs.read(getOutputStreamForFile(destinationFile));
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void read_readsData() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        writeFile(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        binaryFileFs.read(getOutputStreamForFile(destinationFile));
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    @Test
    public void read_gz() throws IOException {
        final ByteArrayOutputStream gzData = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzData);
        gzipOutputStream.write(DATA, 0, DATA.length);
        gzipOutputStream.close();
        assertThat("verify gz", gzData.toByteArray(), is(not(DATA)));

        final Path gzFile = mountPoint.newFile().toPath();
        writeFile(gzFile, gzData.toByteArray());

        final BinaryFileFsImpl gzBinaryFile = new BinaryFileFsImpl(gzFile);

        final ByteArrayOutputStream compressedBytesRead = new ByteArrayOutputStream();
        gzBinaryFile.read(compressedBytesRead);
        assertThat("read compressed", compressedBytesRead.toByteArray(),
                is(gzData.toByteArray()));

        final ByteArrayOutputStream decompressedBytesRead = new ByteArrayOutputStream();
        gzBinaryFile.read(decompressedBytesRead, true);
        assertThat("read decompressed", decompressedBytesRead.toByteArray(),
                is(DATA));
    }

    @Test
    public void size_gz() throws IOException {
        final ByteArrayOutputStream gzData = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzData);
        gzipOutputStream.write(DATA, 0, DATA.length);
        gzipOutputStream.close();
        assertThat("verify gz", gzData.toByteArray(), is(not(DATA)));

        final Path gzFile = mountPoint.newFile().toPath();
        writeFile(gzFile, gzData.toByteArray());

        final BinaryFileFsImpl gzBinaryFile = new BinaryFileFsImpl(gzFile);

        assertThat("size compressed", gzBinaryFile.size(false),
                is((long) gzData.toByteArray().length));

        assertThat("size decompressed", gzBinaryFile.size(true),
                is((long) DATA.length));
    }

    @Test
    public void read_bz2() throws IOException {
        final ByteArrayOutputStream bz2Data = new ByteArrayOutputStream();
        final BZip2CompressorOutputStream bz2Out = new BZip2CompressorOutputStream(bz2Data);
        bz2Out.write(DATA, 0, DATA.length);
        bz2Out.close();
        assertThat("verify bz2", bz2Data.toByteArray(), is(not(DATA)));

        final Path bz2File = mountPoint.newFile().toPath();
        writeFile(bz2File, bz2Data.toByteArray());

        final BinaryFileFsImpl bz2BinaryFile = new BinaryFileFsImpl(bz2File);

        final ByteArrayOutputStream compressedBytesRead = new ByteArrayOutputStream();
        bz2BinaryFile.read(compressedBytesRead);
        assertThat("read compressed", compressedBytesRead.toByteArray(),
                is(bz2Data.toByteArray()));

        final ByteArrayOutputStream decompressedBytesRead = new ByteArrayOutputStream();
        bz2BinaryFile.read(decompressedBytesRead, true);
        assertThat("read decompressed", decompressedBytesRead.toByteArray(),
                is(DATA));
    }

    @Test
    public void size_bz2() throws IOException {
        final ByteArrayOutputStream bz2Data = new ByteArrayOutputStream();
        final BZip2CompressorOutputStream bz2Out = new BZip2CompressorOutputStream(bz2Data);
        bz2Out.write(DATA, 0, DATA.length);
        bz2Out.close();
        assertThat("verify bz2", bz2Data.toByteArray(), is(not(DATA)));

        final Path bz2File = mountPoint.newFile().toPath();
        writeFile(bz2File, bz2Data.toByteArray());

        final BinaryFileFsImpl bz2BinaryFile = new BinaryFileFsImpl(bz2File);

        assertThat("size compressed", bz2BinaryFile.size(false),
                is((long) bz2Data.toByteArray().length));

        assertThat("size decompressed", bz2BinaryFile.size(true),
                is(-4_348_520L));
    }

    @Test
    public void openInputStream_pathDoesNotExist_throws() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        Files.delete(sourceFile);
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try {
            binaryFileFs.openInputStream();
            fail("No Exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void openInputStream_opensStreamForReading() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        writeFile(sourceFile);
        final Path destinationFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try (InputStream is = binaryFileFs.openInputStream()) {
            IOUtils.copy(is, getOutputStreamForFile(destinationFile));
        }
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    private InputStream getInputStreamForFile(Path file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file.toFile()));
    }

    private OutputStream getOutputStreamForFile(Path file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file.toFile()));
    }

    private void writeFile(Path path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            int iterations = (2 * BinaryFileFsImpl.BUFFER_SIZE) / DATA.length;
            while (iterations > 0) {
                fos.write(DATA, 0, DATA.length);
                iterations--;
            }
            fos.flush();
        }
    }

    private void writeFile(Path path, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(bytes, 0, bytes.length);
            fos.flush();
        }
    }
}
