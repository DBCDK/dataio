package dk.dbc.dataio.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;

public class EngineMain {
    private static final Logger log = LoggerFactory.getLogger(EngineMain.class);

    private EngineMain() { }

    public static void main(String[] args) throws JobStoreException {
        final JobStore jobStore = FileSystemJobStore.newFileSystemJobStore(FileSystems.getDefault().getPath(args[0]));

        final Engine engine = new Engine();
        Job job = engine.insertIntoJobStore(FileSystems.getDefault().getPath(args[1]), "{}", jobStore);
        engine.chunkify(job, jobStore);
        engine.process(job, jobStore);
    }
}