package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.util.LinkedList;
import java.util.Queue;

 /**
 * Mocked JobStoreServiceConnectorBean implementation able to intercept
 * calls to createJob() capturing job specifications in local jobSpecification
 * property
 */
public class MockedJobStoreServiceConnectorBean extends  JobStoreServiceConnectorBean {
    public Queue<JobSpecification> jobSpecifications;
    public Queue<JobInfo> jobInfos;

    public MockedJobStoreServiceConnectorBean() {
        jobSpecifications = new LinkedList<>();
        jobInfos = new LinkedList<>();
    }

    @Override
    public JobInfo createJob(JobSpecification jobSpecification) {
        jobSpecifications.add(jobSpecification);
        return jobInfos.remove();
    }
}
