package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.logstore.service.connector.ejb.LogStoreServiceConnectorBean;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.SessionContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobPurgeBeanIT extends AbstractJobStoreIT {
    private final LogStoreServiceConnectorBean mockedLogStoreServiceConnectorBean = mock(LogStoreServiceConnectorBean.class);
    private final LogStoreServiceConnector mockedLogStoreServiceConnector = mock(LogStoreServiceConnector.class);
    private final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final SessionContext sessionContext = mock(SessionContext.class);
    private JobPurgeBean jobPurgeBean;

    @Before
    public void initializeJobPurgeBean() {
        jobPurgeBean = newJobPurgeBean();
        when(mockedLogStoreServiceConnectorBean.getConnector()).thenReturn(mockedLogStoreServiceConnector);
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
    }

    private JobPurgeBean newJobPurgeBean() {
        final JobPurgeBean jobPurgeBean = new JobPurgeBean();
        jobPurgeBean.pgJobStoreRepository = new PgJobStoreRepository();
        jobPurgeBean.pgJobStoreRepository.entityManager = entityManager;
        jobPurgeBean.logStoreServiceConnectorBean = mockedLogStoreServiceConnectorBean;
        jobPurgeBean.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        jobPurgeBean.sessionContext = sessionContext;
        jobPurgeBean.entityManager = entityManager;
        when(sessionContext.getBusinessObject(JobPurgeBean.class)).thenReturn(jobPurgeBean);
        return jobPurgeBean;
    }


    @Test
    public void purgeJob_deletesOnlyExpectedJobs_ok() throws FileStoreServiceConnectorException, LogStoreServiceConnectorUnexpectedStatusCodeException, InterruptedException {
        final JobEntity toBeDeletedJob = newJobEntity();

        // is of expected type, has creation time before scheduled time, has end date
        toBeDeletedJob.getSpecification().withType(JobSpecification.Type.TRANSIENT);
        toBeDeletedJob.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        persist(toBeDeletedJob);

        // is of expected type, has creation time before scheduled time, MISSING end date
        final JobEntity transientJob = newJobEntity();
        transientJob.getSpecification().withType(JobSpecification.Type.TRANSIENT);
        persist(transientJob);

        // is NOT of expected type, has creation time before scheduled time, has end date
        final JobEntity testJob = newJobEntity();
        testJob.getSpecification().withType(JobSpecification.Type.PERSISTENT);
        testJob.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        persist(testJob);

        Thread.sleep(1000);

        // Subject under test
        final List<JobInfoSnapshot> jobCandidates = persistenceContext.run(() -> jobPurgeBean.getJobsForDeletion(JobSpecification.Type.TRANSIENT, 1, ChronoUnit.MILLIS));

        // Verification
        assertThat("Number of jobs for deletion found", jobCandidates.size(), is(1));
        assertThat("job id", jobCandidates.get(0).getJobId(), is(toBeDeletedJob.getId()));
    }

    @Test
    public void purgeJob_deleteOldPersistentJobs() throws InterruptedException {
        final JobEntity oldJob = newJobEntity();
        final JobEntity newerJob = newJobEntity();

        oldJob.getSpecification().withType(JobSpecification.Type.PERSISTENT);
        newerJob.getSpecification().withType(JobSpecification.Type.PERSISTENT);

        //Old job: One day too old => Gets cleaned.
        oldJob.setTimeOfCompletion(new Timestamp(Date.from(Instant.now()
                .minus(JobPurgeBean.JOB_EXPIRATION_AGE_IN_DAYS + 1, ChronoUnit.DAYS)).getTime()));
        oldJob.setNumberOfChunks(1);
        oldJob.setNumberOfItems(1);
        persist(oldJob);
        Thread.sleep(1000);

        //Newer job: Two days younger => Will stay.
        newerJob.setTimeOfCompletion(new Timestamp(Date.from(Instant.now()
                .minus(JobPurgeBean.JOB_EXPIRATION_AGE_IN_DAYS - 1, ChronoUnit.DAYS)).getTime()));
        persist(newerJob);
        Thread.sleep(1000);

        // The job has one chunk with one item.
        final ChunkEntity oldChunk = newPersistedChunkEntity(new ChunkEntity.Key(0, oldJob.getId()));
        newPersistedItemEntity(new ItemEntity.Key(oldJob.getId(), oldChunk.getKey().getId(), (short) 0));

        final List<JobInfoSnapshot> jobCandidates = persistenceContext.run(() ->
                jobPurgeBean.getJobsToCompact(JobSpecification.Type.PERSISTENT, JobPurgeBean.JOB_EXPIRATION_AGE_IN_DAYS, ChronoUnit.DAYS));

        assertThat("Number of jobs scheduled for deletion", jobCandidates.size(), is(1));
        assertThat("Job id", jobCandidates.get(0).getJobId(), is(oldJob.getId()));

        // Do a compact of the one found.
        persistenceContext.run(() -> jobPurgeBean.purgeJobs());
        Thread.sleep(1000);

        // Check that by now the old jobs were already compacted.
        final List<JobInfoSnapshot> jobCandidates2 = persistenceContext.run(() ->
                jobPurgeBean.getJobsToCompact(JobSpecification.Type.PERSISTENT, JobPurgeBean.JOB_EXPIRATION_AGE_IN_DAYS, ChronoUnit.DAYS));

        assertThat("Number of jobs scheduled for deletion", jobCandidates2.size(), is(0  ));

        // Check that no Items and no Chunks are left for this job
        final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(0, oldJob.getId()));
        assertThat("No chunks left", chunkEntity, is(nullValue()));
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, new ItemEntity.Key(oldJob.getId(), 0, (short) 0));
        assertThat("No items left", itemEntity, is(nullValue()));
    }

    @Test
    public void purgeJob_dataFileOnlyDeletedWhenNotUsedByMoreJobs_ok() throws FileStoreServiceConnectorException, LogStoreServiceConnectorUnexpectedStatusCodeException {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create("67");
        final JobEntity jobEntityWithSharedDataFileA = newJobEntity();
        jobEntityWithSharedDataFileA.getSpecification().withDataFile(fileStoreUrn.toString());
        persist(jobEntityWithSharedDataFileA);

        final JobEntity jobEntityWithSharedDataFileB = newJobEntity();
        jobEntityWithSharedDataFileB.getSpecification().withDataFile(fileStoreUrn.toString());
        persist(jobEntityWithSharedDataFileB);

        final List<JobInfoSnapshot> jobCandidates = new ArrayList<>();
        jobCandidates.add(JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntityWithSharedDataFileA));
        jobCandidates.add(JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntityWithSharedDataFileB));

        // Subject under test
        persistenceContext.run(() -> jobPurgeBean.delete(jobCandidates.get(0)));

        // Verification
        verify(mockedFileStoreServiceConnector, times(0)).deleteFile(fileStoreUrn);
        verify(mockedLogStoreServiceConnector).deleteJobLogs(String.valueOf(jobCandidates.get(0).getJobId()));
        assertThat("jobEntityWithSharedDataFileA has been deleted", entityManager.find(JobEntity.class, jobCandidates.get(0).getJobId()), is(nullValue()));

        // Subject under test
        persistenceContext.run(() -> jobPurgeBean.delete(jobCandidates.get(1)));

        // Verification
        verify(mockedFileStoreServiceConnector, times(1)).deleteFile(fileStoreUrn);
        verify(mockedLogStoreServiceConnector).deleteJobLogs(String.valueOf(jobCandidates.get(1).getJobId()));
        assertThat("jobEntityWithSharedDataFileB has been deleted",entityManager.find(JobEntity.class, jobCandidates.get(1).getJobId()), is(nullValue()));
    }
}
