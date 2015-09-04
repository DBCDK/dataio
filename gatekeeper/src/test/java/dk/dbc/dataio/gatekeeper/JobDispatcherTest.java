package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.gatekeeper.operation.CreateJobOperation;
import dk.dbc.dataio.gatekeeper.operation.CreateTransfileOperation;
import dk.dbc.dataio.gatekeeper.operation.FileDeleteOperation;
import dk.dbc.dataio.gatekeeper.operation.FileMoveOperation;
import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.operation.Operation;
import dk.dbc.dataio.gatekeeper.operation.OperationExecutionException;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.MockedWriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.Modification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDispatcherTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private Path dir;
    private Path shadowDir;
    private MockedWriteAheadLog wal = new MockedWriteAheadLog();
    private ConnectorFactory connectorFactory = mock(ConnectorFactory.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);

    @Before
    public void setupFileSystem() throws IOException {
        dir = testFolder.newFolder("in").toPath();
        shadowDir = testFolder.newFolder("shadow").toPath();
    }

    @Before
    public void setupMocks() {
        wal.modifications.clear();
        when(connectorFactory.getFileStoreServiceConnector()).thenReturn(fileStoreServiceConnector);
        when(connectorFactory.getJobStoreServiceConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dirArgIsNull_throws() {
        new JobDispatcher(null, shadowDir, wal, connectorFactory);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_shadowDirArgIsNull_throws() {
        new JobDispatcher(dir, null, wal, connectorFactory);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_walArgIsNull_throws() {
        new JobDispatcher(dir, shadowDir, null, connectorFactory);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_connectorFactoryArgIsNull_throws() {
        new JobDispatcher(dir, shadowDir, wal, null);
    }

    @Test
    public void writeWal_addModificationsToWal() throws IOException {
        final Path transfilePath = writeFile(dir, "file.trans", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2");
        final TransFile transFile = new TransFile(transfilePath);
        final JobDispatcher jobDispatcher = getJobDispatcher();
        jobDispatcher.writeWal(transFile);
        assertThat("Number of WAL modifications", wal.modifications.size(), is(4));
    }

    @Test
    public void processModification_executesOperation() throws OperationExecutionException {
        final String filename = "file";
        writeFile(dir, filename, "data");
        final Modification modification = new Modification(42L);
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg(filename);

        final JobDispatcher jobDispatcher = getJobDispatcher();
        jobDispatcher.processModification(modification);

        assertThat(Files.exists(dir.resolve(filename)), is(false));
    }

    @Test
    public void processModification_operationThrows_unlocksAndRethrows() throws OperationExecutionException, IOException {
        // Delete operation will fail when trying to delete a
        // non-empty folder
        final Path subdir = dir.resolve("subdir");
        writeFile(Files.createDirectory(subdir), "tmp", "data");
        final Modification modification = new Modification(42L);
        modification.lock();
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg(subdir.getFileName().toString());

        final JobDispatcher jobDispatcher = getJobDispatcher();
        try {
            jobDispatcher.processModification(modification);
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat(modification.isLocked(), is(false));
        }
    }

    @Test
    public void getOperation_modificationArgHasDeleteFileOpcode_returnsFileDeleteOperation() {
        final Modification modification = new Modification();
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg("file");

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Operation operation = jobDispatcher.getOperation(modification);
        assertThat("Operation", operation, is(notNullValue()));
        assertThat("Operation.getOpcode()", operation.getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("FileDeleteOperation.getFile()", ((FileDeleteOperation) operation).getFile(),
                is(dir.resolve(modification.getArg()).toAbsolutePath()));
    }

    @Test
    public void getOperation_modificationArgHasMoveFileOpcode_returnsFileMoveOperation() {
        final Modification modification = new Modification();
        modification.setOpcode(Opcode.MOVE_FILE);
        modification.setArg("file");

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Operation operation = jobDispatcher.getOperation(modification);
        assertThat("Operation", operation, is(notNullValue()));
        assertThat("Operation.getOpcode()", operation.getOpcode(), is(Opcode.MOVE_FILE));
        assertThat("FileMoveOperation.getSource()", ((FileMoveOperation) operation).getSource(),
                is(dir.resolve(modification.getArg()).toAbsolutePath()));
        assertThat("FileMoveOperation.getDestination()", ((FileMoveOperation) operation).getDestination(),
                is(shadowDir.resolve(modification.getArg()).toAbsolutePath()));
    }

    @Test
    public void getOperation_modificationArgHasCreateTransfileOpcode_returnsCreateTransfileOperation() {
        final Modification modification = new Modification();
        modification.setOpcode(Opcode.CREATE_TRANSFILE);
        modification.setTransfileName("file");
        modification.setArg("line");

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Operation operation = jobDispatcher.getOperation(modification);
        assertThat("Operation", operation, is(notNullValue()));
        assertThat("Operation.getOpcode()", operation.getOpcode(), is(Opcode.CREATE_TRANSFILE));
        assertThat("CreateTransfileOperation.getDestination()", ((CreateTransfileOperation) operation).getDestination(),
                is(shadowDir.resolve(modification.getTransfileName()).toAbsolutePath()));
        assertThat("CreateTransfileOperation.getContent()", ((CreateTransfileOperation) operation).getContent(),
                is(modification.getArg()));
    }

    @Test
    public void getOperation_modificationArgHasCreateJobOpcode_returnsCreateJobOperation() {
        final Modification modification = new Modification();
        modification.setOpcode(Opcode.CREATE_JOB);
        modification.setArg("line");

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Operation operation = jobDispatcher.getOperation(modification);
        assertThat("Operation", operation, is(notNullValue()));
        assertThat("Operation.getOpcode()", operation.getOpcode(), is(Opcode.CREATE_JOB));
        assertThat("CreateJobOperation.getJobStoreServiceConnector()", ((CreateJobOperation) operation).getJobStoreServiceConnector(),
                is(jobStoreServiceConnector));
        assertThat("CreateJobOperation.getFileStoreServiceConnector()", ((CreateJobOperation) operation).getFileStoreServiceConnector(),
                is(fileStoreServiceConnector));
        assertThat("CreateJobOperation.getWorkingDir()", ((CreateJobOperation) operation).getWorkingDir().toAbsolutePath(),
                is(dir.toAbsolutePath()));
        assertThat("CreateJobOperation.getTransfileData()", ((CreateJobOperation) operation).getTransfileData(),
                is(modification.getArg()));
    }

    private JobDispatcher getJobDispatcher() {
        return new JobDispatcher(dir, shadowDir, wal, connectorFactory);
    }

    private Path writeFile(Path folder, String filename, String content) {
        try {
            return Files.write(folder.resolve(filename), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}