package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TransFileTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_transfileArgIsNull_throws() throws IOException {
        new TransFile(null);
    }

    @Test(expected = NoSuchFileException.class)
    public void constructor_transfileDoesNotExist_throws() throws IOException {
        new TransFile(Paths.get("no-such-file"));
    }

    @Test
    public void constructor_transfileContainsEmptyLines_emptyLinesAreSkipped() throws IOException {
        final String line1 = "b=base1,f=file1";
        final String line2 = "b=base2,f=file2";
        final Path file = testFolder.newFile().toPath();
        final StringBuilder content = new StringBuilder()
                .append(line1).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(line2).append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("slut");
        Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat("Number of transfile lines", transFile.getLines().size(), is(2));
        assertThat("Transfile line 1", transFile.getLines().get(0).getLine(), is(line1));
        assertThat("Transfile line 2", transFile.getLines().get(1).getLine(), is(line2));
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void isComplete_transfileIsNotComplete_returnsFalse() throws IOException {
        final TransFile transFile = new TransFile(testFolder.newFile().toPath());
        assertThat(transFile.isComplete(), is(false));
    }

    @Test
    public void isComplete_transfileIsComplete_returnsTrue() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "slut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.isComplete(), is(true));
    }

    @Test
    public void getLines_whenLinesExist_returnsUnmodifiableList() throws IOException {
        final Path file = testFolder.newFile().toPath();
        Files.write(file, "slut".getBytes(StandardCharsets.UTF_8));
        final TransFile transFile = new TransFile(file);
        try {
            transFile.getLines().add(null);
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void getPath_returnsPathOfTransfile() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.getPath(), is(file));
    }

    @Test
    public void exists_transfileExistsOnTheFileSystem_returnsTrue() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        assertThat(transFile.exists(), is(true));
    }

    @Test
    public void exists_transfileDoesNotExistOnTheFileSystem_returnsFalse() throws IOException {
        final Path file = testFolder.newFile().toPath();
        final TransFile transFile = new TransFile(file);
        Files.delete(file);
        assertThat(transFile.exists(), is(false));
    }
}