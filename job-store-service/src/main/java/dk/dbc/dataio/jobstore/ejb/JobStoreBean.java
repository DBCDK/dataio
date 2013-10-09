package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.fsjobstore.FileSystemJobStore;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import java.nio.file.FileSystems;
import java.nio.file.Path;

@Singleton
public class JobStoreBean implements dk.dbc.dataio.jobstore.JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);
    private static final String JOB_STORE_NAME = "dataio-job-store";

    // class scoped for easy test injection
    Path jobStorePath = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), JOB_STORE_NAME);
    JobStore jobStore;

    @PostConstruct
    public void setupJobStore() {
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch (JobStoreException ex) {
            final String errMsg = "An Error occured while setting up the job-store.";
            LOGGER.error(errMsg, ex);
            throw new RuntimeException(errMsg, ex);
        }
    }

    @Override
    public Job createJob(Path dataObjectPath, Flow flow) throws JobStoreException {
        return jobStore.createJob(dataObjectPath, flow);
    }

    @Override
    public long getNumberOfChunksInJob(Job job) throws JobStoreException {
        return jobStore.getNumberOfChunksInJob(job);
    }

    @Override
    public Chunk getChunk(Job job, long chunkId) throws JobStoreException {
        return jobStore.getChunk(job, chunkId);
    }


    /*
    // Todo: Move into JobStore
    private Job chunkify(Job job) throws JobStoreException {
        Path path = job.getOriginalDataPath();
        List<Chunk> chunks = null;
        try {
            chunks = applyDefaultXmlSplitter(path, job);
            LOGGER.info("Number of chunks: {}", chunks.size());
        } catch (XMLStreamException | IOException ex) {
            LOGGER.info("An error occured: ", ex);
        }
        for (Chunk chunk : chunks) {
            jobStore.addChunk(job, chunk);
        }
        return job;
    }

    private List<Chunk> applyDefaultXmlSplitter(Path path, Job job) throws IOException, XMLStreamException {
        LOGGER.info("Got path: " + path.toString());
        final DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(Files.newInputStream(path));
        final List<Chunk> chunks = new ArrayList<>();

        long chunkId = 0;
        int counter = 0;
        Chunk chunk = new Chunk(chunkId, job.getFlow());
        for (String record : recordSplitter) {
            LOGGER.trace("======> Before [" + record + "]");
            final String recordBase64 = base64encode(record);
            LOGGER.trace("======> After  [" + recordBase64 + "]");
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
    */
}
