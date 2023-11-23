package dk.dbc.dataio.bfs.api;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BinaryFileFsImplTest {
    private static final String DATA = "8 bytes!";
    private static final byte[] BYTES = DATA.getBytes();


    @Test
    public void constructor_pathArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new BinaryFileFsImpl(null));
    }

    @Test
    public void constructor_pathArgIsValid_returnsNewInstance() throws IOException {
        Path filePath = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        assertThat(binaryFileFs, is(notNullValue()));
        assertThat(binaryFileFs.getPath(), is(filePath));
    }

    @Test
    public void write_isArgIsNull_throws() throws IOException {
        Path filePath = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        assertThrows(NullPointerException.class, () -> binaryFileFs.write(null));

    }

    @Test
    public void write_pathAlreadyExists_throws() throws IOException {
        Path existingFile = tempPath();
        writeFile(existingFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(existingFile);
        assertThrows(IllegalStateException.class, () -> binaryFileFs.write(getInputStreamForFile(existingFile)));
    }

    @Test
    public void write_writesData() throws IOException {
        Path sourceFile = tempPath();
        writeFile(sourceFile);
        Path destinationFile = tempPath();
        Files.delete(destinationFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        binaryFileFs.write(getInputStreamForFile(sourceFile));
        assertBinaryEquals(sourceFile, destinationFile);
    }

    private void assertBinaryEquals(Path p1, Path p2) throws IOException {
        byte[] b1 = Files.readAllBytes(p1);
        byte[] b2 = Files.readAllBytes(p2);
        Assertions.assertArrayEquals(b1, b2);
    }

    @Test
    public void append_pathDoesNotExist_throws() {
        Path file = Paths.get("file");
        BinaryFileFsImpl binaryFile = new BinaryFileFsImpl(file);
        assertThrows(IllegalStateException.class, () -> binaryFile.append(new byte[]{}));
    }

    @Test
    public void append() throws IOException {
        Path destinationFile = tempPath();
        Files.delete(destinationFile);
        BinaryFileFsImpl binaryFile = new BinaryFileFsImpl(destinationFile);
        binaryFile.write(new ByteArrayInputStream("foo".getBytes()));
        binaryFile.append("bar".getBytes());
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        binaryFile.read(sink);
        assertThat(sink.toString(), is("foobar"));
    }

    @Test
    public void openOutputStream_pathAlreadyExists_throws() throws IOException {
        Path existingFile = tempPath();
        writeFile(existingFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(existingFile);
        assertThrows(IllegalStateException.class, binaryFileFs::openOutputStream);
    }

    @Test
    public void openOutputStream_returnsStreamForWriting() throws IOException {
        Path sourceFile = tempPath();
        writeFile(sourceFile);
        Path destinationFile = tempPath();
        Files.delete(destinationFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        try (OutputStream os = binaryFileFs.openOutputStream()) {
            FileUtils.copyFile(sourceFile.toFile(), os);
        }
        assertBinaryEquals(sourceFile, destinationFile);
    }

    @Test
    public void openOutputStream_returnsStreamForWriting_withAppend() throws IOException {
        Path sourceFile = tempPath();
        writeFile(sourceFile);
        Path destinationFile = tempPath();
        writeFile(destinationFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(destinationFile);
        try (OutputStream os = binaryFileFs.openOutputStream(true)) {
            FileUtils.copyFile(sourceFile.toFile(), os);
        }
        Files.writeString(sourceFile, DATA + DATA);
        assertBinaryEquals(sourceFile, destinationFile);
    }

    @Test
    public void delete_pathDoesNotExist_returns() {
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(Paths.get("/no/such/file"));
        binaryFileFs.delete();
    }

    @Test
    public void delete_pathDoesExist_deletesFile() throws IOException {
        Path filePath = tempPath();
        assertThat(Files.exists(filePath), is(true));
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(filePath);
        binaryFileFs.delete();
        assertThat(Files.exists(filePath), is(false));
    }

    @Test
    public void read_osArgIsNull_throws() throws IOException {
        Path sourceFile = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        assertThrows(NullPointerException.class, () -> binaryFileFs.read(null));
    }

    @Test
    public void read_pathDoesNotExist_throws() throws IOException {
        Path sourceFile = tempPath();
        Files.delete(sourceFile);
        Path destinationFile = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        assertThrows(IllegalStateException.class, () -> binaryFileFs.read(getOutputStreamForFile(destinationFile)));
    }

    @Test
    public void read_readsData() throws IOException {
        Path sourceFile = tempPath();
        writeFile(sourceFile);
        Path destinationFile = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        binaryFileFs.read(getOutputStreamForFile(destinationFile));
        assertBinaryEquals(sourceFile, destinationFile);
    }

    @Test
    public void read_gz() throws IOException {
        ByteArrayOutputStream gzData = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzData);
        gzipOutputStream.write(BYTES, 0, BYTES.length);
        gzipOutputStream.close();
        assertThat("verify gz", gzData.toByteArray(), is(not(BYTES)));

        Path gzFile = tempPath();
        writeFile(gzFile, gzData.toByteArray());

        BinaryFileFsImpl gzBinaryFile = new BinaryFileFsImpl(gzFile);

        ByteArrayOutputStream compressedBytesRead = new ByteArrayOutputStream();
        gzBinaryFile.read(compressedBytesRead);
        assertThat("read compressed", compressedBytesRead.toByteArray(), is(gzData.toByteArray()));

        ByteArrayOutputStream decompressedBytesRead = new ByteArrayOutputStream();
        gzBinaryFile.read(decompressedBytesRead, true);
        assertThat("read decompressed", decompressedBytesRead.toByteArray(), is(BYTES));
    }

    private void writeFile(Path file, byte[] byteArray) throws IOException {
        Files.write(file, byteArray);
    }

    @Test
    public void size_gz() throws IOException {
        ByteArrayOutputStream gzData = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzData);

        gzipOutputStream.write(BYTES, 0, BYTES.length);
        gzipOutputStream.close();
        assertThat("verify gz", gzData.toByteArray(), is(not(DATA)));

        Path gzFile = tempPath();
        writeFile(gzFile, gzData.toByteArray());

        BinaryFileFsImpl gzBinaryFile = new BinaryFileFsImpl(gzFile);

        assertThat("size compressed", gzBinaryFile.size(false),
                is((long) gzData.toByteArray().length));

        assertThat("size decompressed", gzBinaryFile.size(true),
                is((long) BYTES.length));
    }

    @Test
    public void read_bz2() throws IOException {
        ByteArrayOutputStream bz2Data = new ByteArrayOutputStream();
        BZip2CompressorOutputStream bz2Out = new BZip2CompressorOutputStream(bz2Data);
        bz2Out.write(BYTES, 0, BYTES.length);
        bz2Out.close();
        assertThat("verify bz2", bz2Data.toByteArray(), is(not(BYTES)));

        Path bz2File = tempPath();
        writeFile(bz2File, bz2Data.toByteArray());

        BinaryFileFsImpl bz2BinaryFile = new BinaryFileFsImpl(bz2File);

        ByteArrayOutputStream compressedBytesRead = new ByteArrayOutputStream();
        bz2BinaryFile.read(compressedBytesRead);
        assertThat("read compressed", compressedBytesRead.toByteArray(),
                is(bz2Data.toByteArray()));

        ByteArrayOutputStream decompressedBytesRead = new ByteArrayOutputStream();
        bz2BinaryFile.read(decompressedBytesRead, true);
        assertThat("read decompressed", decompressedBytesRead.toByteArray(),
                is(BYTES));
    }

    @Test
    public void size_bz2() throws IOException {
        ByteArrayOutputStream bz2Data = new ByteArrayOutputStream();
        BZip2CompressorOutputStream bz2Out = new BZip2CompressorOutputStream(bz2Data);
        bz2Out.write(BYTES, 0, BYTES.length);
        bz2Out.close();
        assertThat("verify bz2", bz2Data.toByteArray(), is(not(DATA)));

        Path bz2File = tempPath();
        writeFile(bz2File, bz2Data.toByteArray());

        BinaryFileFsImpl bz2BinaryFile = new BinaryFileFsImpl(bz2File);

        assertThat("size compressed", bz2BinaryFile.size(false),
                is((long) bz2Data.toByteArray().length));

        assertThat("size decompressed", bz2BinaryFile.size(true), is(-4_348_520L));
    }

    @Test
    public void openInputStream_pathDoesNotExist_throws() throws IOException {
        Path sourceFile = tempPath();
        Files.delete(sourceFile);
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        assertThrows(IllegalStateException.class, binaryFileFs::openInputStream);
    }

    @Test
    public void openInputStream_opensStreamForReading() throws IOException {
        Path sourceFile = tempPath();
        writeFile(sourceFile);
        Path destinationFile = tempPath();
        BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try (InputStream is = binaryFileFs.openInputStream(); OutputStream os = getOutputStreamForFile(destinationFile)) {
            IOUtils.copy(is, os);
        }
        assertBinaryEquals(sourceFile, destinationFile);
    }

    private InputStream getInputStreamForFile(Path file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file.toFile()));
    }

    private OutputStream getOutputStreamForFile(Path file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file.toFile()));
    }

    private Path tempPath() throws IOException {
        return Files.createTempFile("bfs_test", "");
    }

    private void writeFile(Path path) throws IOException {
        Files.writeString(path, DATA);
    }
}
