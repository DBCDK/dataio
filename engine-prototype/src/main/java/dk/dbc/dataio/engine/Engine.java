package dk.dbc.dataio.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Engine {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    public Job insertIntoJobStore(Path dataObjectPath, String flowInfoJson, JobStore jobStore) throws JobStoreException {
        return jobStore.createJob(dataObjectPath, null);
    }

    public Job chunkify(Job job, JobStore jobStore) {
        return job;
    }

    public Job process(Job job, JobStore jobStore) {
        return job;
    }

    public Job sendToSink(Job job, JobStore jobStore) {
        return job;
    }

}