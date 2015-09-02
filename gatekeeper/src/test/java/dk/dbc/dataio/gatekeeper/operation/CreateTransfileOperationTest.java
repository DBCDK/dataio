package dk.dbc.dataio.gatekeeper.operation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CreateTransfileOperationTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_destinationArgIsNull_throws() {
        new CreateTransfileOperation(null, "content");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new CreateTransfileOperation(Paths.get("destination"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_contentArgIsEmpty_throws() {
        new CreateTransfileOperation(Paths.get("destination"), " ");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final Path destination = Paths.get("destination");
        final String content = "content";
        final CreateTransfileOperation createTransfileOperation = new CreateTransfileOperation(destination, content);
        assertThat("instance", createTransfileOperation, is(notNullValue()));
        assertThat("getDestination()", createTransfileOperation.getDestination(), is(destination));
        assertThat("getContent()", createTransfileOperation.getContent(), is(content));
    }

    @Test
    public void execute_destinationPathCanBeWritten_createsFile() throws IOException, OperationExecutionException {
        final Path destination = Paths.get("destination");
        try {
            final String content = "content";
            final CreateTransfileOperation createTransfileOperation = new CreateTransfileOperation(destination, content);
            createTransfileOperation.execute();
            assertThat("File is written", Files.exists(destination), is(true));
            final String actualContent = new String(Files.readAllBytes(destination), StandardCharsets.UTF_8);
            assertThat("File content", actualContent, is(content));
        } finally {
            Files.delete(destination);
        }
    }

    @Test
    public void execute_fileCreationFails_throws() throws IOException, OperationExecutionException {
        final Path destination = testFolder.newFile("destination").toPath();
        final String content = "content";
        final CreateTransfileOperation createTransfileOperation = new CreateTransfileOperation(destination, content);
        try {
            createTransfileOperation.execute();
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
        }
    }
}