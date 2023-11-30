package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.gatekeeper.operation.CreateJobOperation;
import dk.dbc.dataio.gatekeeper.operation.FileDeleteOperation;
import dk.dbc.dataio.gatekeeper.operation.Opcode;
import dk.dbc.dataio.gatekeeper.operation.Operation;
import dk.dbc.dataio.gatekeeper.operation.OperationExecutionException;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.MockedWriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.Modification;
import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDispatcherTest {
    @TempDir
    public Path testFolder;

    private Path dir;
    private MockedWriteAheadLog wal = new MockedWriteAheadLog();
    private final ConnectorFactory connectorFactory = mock(ConnectorFactory.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private ShutdownManager shutdownManager;
    private final List<GatekeeperDestination> gatekeeperDestinations = ModificationFactoryTest.getGatekeeperDestinationsForTest();

    @BeforeEach
    public void setupFileSystem() throws IOException {
        dir = Files.createDirectory(testFolder.resolve("in"));
    }

    @BeforeEach
    public void setupMocks() throws JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException,
            FileStoreServiceConnectorException {
        wal = new MockedWriteAheadLog();
        shutdownManager = new ShutdownManager();
        when(connectorFactory.getFileStoreServiceConnector()).thenReturn(fileStoreServiceConnector);
        when(connectorFactory.getJobStoreServiceConnector()).thenReturn(jobStoreServiceConnector);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(new JobInfoSnapshot());
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn("fileId");
        when(flowStoreServiceConnector.findAllGatekeeperDestinations()).thenReturn(gatekeeperDestinations);
    }

    @Test
    public void constructor_dirArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JobDispatcher(null, wal, connectorFactory, shutdownManager));
    }

    @Test
    public void constructor_walArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JobDispatcher(dir, null, connectorFactory, shutdownManager));
    }

    @Test
    public void constructor_connectorFactoryArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JobDispatcher(dir, wal, null, shutdownManager));
    }

    @Test
    public void constructor_shutdownManagerArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JobDispatcher(dir, wal, connectorFactory, null));
    }

    @Test
    public void processIfCompleteTransfile_fileDoesNotHaveTransfileExtension_notProcessedReturnsFalse()
            throws OperationExecutionException, ModificationLockedException, InterruptedException {
        Path filePath = writeFile(dir, "file.dat", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2");
        JobDispatcher jobDispatcher = getJobDispatcher();
        assertThat("processIfCompleteTransfile()", jobDispatcher.processIfCompleteTransfile(filePath), is(false));
        assertThat("WAL modifications", wal.modificationsAddedOverTime, is(0));
    }

    @Test
    public void processIfCompleteTransfile_fileDoesNotExist_notProcessedReturnsFalse()
            throws OperationExecutionException, ModificationLockedException, InterruptedException {
        Path transfilePath = dir.resolve("file.trans");
        JobDispatcher jobDispatcher = getJobDispatcher();
        assertThat("processIfCompleteTransfile()", jobDispatcher.processIfCompleteTransfile(transfilePath), is(false));
        assertThat("WAL modifications", wal.modificationsAddedOverTime, is(0));
    }

    @Test
    public void processIfCompleteTransfile_fileIsIncomplete_notProcessedReturnsFalse()
            throws OperationExecutionException, ModificationLockedException, InterruptedException {
        Path transfilePath = writeFile(dir, "123456.trans", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2");
        JobDispatcher jobDispatcher = getJobDispatcher();
        assertThat("processIfCompleteTransfile()", jobDispatcher.processIfCompleteTransfile(transfilePath), is(false));
        assertThat("WAL modifications", wal.modificationsAddedOverTime, is(0));
    }

    @Test
    public void processIfCompleteTransfile_fileIsComplete_processedReturnsTrue()
            throws OperationExecutionException, ModificationLockedException, InterruptedException {
        writeFile(dir, "820010.file", "");
        Path transfilePath = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\nslut");
        JobDispatcher jobDispatcher = getJobDispatcher();
        assertThat("processIfCompleteTransfile()", jobDispatcher.processIfCompleteTransfile(transfilePath), is(true));

        // We don't assert all the modifications
        assertThat("Original transfile exists", Files.exists(transfilePath), is(false));

        // Assert WAL interaction
        assertThat("WAL is empty", wal.modifications.isEmpty(), is(true));
        assertThat("WAL modifications", wal.modificationsAddedOverTime, is(3));
    }

    @Test
    public void processTransfile()
            throws OperationExecutionException, InterruptedException, ModificationLockedException {
        writeFile(dir, "820010.file", "");
        Path transfilePath = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\nslut");
        TransFile transFile = new TransFile(transfilePath);
        JobDispatcher jobDispatcher = getJobDispatcher();
        jobDispatcher.processTransfile(transFile);

        // We don't assert all the modifications
        assertThat("Original transfile exists", Files.exists(transfilePath), is(false));

        // Assert WAL interaction
        assertThat("WAL is empty", wal.modifications.isEmpty(), is(true));
        assertThat("WAL modifications", wal.modificationsAddedOverTime, is(3));
    }

    @Test
    public void getCompleteTransfiles_noCompleteTransfiles_returnsEmptyList() throws IOException {
        writeFile(dir, "file.trans", "data");
        JobDispatcher jobDispatcher = getJobDispatcher();
        List<TransFile> completeTransfiles = jobDispatcher.getCompleteTransfiles();
        assertThat(completeTransfiles.isEmpty(), is(true));
    }

    @Test
    public void getCompleteTransfiles_returnsList() throws IOException {
        writeFile(dir, "file1.trans", "data1\nslut");
        writeFile(dir, "file2.trans", "data2\n");
        writeFile(dir, "file3.trs", "data3\nfinish");
        JobDispatcher jobDispatcher = getJobDispatcher();
        List<TransFile> completeTransfiles = jobDispatcher.getCompleteTransfiles();
        assertThat("Number of transfiles found", completeTransfiles.size(), is(2));
        Set<String> transfiles = new HashSet<>(2);
        transfiles.add(completeTransfiles.get(0).getPath().getFileName().toString());
        transfiles.add(completeTransfiles.get(1).getPath().getFileName().toString());
        assertThat("transfiles found", transfiles, containsInAnyOrder("file1.trans", "file3.trs"));
    }

    @Test
    public void getStalledIncompleteTransfiles_noStalledTransfiles_returnsEmptyList() throws IOException {
        writeFile(dir, "file1.trans", "data1");
        JobDispatcher jobDispatcher = getJobDispatcher();
        List<TransFile> stalledTransfiles = jobDispatcher.getStalledIncompleteTransfiles();
        assertThat(stalledTransfiles.isEmpty(), is(true));
    }

    @Test
    public void getStalledIncompleteTransfiles_stalledTransfilesExist_returnsList() throws IOException {
        Path path1 = writeFile(dir, "file1.trans", "data1");
        Path path2 = writeFile(dir, "file2.trs", "data2");
        Path path3 = writeFile(dir, "file3.trans", "data3\nslut");
        writeFile(dir, "file4.trans", "data4");
        BasicFileAttributes fileAttributes = Files.readAttributes(path1, BasicFileAttributes.class);
        FileTime lastModified = FileTime.from(
                fileAttributes.lastAccessTime().toMillis() - JobDispatcher.STALLED_TRANSFILE_THRESHOLD_IN_MS,
                TimeUnit.MILLISECONDS);
        Files.setLastModifiedTime(path1, lastModified); // file1.trans exceeds threshold and is incomplete
        Files.setLastModifiedTime(path2, lastModified); // file2.trs   exceeds threshold and is incomplete
        Files.setLastModifiedTime(path3, lastModified); // file3.trans exceeds threshold but is complete
        // file4.trans is incomplete but does not exceed threshold

        JobDispatcher jobDispatcher = getJobDispatcher();
        List<TransFile> stalledTransfiles = jobDispatcher.getStalledIncompleteTransfiles();
        assertThat("Number of transfiles found", stalledTransfiles.size(), is(2));
        assertThat("transfiles found", stalledTransfiles.stream()
                        .map(transfile -> transfile.getPath().getFileName().toString())
                        .collect(Collectors.toList()),
                containsInAnyOrder("file1.trans", "file2.trs"));
    }

    @Test
    public void writeWal_addModificationsToWal() {
        writeFile(dir, "820010.file", "");
        Path transfilePath = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\nslut");
        TransFile transFile = new TransFile(transfilePath);
        JobDispatcher jobDispatcher = getJobDispatcher();
        jobDispatcher.writeWal(transFile);
        assertThat("Number of WAL modifications", wal.modifications.size(), is(3));
    }

    @Test
    public void processModification_executesOperation() throws OperationExecutionException, InterruptedException {
        final String filename = "file";
        writeFile(dir, filename, "data");
        Modification modification = new Modification(42L);
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg(filename);

        JobDispatcher jobDispatcher = getJobDispatcher();
        jobDispatcher.processModification(modification);

        assertThat("file exists", Files.exists(dir.resolve(filename)), is(false));
        assertThat("shutdownManager.isReadyToExit()", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void processModification_operationThrows_unlocksAndRethrows() throws IOException, InterruptedException {
        // Delete operation will fail when trying to delete a
        // non-empty folder
        Path subdir = dir.resolve("subdir");
        writeFile(Files.createDirectory(subdir), "tmp", "data");
        Modification modification = new Modification(42L);
        modification.lock();
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg(subdir.getFileName().toString());

        JobDispatcher jobDispatcher = getJobDispatcher();
        try {
            jobDispatcher.processModification(modification);
            fail("No OperationExecutionException thrown");
        } catch (OperationExecutionException e) {
            assertThat("modification.isLocked()", modification.isLocked(), is(false));
        }
        assertThat("shutdownManager.isReadyToExit()", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void processModification_shutdownManagerInStateShutdownInProgress_unlocksAndThrows() throws OperationExecutionException {
        shutdownManager.signalShutdownInProgress();
        Modification modification = new Modification(42L);
        modification.lock();
        JobDispatcher jobDispatcher = getJobDispatcher();
        try {
            jobDispatcher.processModification(modification);
            fail("No OperationExecutionException thrown");
        } catch (InterruptedException e) {
            assertThat("modification.isLocked()", modification.isLocked(), is(false));
        }
        assertThat("shutdownManager.isReadyToExit()", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void getOperation_modificationArgHasDeleteFileOpcode_returnsFileDeleteOperation() {
        Modification modification = new Modification();
        modification.setOpcode(Opcode.DELETE_FILE);
        modification.setArg("file");

        JobDispatcher jobDispatcher = getJobDispatcher();
        Operation operation = jobDispatcher.getOperation(modification);
        assertThat("Operation", operation, is(notNullValue()));
        assertThat("Operation.getOpcode()", operation.getOpcode(), is(Opcode.DELETE_FILE));
        assertThat("FileDeleteOperation.getFile()", ((FileDeleteOperation) operation).getFile(),
                is(dir.resolve(modification.getArg()).toAbsolutePath()));
    }

    @Test
    public void getOperation_modificationArgHasCreateJobOpcode_returnsCreateJobOperation() {
        Modification modification = new Modification();
        modification.setOpcode(Opcode.CREATE_JOB);
        modification.setTransfileName("123456.001.trans");
        modification.setArg("line");

        JobDispatcher jobDispatcher = getJobDispatcher();
        Operation operation = jobDispatcher.getOperation(modification);
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

    /*
     * Private methods
     */

    private JobDispatcher getJobDispatcher() {
        return new JobDispatcher(dir, wal, connectorFactory, shutdownManager);
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
