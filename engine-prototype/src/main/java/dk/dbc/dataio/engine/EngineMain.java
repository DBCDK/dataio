package dk.dbc.dataio.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class EngineMain {
    private static final Logger log = LoggerFactory.getLogger(EngineMain.class);

    private EngineMain() { }

    public static void main(String[] args) throws JobStoreException {
        log.info("Program arguments 1: {} 2: {}, 3: {}", args[0], args[1], args[2]);
        final Path jobStorePath = FileSystems.getDefault().getPath(args[0]);
        final Path dataObjectPath = FileSystems.getDefault().getPath(args[1]);
        final String flowInfoJson = args[2];

        final JobStore jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        final Engine engine = new Engine();
        Job job = engine.insertIntoJobStore(dataObjectPath, flowInfoJson, jobStore);
        engine.chunkify(job, jobStore);
        engine.process(job, jobStore);
    }
}