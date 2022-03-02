package dk.dbc.dataio.gatekeeper;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.gatekeeper.operation.OperationExecutionException;
import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLogH2;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobDispatcherIT {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private Path dir;
    private Path shadowDir;
    private Path walFile;
    private WriteAheadLog wal;
    private ConnectorFactory connectorFactory = mock(ConnectorFactory.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private ShutdownManager shutdownManager;
    private Exception exception;
    private List<GatekeeperDestination> gatekeeperDestinations = ModificationFactoryTest.getGatekeeperDestinationsForTest();;


    @Before
    public void setupSystem() throws IOException {
        exception = null;
        dir = testFolder.newFolder("in").toPath();
        shadowDir = testFolder.newFolder("shadow").toPath();
        walFile = testFolder.newFolder("wal").toPath().resolve("gatekeeper.wal").toAbsolutePath();
        wal = new WriteAheadLogH2(walFile.toString());
        shutdownManager = new ShutdownManager();
    }

    @Before
    public void setupMocks() throws JobStoreServiceConnectorException,
                                    FlowStoreServiceConnectorException,
                                    FileStoreServiceConnectorException {
        when(connectorFactory.getFileStoreServiceConnector()).thenReturn(fileStoreServiceConnector);
        when(connectorFactory.getJobStoreServiceConnector()).thenReturn(jobStoreServiceConnector);
        when(fileStoreServiceConnector.addFile(any(InputStream.class))).thenReturn("fileId");
        when(flowStoreServiceConnector.findAllGatekeeperDestinations()).thenReturn(gatekeeperDestinations);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(new JobInfoSnapshot());
    }

    /*
     * Given: a filesystem with a complete transfile
     * When : the job dispatcher is started
     * Then : the transfile is processed
     */
    @Test(timeout = 5000)
    public void staticTransfilesProcessed() throws Throwable {
        // Given...
        final Path transfile = writeFile(dir, "820010.trs",
                "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\nslut");
        writeFile(dir, "820010.file", "");
        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();

            // Then...
            waitWhileFileExists(transfile);
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/820010.trs exists", Files.exists(transfile), is(false));
            assertThat("shadowDir/820010.trs exists", Files.exists(shadowDir.resolve("820010.trs")), is(true));
            assertEmptyWal();
        } finally {
            t.interrupt();
        }
    }

    /*
     * Given: a filesystem with a stalled transfile
     * When : the job dispatcher is started
     * Then : the transfile is processed
     */
    @Test(timeout = 5000)
    public void stalledTransfilesProcessed() throws Throwable {
        // Given...
        final Path transfile = writeFile(dir, "file.trs", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        stallFile(transfile);

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();

            // Then...
            waitWhileFileExists(transfile);
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/file.trs exists", Files.exists(transfile), is(false));
            assertEmptyWal();
        } finally {
            t.interrupt();
        }
    }

    /*
     * Given: a filesystem with a complete transfile
     * When : the job dispatcher is shutdown before the transfile could be processed
     * Then : the transfile modifications are stored in the WAL
     * And  : when a new job dispatcher started all WAL entries are processed
     */
    @Test(timeout = 5000)
    public void walProcessedAfterRestart() throws Throwable {
        // Given...
        final Path transfile = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\nslut");
        writeFile(dir, "820010.file", "");
        JobDispatcher jobDispatcher = getJobDispatcher();

        Thread t = getJobDispatcherThread(jobDispatcher);
        try {
            // When...
            shutdownManager.signalShutdownInProgress();
            t.start();

            // Then...
            waitWhileNoException();
            assertThat("Exception from thread", exception instanceof InterruptedException, is(true));
            assertThat("dir/820010.trans exists", Files.exists(transfile), is(true));
            assertThat("shadowDir/820010.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(false));
            assertWalSize(4);
        } finally {
            t.interrupt();
        }

        // And...
        shutdownManager = new ShutdownManager();
        jobDispatcher = getJobDispatcher();
        t = getJobDispatcherThread(jobDispatcher);
        try {
            t.start();

            waitWhileFileExists(transfile);
            assertThat("dir/820010.trans exists", Files.exists(transfile), is(false));
            assertThat("shadowDir/820010.trans exists", Files.exists(shadowDir.resolve("820010.trans")), is(true));
            assertEmptyWal();
        } finally {
            t.interrupt();
        }
    }

    /*
     * Given: a WAL with an already locked modification
     * When : the job dispatcher is started
     * Then : an exception is thrown
     */
    @Test(timeout = 5000)
    public void walContainsAlreadyLockedModification() throws InterruptedException {
        // Given...
        insertLockedWalModification();

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();

            // Then...
            waitWhileNoException();
            assertThat("ModificationLockedException exception from thread",
                    exception instanceof ModificationLockedException, is(true));
        } finally {
            t.interrupt();
        }
    }

    /*
     * Given: a filesystem with an incomplete transfile
     * When : the job dispatcher is started
     * Then : the transfile is no processed processed
     * And  : nothing as added to the WAL
     * When : the transfile is updated to be complete
     * Then : the transfile is processed
     */
    @Test(timeout = 5000)
    public void fileChangesDetected() throws InterruptedException {
        if (SystemUtil.isOsX()) {  // Do NOT run on Mac OsX, because WatchService is poll based on OsX, and has a long poll time (10 sec)
            return;
        }

        // Given...
        final Path transfile = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2\n");
        writeFile(dir, "820010.file", "");

        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();
            Thread.sleep(500);

            // Then...
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/820010.trans exists", Files.exists(transfile), is(true));
            assertThat("shadowDir/820010.trans exists", Files.exists(shadowDir.resolve("820010.trans")), is(false));

            // And...
            assertEmptyWal();

            // When...
            appendToFile(transfile, "slut");

            // Then...
            waitWhileFileExists(transfile);
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/820010.trans exists", Files.exists(transfile), is(false));
            assertThat("shadowDir/820010.trans exists", Files.exists(shadowDir.resolve("820010.trans")), is(true));
            assertEmptyWal();
        } finally {
            t.interrupt();
        }
    }

    /*
     * Given: a filesystem with two incomplete transfiles
     * When : the job dispatcher is started
     * And  : afterwards one of the transfiles becomes stalled
     * When : the other transfile is updated to be complete
     * Then : the stalled transfile is also processed
     */
    @Test(timeout = 5000)
    public void checkForStalledTransfilesExecutedOnTransfileCompletedEvent() throws InterruptedException {
        if (SystemUtil.isOsX()) {  // Do NOT run on Mac OsX, because WatchService is poll based on OsX, and has a long poll time (10 sec)
            return;
        }

        // Given...
        final Path stalledTransfile = writeFile(dir, "820010.trans", "b=danbib,f=820010.file,t=lin,c=latin-1,o=marc2");
        final Path transfile = writeFile(dir, "820030.trans", "b=danbib,f=820030.file,t=lin,c=latin-1,o=marc2\n");
        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();
            Thread.sleep(500);

            // And...
            assertThat("dir/820010.trans exists", Files.exists(stalledTransfile), is(true));
            stallFile(stalledTransfile);

            // When...
            appendToFile(transfile, "slut");

            // Then...
            waitWhileFileExists(stalledTransfile);
            assertThat("No exception from thread", exception, is(nullValue()));
        } finally {
            t.interrupt();
        }
    }

    private Thread getJobDispatcherThread(final JobDispatcher jobDispatcher) {
        return new Thread(() -> {
            try {
                jobDispatcher.execute();
            } catch (IOException | InterruptedException | OperationExecutionException | ModificationLockedException e) {
                exception = e;
            }
        });
    }

    private JobDispatcher getJobDispatcher() {
        return new JobDispatcher(dir, shadowDir, wal, connectorFactory, shutdownManager);
    }

    private void waitWhileFileExists(Path file) throws InterruptedException {
        while (Files.exists(file))
            Thread.sleep(100);
    }

    private void waitWhileNoException() throws InterruptedException {
        while (exception == null)
            Thread.sleep(100);
    }

    private Path writeFile(Path folder, String filename, String content) {
        try {
            return Files.write(folder.resolve(filename), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path appendToFile(Path file, String content) {
        try {
            return Files.write(file, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Connection newWalConnection() {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(
                    String.format("jdbc:h2:file:%s", walFile),
                    "gatekeeper", "gatekeeper");
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void assertEmptyWal() {
        assertWalSize(0);
    }

    private void assertWalSize(int expectedSize) {
        try (Connection connection = newWalConnection()) {
            final String stmt = "SELECT count(*) FROM modification";
            PreparedStatement ps = null;
            try {
                ps = JDBCUtil.query(connection, stmt);
                ResultSet rs = ps.getResultSet();
                rs.next();
                assertThat("WAL size", expectedSize, is(rs.getInt(1)));
            } finally {
                JDBCUtil.closeStatement(ps);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void insertLockedWalModification() {
        try (Connection connection = newWalConnection()) {
            final String stmt = "INSERT INTO modification(id, transfileName, opcode, arg, locked) VALUES(1, 'file.trans', 'DELETE_FILE', 'file.trans', 't')";
            JDBCUtil.update(connection, stmt);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void stallFile(Path file) {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
            final FileTime lastModified = FileTime.from(
                    fileAttributes.lastAccessTime().toMillis() - JobDispatcher.STALLED_TRANSFILE_THRESHOLD_IN_MS,
                    TimeUnit.MILLISECONDS);
            Files.setLastModifiedTime(file, lastModified);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
