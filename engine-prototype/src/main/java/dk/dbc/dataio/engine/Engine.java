package dk.dbc.dataio.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    public Job insertIntoJobStore(Path dataObjectPath, String flowInfoJson, JobStore jobStore) throws JobStoreException {
        return jobStore.createJob(dataObjectPath, JsonUtil.fromJson(flowInfoJson, FlowInfo.class));
    }

    public Job chunkify(Job job, JobStore jobStore) throws JobStoreException {
        Path path = job.getOriginalDataPath();
        List<Chunk> chunks = null;
        try {
            chunks = splitByLine(path, job);
            log.info("Number of chunks: {}",  chunks.size());
        } catch (IOException ex) {
            System.err.println("An error occured: " + ex);
        }
        for (Chunk chunk : chunks) {
            jobStore.addChunk(job, chunk);
        }
        return job;
    }

    public Job process(Job job, JobStore jobStore) throws JobStoreException {
        long numberOfChunks = jobStore.getNumberOfChunksInJob(job);
        log.info("Number of chunks for jobId [{}]: {}", job.getId(), numberOfChunks);
        for (int i = 0; i < numberOfChunks; i++) {
            Chunk chunk = jobStore.getChunk(job, i);
            ProcessChunkResult chunkResult = processChunk(chunk);
            jobStore.addChunkResult(job, chunkResult);
        }
        return job;
    }

    public Job sendToSink(Job job, JobStore jobStore) {
        return job;
    }

    /**
     * Read file line by line and split it into Chunks
     *
     * @param path
     * @param job
     * @return
     */
    private List<Chunk> splitByLine(Path path, Job job) throws IOException {
        log.info("Got path: " + path.toString());
        List<Chunk> chunks = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(path, Charset.forName("UTF-8"));
        String line;
        long chunkId = 0;
        int counter = 0; 
        Chunk chunk = new Chunk(chunkId, job.getFlowInfo());
        while((line = br.readLine()) != null) {
            if (counter++ < Chunk.MAX_RECORDS_PER_CHUNK) {
                chunk.addRecord(line);
            } else {
                chunks.add(chunk);
                chunk = new Chunk(++chunkId, job.getFlowInfo());
                chunk.addRecord(line);
                counter = 1;
            }
        }
        if (counter != 0) {
            chunks.add(chunk);
        }
        return chunks;
    }


    private ProcessChunkResult processChunk(Chunk chunk) {
        log.info("Processing chunk: {}", chunk.getId());
        FlowInfo flowInfo = chunk.getFlowInfo();
        List<String> records = chunk.getRecords();

        List<String> results = new ArrayList<>();
        
        for(String record : records) {
            String recordResult = processRecord(flowInfo, record);
            results.add(recordResult);
        }

        return new ProcessChunkResult(chunk.getId(), results);
    }
    
    private String processRecord(FlowInfo flowInfo, String record) {
        log.info("Record: {}", record);
        return javascriptRecordHandler(flowInfo, record);
    }

    private String javascriptRecordHandler(FlowInfo flowInfo, String record) {
        JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(flowInfo.getComponents().get(0).getJavascript());
        Object res = scriptWrapper.callMethod(flowInfo.getComponents().get(0).getInvocationMethod(), new Object[]{record});
        return (String)res;
    }
}