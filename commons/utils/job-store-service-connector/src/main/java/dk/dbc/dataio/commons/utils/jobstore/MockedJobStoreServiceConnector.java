package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;

import java.util.LinkedList;
import java.util.Queue;

/**
* Mocked JobStoreServiceConnector implementation able to intercept
* calls to createJob() capturing job specifications in local jobSpecification
* property
*/
public class MockedJobStoreServiceConnector extends  JobStoreServiceConnector {
    public Queue<JobSpecification> jobSpecifications;
    public Queue<JobInfo> jobInfos;

    public MockedJobStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseurl");
        jobSpecifications = new LinkedList<>();
        jobInfos = new LinkedList<>();
    }

    @Override
    public JobInfo createJob(JobSpecification jobSpecification) {
        jobSpecifications.add(jobSpecification);
        return jobInfos.remove();
    }
}
