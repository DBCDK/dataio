package dk.dbc.dataio.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    public Job insertIntoJobStore(Path dataObjectPath, String flowInfoJson, JobStore jobStore) throws JobStoreException {
        return jobStore.createJob(dataObjectPath, new FlowInfo(flowInfoJson));
    }

    public Job chunkify(Job job, JobStore jobStore) throws JobStoreException {
        Path path = job.getOriginalDataPath();
        List<Chunk> chunks = null;
        try {
            chunks = splitByLine(path);
        } catch (IOException ex) {
            System.err.println("An error occured: " + ex);
        }
        for (Chunk chunk : chunks) {
            jobStore.addChunk(job, chunk);
        }
        return job;
    }

    public Job process(Job job, JobStore jobStore) {
        return job;
    }

    public Job sendToSink(Job job, JobStore jobStore) {
        return job;
    }

    /**
     * Read file line by line and split it into Chunks
     * @param path
     * @return 
     */
    private List<Chunk> splitByLine(Path path) throws IOException {
        log.info("Got path: " + path.toString());
        List<Chunk> chunks = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"));
        String line;
        int counter = 0; 
        Chunk chunk = new Chunk();
        while((line = br.readLine()) != null) {
            if(counter < Chunk.MAX_RECORDS_PER_CHUNK) {
                chunk.addRecord(line);
            } else {
                chunks.add(chunk);
                chunk = new Chunk();
                counter = 0;
            }
        }
        if(counter != 0) {
            chunks.add(chunk);
        }
        return chunks;
    }
}