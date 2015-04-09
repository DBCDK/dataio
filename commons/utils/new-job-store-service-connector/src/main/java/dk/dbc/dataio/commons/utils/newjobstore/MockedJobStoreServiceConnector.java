package dk.dbc.dataio.commons.utils.newjobstore;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;

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

    public MockedJobStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseurl");
        jobInputStreams = new LinkedList<>();
        jobInfoSnapshots = new LinkedList<>();
    }

    @Override
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) {
        jobInputStreams.add(jobInputStream);
        return jobInfoSnapshots.remove();
    }
}
