/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gatekeeper;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.gatekeeper.operation.OperationExecutionException;
import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLog;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLogH2;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
    private ShutdownManager shutdownManager;
    private Exception exception;

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
    public void setupMocks() throws JobStoreServiceConnectorException {
        when(connectorFactory.getFileStoreServiceConnector()).thenReturn(fileStoreServiceConnector);
        when(connectorFactory.getJobStoreServiceConnector()).thenReturn(jobStoreServiceConnector);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(new JobInfoSnapshotBuilder().build());
    }

    /*
     * Given: a filesystem with a complete transfile
     * When : the job dispatcher is started
     * Then : the transfile is processed
     */
    @Test(timeout = 5000)
    public void stagnantTransfilesProcessed() throws Throwable {
        // Given...
        writeFile(dir, "file.trans", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2\nslut");
        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();

            // Then...
            waitWhileFileExists(dir.resolve("file.trans"));
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/file.trans exists", Files.exists(dir.resolve("file.trans")), is(false));
            assertThat("shadowDir/file.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(true));
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
        writeFile(dir, "file.trans", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2\nslut");
        JobDispatcher jobDispatcher = getJobDispatcher();

        Thread t = getJobDispatcherThread(jobDispatcher);
        try {
            // When...
            shutdownManager.signalShutdownInProgress();
            t.start();

            // Then...
            waitWhileNoException();
            assertThat("Exception from thread", exception instanceof InterruptedException, is(true));
            assertThat("dir/file.trans exists", Files.exists(dir.resolve("file.trans")), is(true));
            assertThat("shadowDir/file.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(false));
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

            waitWhileFileExists(dir.resolve("file.trans"));
            assertThat("dir/file.trans exists", Files.exists(dir.resolve("file.trans")), is(false));
            assertThat("shadowDir/file.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(true));
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
        if (isOsX()) {  // Do NOT run on Mac OsX, because WatchService is poll based on OsX, and has a long poll time (10 sec)
            return;
        }

        // Given...
        final Path transfile = writeFile(dir, "file.trans", "b=danbib,f=123456.file,t=lin,c=latin-1,o=marc2\n");
        final JobDispatcher jobDispatcher = getJobDispatcher();
        final Thread t = getJobDispatcherThread(jobDispatcher);

        try {
            // When...
            t.start();
            Thread.sleep(500);

            // Then...
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/file.trans exists", Files.exists(dir.resolve("file.trans")), is(true));
            assertThat("shadowDir/file.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(false));

            // And...
            assertEmptyWal();

            // When...
            appendToFile(transfile, "slut");

            // Then...
            waitWhileFileExists(dir.resolve("file.trans"));
            assertThat("No exception from thread", exception, is(nullValue()));
            assertThat("dir/file.trans exists", Files.exists(dir.resolve("file.trans")), is(false));
            assertThat("shadowDir/file.trans exists", Files.exists(shadowDir.resolve("file.trans")), is(true));
            assertEmptyWal();
        } finally {
            t.interrupt();
        }
    }

    private Thread getJobDispatcherThread(final JobDispatcher jobDispatcher) {
        return new Thread(new Runnable(){
            public void run(){
                try {
                    jobDispatcher.execute();
                } catch (IOException | InterruptedException | OperationExecutionException | ModificationLockedException e) {
                    exception = e;
                }
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

    private boolean isOsX() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }
}
