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

import org.apache.commons.codec.binary.Base64;

public class Engine {

    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    public Job insertIntoJobStore(Path dataObjectPath, String flowInfoJson, JobStore jobStore) throws JobStoreException {
        return jobStore.createJob(dataObjectPath, JsonUtil.fromJson(flowInfoJson, Flow.class, JsonUtil.getMixIns()));
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
        Chunk chunk = new Chunk(chunkId, job.getFlow());
        while((line = br.readLine()) != null) {
            log.info("======> Before [" + line + "]");
            String base64line = base64encode(line);
            log.info("======> After  [" + base64line + "]");
            if (counter++ < Chunk.MAX_RECORDS_PER_CHUNK) {
                chunk.addRecord(base64line);
            } else {
                chunks.add(chunk);
                chunk = new Chunk(++chunkId, job.getFlow());
                chunk.addRecord(base64line);
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
        Flow flow = chunk.getFlow();
        List<String> records = chunk.getRecords();

        List<String> results = new ArrayList<>();
        
        for(String record : records) {
            String recordResult = processRecord(flow, base64decode(record));
            String recordResultBase64 = base64encode(recordResult);
            results.add(recordResultBase64);
        }

        return new ProcessChunkResult(chunk.getId(), results);
    }
    
    private String processRecord(Flow flow, String record) {
        log.info("Record: {}", record);
        return javascriptRecordHandler(flow, record);
    }

    private String javascriptRecordHandler(Flow flow, String record) {
        List<JavaScript> javascriptsBase64 = flow.getContent().getComponents().get(0).getContent().getJavascripts();
        List<StringSourceSchemeHandler.Script> javascripts = new ArrayList<>();
        for(JavaScript javascriptBase64 : javascriptsBase64) {
            javascripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(), base64decode(javascriptBase64.getJavascript())));
        }
        JSWrapperSingleScript scriptWrapper = new JSWrapperSingleScript(javascripts);
        Object res = scriptWrapper.callMethod(flow.getContent().getComponents().get(0).getContent().getInvocationMethod(), new Object[]{record});
        return (String)res;
    }
    
    
    public static String base64encode(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes());
    }

    public static String base64decode(String dataToDecode) {
        return new String(Base64.decodeBase64(dataToDecode));
    }

}