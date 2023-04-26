package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.httpclient.HttpClient;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Mocked JobStoreServiceConnector implementation able to intercept
 * calls to addJob() capturing job input streams in local jobInputStreams
 * field and returning values from local jobInfoSnapshots field.
 */
public class MockedJobStoreServiceConnector extends JobStoreServiceConnector {
    public Queue<JobInputStream> jobInputStreams;
    public Queue<JobInfoSnapshot> jobInfoSnapshots;
    public Queue<Chunk> chunks;

    public MockedJobStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseurl", null);
        jobInputStreams = new LinkedList<>();
        jobInfoSnapshots = new LinkedList<>();
        chunks = new LinkedList<>();
    }

    @Override
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) {
        jobInputStreams.add(jobInputStream);
        return jobInfoSnapshots.remove();
    }

    @Override
    public JobInfoSnapshot addChunk(Chunk chunk, int jobId, long chunkId) {
        chunks.add(chunk);
        return jobInfoSnapshots.remove();
    }
}
