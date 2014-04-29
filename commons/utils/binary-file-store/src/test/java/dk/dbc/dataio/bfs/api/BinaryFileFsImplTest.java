package dk.dbc.dataio.bfs.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static junitx.framework.FileAssert.assertBinaryEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BinaryFileFsImplTest {
    private static final byte[] DATA = "8 bytes!".getBytes();

    @Rule
    public TemporaryFolder mountPoint = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
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
        } catch (IllegalArgumentException e) {
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
    public void read_osArgIsNull_throws() throws IOException {
        final Path sourceFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        try {
            binaryFileFs.read(null);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
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
        final Path destinationFile = mountPoint.newFile().toPath();
        final BinaryFileFsImpl binaryFileFs = new BinaryFileFsImpl(sourceFile);
        binaryFileFs.read(getOutputStreamForFile(destinationFile));
        assertBinaryEquals(sourceFile.toFile(), destinationFile.toFile());
    }

    private InputStream getInputStreamForFile(Path file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file.toFile()));
    }

    private OutputStream getOutputStreamForFile(Path file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file.toFile()));
    }

    private void writeFile(Path path) throws IOException {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
            int iterations = (2 * BinaryFileFsImpl.BUFFER_SIZE) / DATA.length;
            while (iterations > 0) {
                bos.write(DATA, 0, DATA.length);
                iterations--;
            }
            bos.flush();
        }
    }

}