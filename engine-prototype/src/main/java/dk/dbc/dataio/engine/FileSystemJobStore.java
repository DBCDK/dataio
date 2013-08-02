package dk.dbc.dataio.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemJobStore implements JobStore {
    private static final Logger log = LoggerFactory.getLogger(FileSystemJobStore.class);

    private final Path storePath;

    public FileSystemJobStore(Path storePath) throws JobStoreException {
        if (storePath == null) {
            throw new NullPointerException("storePath can not be null");
        }
        if (!canUseExistingStorePath(storePath)) {
            createDirectory(storePath);
        }
        this.storePath = storePath;

        log.info("Placing job store in {}", this.storePath);
    }

    @Override
    public Job createJob(Path dataObjectPath, FlowInfo flowInfo) throws JobStoreException {
        final long jobId = System.currentTimeMillis();
        final Path jobPath = getJobPath(jobId);

        log.info("Creating job in {}", jobPath);
        createDirectory(FileSystems.getDefault().getPath(storePath.toString(), Long.toString(jobId)));

        return new Job(jobId, dataObjectPath);
    }

    private boolean canUseExistingStorePath(Path storePath) throws JobStoreException {
        if (Files.exists(storePath)) {
            if (!Files.isDirectory(storePath)) {
                throw new JobStoreException(String.format("Job store path is not a directory: %s", storePath));
            }
            return true;
        }
        return false;
    }

    private void createDirectory(Path dir) throws JobStoreException {
        try {
            Files.createDirectory(dir);
        } catch (IOException e) {
            throw new JobStoreException(String.format("Unable to create directory: %s", dir), e);
        }
    }

    private Path getJobPath(long jobId) {
        return FileSystems.getDefault().getPath(storePath.toString(), Long.toString(jobId));
    }

    public static FileSystemJobStore newFileSystemJobStore(Path storePath) throws JobStoreException {
        return new FileSystemJobStore(storePath);
    }
}
