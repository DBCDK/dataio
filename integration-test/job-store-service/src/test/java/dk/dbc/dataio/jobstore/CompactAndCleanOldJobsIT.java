package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class CompactAndCleanOldJobsIT extends AbstractJobStoreServiceContainerTest {
    Logger LOGGER = LoggerFactory.getLogger(AbstractJobStoreServiceContainerTest.class);

    final String OLD_JOB_ID = "41434";
    final String A_LITTLE_YOUNGER_JOB_ID = "41435";
    final String OLD_JOB_SUPER_TRANSIENT_ID = "41436";
    final String A_LITTLE_YOUNGERSUPER_TRANSIENT__JOB_ID = "41437";



    /**
     * Given: Four existing jobs
     * - One job old persistent. To be cleaned (compacted). One chunk with six items.
     * - One job, two days old, super transient. To be removed. One chunk with six items.
     * - One persistent job, a little younger. One chunk with six items. Left as is.
     * - One super transient job, from today. One chunk with six items. Left as is.
     * <p>
     * When: purgeJobs is executed
     * <p>
     * Then:
     * - The old persistent job no longer has logs. Chunk and items for the job no longer exists.
     * - The younger job has been left unharmed.
     * - The super transient job two days old is deleted. Chunk and items for the job no longer exists.
     * - The super transient job from today is left unharmed.
     */
    @Test
    public void cleanOldJobs() throws JobStoreServiceConnectorException, LogStoreServiceConnectorException {

        // Given ...
        assertChunksItemsAndLogs(OLD_JOB_ID, 1, 6, true);
        assertChunksItemsAndLogs(A_LITTLE_YOUNGER_JOB_ID, 1, 6, true);
        assertChunksItemsAndLogs(OLD_JOB_SUPER_TRANSIENT_ID, 1, 6, true);
        assertChunksItemsAndLogs(A_LITTLE_YOUNGERSUPER_TRANSIENT__JOB_ID, 1, 6, true);

        // When ...
        jobStoreServiceConnector.purge();

        // Then ...
        assertChunksItemsAndLogs(OLD_JOB_ID, 0, 0, false);
        assertChunksItemsAndLogs(A_LITTLE_YOUNGER_JOB_ID, 1, 6, true);

        assertChunksItemsAndLogs(OLD_JOB_SUPER_TRANSIENT_ID, 0, 0, false);
        assertChunksItemsAndLogs(A_LITTLE_YOUNGERSUPER_TRANSIENT__JOB_ID, 1, 6, true);


        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, OLD_JOB_ID));
        JobInfoSnapshot oldJob = jobStoreServiceConnector.listJobs(criteria).get(0);
        assertThat("COMPACTED", oldJob.getSpecification().getType(), is(JobSpecification.Type.COMPACTED));
    }

    private void assertLogs(String jobId, int numberOfChunks, int numberOfItems)
            throws LogStoreServiceConnectorException {

        // This is a test setup. We know for certain that none of our test jobs feature more than
        // one chunk with six items.
        //
        for (int itemNum = 0; itemNum < numberOfItems; itemNum++) {
            assertThat(String.format("Log jobid/chunk/item:%s/0/%d", jobId, itemNum),
                    logStoreServiceConnector.getItemLog(jobId, 0, itemNum), is(notNullValue()));
        }
    }

    private void assertNoLogs(String jobId) {
        for (int itemNum = 0; itemNum < 10; itemNum++) {
            try {
                logStoreServiceConnector.getItemLog(jobId, 0, itemNum);
                fail(String.format("Logs for jobid/chunk/item: %s/0/%d still exists", jobId, itemNum));
            } catch (LogStoreServiceConnectorException ignored) {

            }

        }
    }

    private List<ItemInfoSnapshot> getItemsFor(String jobId) throws JobStoreServiceConnectorException {
        return jobStoreServiceConnector.listItems(
                new ItemListCriteria().where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId)));
    }

    private void assertChunksItemsAndLogs(String jobId, int numberOfExpectedChunks,
                                          int numberOfExpectedItems, boolean expectLogs)
            throws LogStoreServiceConnectorException, JobStoreServiceConnectorException {
        List<ItemInfoSnapshot> oldJobItemInfoSnapshots = getItemsFor(jobId);

        final int jobNumberOfItems = oldJobItemInfoSnapshots.size();
        final int jobNumberOfChunks = oldJobItemInfoSnapshots.stream()
                .map(ItemInfoSnapshot::getChunkId).collect(Collectors.toSet()).size();

        assertThat(String.format("Existing items for job '%s'", jobId), jobNumberOfItems, is(numberOfExpectedItems));
        assertThat(String.format("Existing chunk for job '%s'", jobId), jobNumberOfChunks, is(numberOfExpectedChunks));

        if (expectLogs) {
            assertLogs(jobId, jobNumberOfChunks, jobNumberOfItems);
        } else {
            assertNoLogs(jobId);
        }
    }
}
