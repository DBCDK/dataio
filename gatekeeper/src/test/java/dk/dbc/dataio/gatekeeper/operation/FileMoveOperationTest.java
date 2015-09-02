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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FileMoveOperationTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_sourceArgIsNull_throws() {
        new FileMoveOperation(null, Paths.get("destination"));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new FileMoveOperation(Paths.get("source"), null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Path source = Paths.get("source");
        final Path destination = Paths.get("destination");
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination);
        assertThat("instance", fileMoveOperation, is(notNullValue()));
        assertThat("getSource()", fileMoveOperation.getSource(), is(source));
        assertThat("getDestination()", fileMoveOperation.getDestination(), is(destination));
    }

    @Test
    public void execute_sourcePathDoesNotExist_returns() throws OperationExecutionException {
        final Path source = Paths.get("source");
        final Path destination = Paths.get("destination");
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination);
        fileMoveOperation.execute();
    }

    @Test
    public void execute_sourcePathExists_movesToDestination() throws IOException, OperationExecutionException {
        final Path source = testFolder.newFile("file").toPath();
        final Path destination = testFolder.newFolder().toPath();
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, destination.resolve("file"));
        fileMoveOperation.execute();
        assertThat("Source file exists after move", Files.exists(source), is(false));
        assertThat("Destination file exists after move", Files.exists(destination.resolve("file")), is(true));
    }

    @Test
    public void execute_moveFails_throws() throws IOException, OperationExecutionException {
        final Path source = testFolder.newFile("file").toPath();
        final FileMoveOperation fileMoveOperation = new FileMoveOperation(source, testFolder.newFolder().toPath());
        try {
            fileMoveOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
        }
    }
}