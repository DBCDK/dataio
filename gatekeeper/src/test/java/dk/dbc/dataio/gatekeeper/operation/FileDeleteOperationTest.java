package dk.dbc.dataio.gatekeeper.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileDeleteOperationTest {
    @TempDir
    public Path testFolder;

    @Test
    public void constructor_fileArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FileDeleteOperation(null));
    }

    @Test
    public void constructor_fileArgsIsValid_returnsNewInstance() {
        Path file = Paths.get("file");
        FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(file);
        assertThat("instance", fileDeleteOperation, is(notNullValue()));
        assertThat("getFile()", fileDeleteOperation.getFile(), is(file));
    }

    @Test
    public void execute_filePathExists_deletesFile() throws IOException, OperationExecutionException {
        Path file = Files.createFile(testFolder.resolve("file"));
        FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(file);
        fileDeleteOperation.execute();
        assertThat("File exists after move", Files.exists(file), is(false));
    }

    @Test
    public void execute_deleteFails_throws() throws IOException {
        Path folder = Files.createDirectory(testFolder.resolve(UUID.randomUUID().toString()));
        Path file = Files.createFile(testFolder.resolve("file"));
        Files.move(file, folder.resolve("file"));
        FileDeleteOperation fileDeleteOperation = new FileDeleteOperation(folder);
        assertThrows(OperationExecutionException.class, fileDeleteOperation::execute);
    }
}
