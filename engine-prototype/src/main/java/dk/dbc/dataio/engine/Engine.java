package dk.dbc.dataio.engine;

import java.nio.file.Path;

public class Engine {

        public Job insertIntoJobStore(Path dataObjectPath, String flowInfoJson, JobStore jobStore) {
            return new Job();
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