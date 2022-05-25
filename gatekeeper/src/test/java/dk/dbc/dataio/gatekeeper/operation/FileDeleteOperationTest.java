package dk.dbc.dataio.gatekeeper.operation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class FileDeleteOperationTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_fileArgIsNull_throws() {
        new FileDeleteOperation(null);
    }

    @Test
    public void constructor_fileArgsIsValid_returnsNewInstance() {
        final Path file = Paths.get("file");
        final FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(file);
        assertThat("instance", fileDeleteOperation, is(notNullValue()));
        assertThat("getFile()", fileDeleteOperation.getFile(), is(file));
    }

    @Test
    public void execute_filePathExists_deletesFile() throws IOException, OperationExecutionException {
        final Path file = testFolder.newFile("file").toPath();
        final FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(file);
        fileDeleteOperation.execute();
        assertThat("File exists after move", Files.exists(file), is(false));
    }

    @Test
    public void execute_deleteFails_throws() throws IOException, OperationExecutionException {
        final Path folder = testFolder.newFolder().toPath();
        final Path file = testFolder.newFile("file").toPath();
        Files.move(file, folder.resolve("file"));
        final FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(folder);
        try {
            fileDeleteOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
        }
    }
}
