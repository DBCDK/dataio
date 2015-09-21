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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gatekeeper.operation.CreateJobOperation;
import dk.dbc.dataio.gatekeeper.operation.CreateTransfileOperation;
import dk.dbc.dataio.gatekeeper.operation.FileDeleteOperation;
import dk.dbc.dataio.gatekeeper.operation.FileMoveOperation;
import dk.dbc.dataio.gatekeeper.operation.Operation;
import dk.dbc.dataio.gatekeeper.operation.OperationExecutionException;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.gatekeeper.wal.Modification;
import dk.dbc.dataio.gatekeeper.wal.ModificationLockedException;
import dk.dbc.dataio.gatekeeper.wal.WriteAheadLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

public class JobDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobDispatcher.class);
    private static final String TRANSFILE_EXTENSION = ".trans";

    private final Path dir;
    private final Path shadowDir;
    private final WriteAheadLog wal;
    private final ConnectorFactory connectorFactory;
    private final ShutdownManager shutdownManager;

    private WatchService dirMonitor;

    public JobDispatcher(Path dir, Path shadowDir, WriteAheadLog wal, ConnectorFactory connectorFactory,
                         ShutdownManager shutdownManager)
            throws NullPointerException {
        this.dir = InvariantUtil.checkNotNullOrThrow(dir, "dir");
        this.shadowDir = InvariantUtil.checkNotNullOrThrow(shadowDir, "shadowDir");
        this.wal = InvariantUtil.checkNotNullOrThrow(wal, "wal");
        this.connectorFactory = InvariantUtil.checkNotNullOrThrow(connectorFactory, "connectorFactory");
        this.shutdownManager = InvariantUtil.checkNotNullOrThrow(shutdownManager, "shutdownManager");
    }

    public void execute() throws IOException, InterruptedException, ModificationLockedException, OperationExecutionException {
        // Setup directory monitoring to start accumulating file system events
        reset();
        // Process any existing entries in the write-ahead-log
        processWal();
        // Process all stagnant file that may not experience any future file system events
        processStagnantTransfiles();
        // Wait for and process file system events
        monitorDirEvents();
    }

    /* Setup directory monitoring to start accumulating file system events
     */
    public void reset() throws IOException {
        close();
        LOGGER.info("Starting monitoring of {}", dir.toAbsolutePath());
        dirMonitor = FileSystems.getDefault().newWatchService();
        dir.register(dirMonitor,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public void close() throws IOException {
        if (dirMonitor != null) {
            LOGGER.info("Stopping monitoring of {}", dir.toAbsolutePath());
            dirMonitor.close();
        }
    }

    /* Process all stagnant file that may not experience any future file system events
     */
    private void processStagnantTransfiles()
            throws IOException, ModificationLockedException, OperationExecutionException, InterruptedException {
        for (TransFile transFile : getCompleteTransfiles()) {
            processTransfile(transFile);
        }
    }

    /* Process all modifications currently contained in the WAL
     */
    private void processWal() throws ModificationLockedException, OperationExecutionException, InterruptedException {
        Modification next = wal.next();
        while (next != null) {
            LOGGER.info("Processing WAL entry {}", next);
            processModification(next);
            next = wal.next();
        }
    }

    /* Wait for and process file system events
     */
    private void monitorDirEvents() throws InterruptedException, IOException,
                                           ModificationLockedException, OperationExecutionException {
        // Start the infinite polling loop
        while (true) {
            final WatchKey key = dirMonitor.take();
            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                if (StandardWatchEventKinds.OVERFLOW == watchEvent.kind()) {
                    // Too many events accumulated - throw exception to enable the caller
                    // to reset this job dispatcher
                    throw new IllegalStateException("Directory monitoring overflowed");
                } else {
                    final Path file = dir.resolve((Path) watchEvent.context());
                    processIfCompleteTransfile(file);
                }
            }
            key.reset();
        }
    }

    /**
     * Processes given file if complete transfile
     * @param file file to test and possibly process
     * @return true if transfile was processed, false if not
     * @throws IOException if unable to read transfile
     * @throws ModificationLockedException if WAL modification is already locked
     * @throws OperationExecutionException if an operation was unable to complete successfully
     * @throws InterruptedException if shutdown was detected before WAL could be emptied
     */
    boolean processIfCompleteTransfile(Path file) throws IOException, ModificationLockedException,
                                                         OperationExecutionException, InterruptedException {
        if (file.getFileName().toString().endsWith(TRANSFILE_EXTENSION)) {
            final TransFile transFile = new TransFile(file);
            if (!transFile.exists()) {
                LOGGER.warn("Transfile {} no longer exists in the file system", file);
                return false;
            }
            if (transFile.isComplete()) {
                processTransfile(transFile);
                return true;
            } else {
                LOGGER.warn("Transfile {} is incomplete", file);
            }
        }
        return false;
    }

    /**
     * First stores all modifications for given transfile into the WAL,
     * and then subsequently executes their corresponding operations one
     * after another
     * @param transfile transfile for which modifications are to be added and processed
     * @throws ModificationLockedException if WAL modification is already locked
     * @throws OperationExecutionException if an operation was unable to complete successfully
     * @throws InterruptedException if shutdown was detected before WAL could be emptied
     */
    void processTransfile(TransFile transfile)
            throws ModificationLockedException, OperationExecutionException, InterruptedException {
        LOGGER.info("Processing transfile {}", transfile.getPath());
        writeWal(transfile);
        processWal();
    }

    /**
     * @return list of complete transfiles found in the monitored directory
     * @throws IOException on failure to search working directory or
     * if unable to read a found transfile
     */
    List<TransFile> getCompleteTransfiles() throws IOException {
        final ArrayList<TransFile> transfiles = new ArrayList<>();
        for (Path transfilePath : FileFinder.findFilesWithExtension(dir, TRANSFILE_EXTENSION)) {
            final TransFile transfile = new TransFile(transfilePath);
            if (transfile.isComplete()) {
                transfiles.add(transfile);
            } else {
                LOGGER.warn("Transfile {} is incomplete", transfilePath);
            }
        }
        return transfiles;
    }

    /**
     * Loads the necessary modifications for a given transfile into the WAL
     * @param transfile transfile for which modifications are to be added
     */
    void writeWal(TransFile transfile) {
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        wal.add(modificationFactory.getModifications());
    }

    /**
     * Converts given modification into its corresponding operation and executes it
     * @param modification modification to carry out
     * @throws OperationExecutionException if operation failed to complete (the
     * modification will also be unlocked in the WAL)
     * @throws InterruptedException shutdown-in-progress state is detected in the
     * shutdownManager. The modification will also be unlocked in the WAL.
     */
    void processModification(Modification modification) throws OperationExecutionException, InterruptedException {
        if (shutdownManager.signalBusy()) {
            try {
                final Operation operation = getOperation(modification);
                LOGGER.info("Executing {}", operation.getOpcode());
                operation.execute();
                wal.delete(modification);
            } catch (OperationExecutionException e) {
                // We need to unlock this modification, so that it will not
                // throw a ModificationLockedException when this job dispatcher
                // is reset
                wal.unlock(modification);
                throw e;
            } finally {
                shutdownManager.signalReadyToExit();
            }
        } else {
            // We didn't get the chance to work on this modification,
            // so it should be safe to unlock
            try {
                wal.unlock(modification);
            } catch (Exception e) {
                LOGGER.error("Unable to unlock modification {}", modification, e);
            }
            // Slight misuse of InterruptedException, but shutdown is
            // in progress and we need to exit in a consistent state
            throw new InterruptedException("Shutdown detected");
        }
    }

    /**
     * Maps modification to operation
     * @param modification WAL modification
     * @return operation matching modification
     * @throws IllegalStateException if given modification with unknown opcode
     */
    Operation getOperation(Modification modification) throws IllegalStateException {
        Operation op;
        switch (modification.getOpcode()) {
            case DELETE_FILE:
                op = new FileDeleteOperation(dir.resolve(modification.getArg()).toAbsolutePath());
                break;
            case MOVE_FILE:
                final Path source = dir.resolve(modification.getArg()).toAbsolutePath();
                op = new FileMoveOperation(source, shadowDir.resolve(source.getFileName()).toAbsolutePath());
                break;
            case CREATE_TRANSFILE:
                op = new CreateTransfileOperation(shadowDir.resolve(modification.getTransfileName()).toAbsolutePath(),
                        modification.getArg());
                break;
            case CREATE_JOB:
                op = new CreateJobOperation(connectorFactory.getJobStoreServiceConnector(),
                        connectorFactory.getFileStoreServiceConnector(), dir.toAbsolutePath(),
                        modification.getTransfileName(), modification.getArg());
                break;
            default:
                throw new IllegalStateException(String.format("Unhandled opcode '%s'", modification.getOpcode()));
        }
        return op;
    }
}
