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

    private WatchService dirMonitor;

    public JobDispatcher(Path dir, Path shadowDir, WriteAheadLog wal, ConnectorFactory connectorFactory)
            throws NullPointerException {
        this.dir = InvariantUtil.checkNotNullOrThrow(dir, "dir");
        this.shadowDir = InvariantUtil.checkNotNullOrThrow(shadowDir, "shadowDir");
        this.wal = InvariantUtil.checkNotNullOrThrow(wal, "wal");
        this.connectorFactory = InvariantUtil.checkNotNullOrThrow(connectorFactory, "connectorFactory");
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
    private List<TransFile> processStagnantTransfiles()
            throws IOException, ModificationLockedException, OperationExecutionException {
        List<TransFile> processedTransfiles = new ArrayList<>();
        for (TransFile transFile : getCompletedTransfiles()) {
            processTransfile(transFile);
            processedTransfiles.add(processTransfile(transFile));
        }
        return processedTransfiles;
    }

    private void processWal() throws ModificationLockedException, OperationExecutionException {
        Modification next = wal.next();
        while (next != null) {
            LOGGER.info("Processing WAL entry {}", next);
            processModification(next);
            next = wal.next();
        }
    }

    private TransFile processTransfile(TransFile transfile)
            throws ModificationLockedException, OperationExecutionException {
        LOGGER.info("Processing transfile {}", transfile.getPath());
        writeWal(transfile);
        processWal();
        return transfile;
    }

    private List<TransFile> getCompletedTransfiles() throws IOException {
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
                    LOGGER.info("Detected event on {}", file);
                    if (file.getFileName().toString().endsWith(TRANSFILE_EXTENSION)) {
                        final TransFile transFile = new TransFile(file);
                        if (!transFile.exists()) {
                            LOGGER.warn("Transfile {} no longer exists in the file system", file);
                            continue;
                        }

                        if (transFile.isComplete()) {
                            processTransfile(transFile);
                        } else {
                            LOGGER.warn("Transfile {} is incomplete", file);
                        }
                    }
                }
            }
            key.reset();
        }
    }

    void writeWal(TransFile transfile) {
        final ModificationFactory modificationFactory = new ModificationFactory(transfile);
        wal.add(modificationFactory.getModifications());
    }

    /**
     * Converts given modification into its corresponding operation
     * and executes it
     * @param modification modification to carry out
     * @throws OperationExecutionException if operation failed to complete (the
     * modification will also be unlocked in the WAL)
     */
    void processModification(Modification modification) throws OperationExecutionException {
        final Operation operation = getOperation(modification);
        LOGGER.info("Executing {}", operation.getOpcode());
        try {
            operation.execute();
        } catch (OperationExecutionException e) {
            // We need to unlock this modification, so that it will not
            // throw a ModificationLockedException when this job dispatcher
            // is reset
            wal.unlock(modification);
            throw e;
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
                        connectorFactory.getFileStoreServiceConnector(), dir.toAbsolutePath(), modification.getArg());
                break;
            default:
                throw new IllegalStateException(String.format("Unhandled opcode '%s'", modification.getOpcode()));
        }
        return op;
    }
}
