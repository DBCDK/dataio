package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
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
    private WatchService dirMonitor;

    public JobDispatcher(Path dir) throws NullPointerException {
        this.dir = InvariantUtil.checkNotNullOrThrow(dir, "dir");
    }

    public void execute() throws IOException, InterruptedException {
        // Setup directory monitoring to start accumulating file system events
        reset();

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
    private List<TransFile> processStagnantTransfiles() throws IOException {
        List<TransFile> processedTransfiles = new ArrayList<>();
        for (TransFile transFile : getCompletedTransfiles()) {
            processedTransfiles.add(processTransfile(transFile));
        }
        return processedTransfiles;
    }

    private TransFile processTransfile(TransFile transFile) {
        LOGGER.info("Processing transfile {}", transFile.getPath());
        return transFile;
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
    private void monitorDirEvents() throws InterruptedException, IOException {
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
}
