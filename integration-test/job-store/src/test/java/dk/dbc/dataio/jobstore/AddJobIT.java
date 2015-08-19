package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AddJobIT extends AbstractJobStoreTest {

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 10000;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public TestName test = new TestName();

    /**
     * Given: an empty job-store <br/>
     * When: a job specification is posted, which references a harvester marcXchange data file
     * containing a number of records exceeding the capacity of a single chunk <br/>
     * Then: a new job is created without error <br/>
     * And: the proper number of chunks is created <br/>
     */
    @Test
    public void createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated()
            throws IOException, JobStoreServiceConnectorException, URISyntaxException {
        final int recordCount = 15;
        final String fileId = createMarcxchangeHarvesterDataFile(tmpFolder.newFile(), recordCount);
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                    .setPackaging("xml")
                    .setFormat("katalog")
                    .setCharset("utf8")
                    .setDestination(test.getMethodName())
                    .setSubmitterId(700000)
                    .setDataFile(FileStoreUrn.create(fileId).toString())
                    .build();
        createFlowStoreEnvironmentMatchingJobSpecification(jobSpecification);

        // When...

        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(getJobInputStream(jobSpecification));

        final JobInfoSnapshot jobInfoSnapshotAfterWait = this.waitForJobCompletion(jobInfoSnapshot.getJobId());

        // Then...
        final State jobState = jobInfoSnapshotAfterWait.getState();
        assertThat("Partitioning phase complete", jobState.phaseIsDone(State.Phase.PARTITIONING), is(true));
        assertThat("Partitioning phase failures", jobState.getPhase(State.Phase.PARTITIONING).getFailed(), is(0));

        // And...
        assertThat("Number of items", jobInfoSnapshotAfterWait.getNumberOfItems(), is(recordCount));
        assertThat("Number of chunks", jobInfoSnapshotAfterWait.getNumberOfChunks(), is(2));
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        System.out.println("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - waiting in MAX milliseconds: " + MAX_WAIT_IN_MS);
        System.out.println("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - sleeping in milliseconds: " + SLEEP_INTERVAL_IN_MS);


        while ( remainingWaitInMs > 0 ) {
            System.out.println("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - remaining wait in milliseconds: " + remainingWaitInMs);

            jobInfoSnapshot = jobStoreServiceConnector.listJobs(criteria).get(0);
            if (phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (!phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
            throw new IllegalStateException(String.format("Job %d did not complete successfully in time",
                    jobInfoSnapshot.getJobId()));
        }

        return jobInfoSnapshot;
    }

    private boolean phasePartitioningDoneSuccessfully(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.phaseIsDone(State.Phase.PARTITIONING);
    }
}