package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.engine.JobStore;
import dk.dbc.dataio.engine.FileSystemJobStore;
import dk.dbc.dataio.engine.Job;
import dk.dbc.dataio.engine.JobStoreException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.engine.Chunk;
import dk.dbc.dataio.engine.DefaultXMLRecordSplitter;
import static dk.dbc.dataio.engine.Engine.base64encode;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;

@Singleton
public class JobStoreBean {

    private Logger log = LoggerFactory.getLogger(JobStoreBean.class);
    private static final String jobStoreName = "dataio-job-store";
    private Path jobStorePath = FileSystems.getDefault().getPath(String.format("/tmp/%s", jobStoreName));
    private JobStore jobStore;

    @PostConstruct
    public void setupJobStore() {
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch (JobStoreException ex) {
            String errMsg = "An Error occured while setting up the job-store.";
            log.error(errMsg, ex);
            throw new RuntimeException(errMsg, ex);
        }
    }

    public Job createJob(Path jobPath, Flow flow) throws JobStoreException {
        Job job = jobStore.createJob(jobPath, flow);
        return chunkify(job);
    }

    public long getNumberOfChunksInJob(Job job) throws JobStoreException {
        return jobStore.getNumberOfChunksInJob(job);
    }

    public Chunk getChunk(Job job, long chunkId) throws JobStoreException {
        return jobStore.getChunk(job, chunkId);
    }

    private Job chunkify(Job job) throws JobStoreException {
        Path path = job.getOriginalDataPath();
        List<Chunk> chunks = null;
        try {
            //chunks = splitByLine(path, job);
            chunks = applyDefaultXmlSplitter(path, job);
            log.info("Number of chunks: {}", chunks.size());
        } catch (XMLStreamException | IOException ex) {
            log.info("An error occured: ", ex);
        }
        for (Chunk chunk : chunks) {
            jobStore.addChunk(job, chunk);
        }
        return job;
    }

    private List<Chunk> applyDefaultXmlSplitter(Path path, Job job) throws IOException, XMLStreamException {
        log.info("Got path: " + path.toString());
        final DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(Files.newInputStream(path));
        final List<Chunk> chunks = new ArrayList<>();

        long chunkId = 0;
        int counter = 0;
        Chunk chunk = new Chunk(chunkId, job.getFlow());
        for (String record : recordSplitter) {
            log.trace("======> Before [" + record + "]");
            final String recordBase64 = base64encode(record);
            log.trace("======> After  [" + recordBase64 + "]");
            if (counter++ < Chunk.MAX_RECORDS_PER_CHUNK) {
                chunk.addRecord(recordBase64);
            } else {
                chunks.add(chunk);
                chunk = new Chunk(++chunkId, job.getFlow());
                chunk.addRecord(recordBase64);
                counter = 1;
            }
        }
        if (counter != 0) {
            chunks.add(chunk);
        }
        return chunks;
    }
}
