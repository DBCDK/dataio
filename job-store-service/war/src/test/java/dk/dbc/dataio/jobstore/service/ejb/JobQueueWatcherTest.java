package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by ThomasBerg on 18/09/15.
 */
public class JobQueueWatcherTest {

    private final FileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private final FileStoreServiceConnector mockedFileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobQueueRepository mockedJobQueueRepository = mock(JobQueueRepository.class);
    private final PgJobStoreRepository mockedJobStoreRepository = mock(PgJobStoreRepository.class);
    private final PgJobStore mockedJobStore = mock(PgJobStore.class);

    @Test
    public void doWatch_startNoJobs() throws JobStoreException {

        /*
            Expectations:
            No expectations necessary because the mock of mockedJobQueueRepository.getUniqueSinkIds() in default returns an empty List.

            which triggers no jobs to be started!
        */


        // Subject Under Test
        getJobQueueWatcherWithMockedInstances().doWatch();

        // Verifications - their is only 1 interaction with JobStore Bean (method: handlePartitioningAsynchronously)
        verifyZeroInteractions(mockedJobStore);
    }

    @Test
    public void doWatch_startOneJob() throws JobStoreException, URISyntaxException {

        final JobQueueWatcher jobQueueWatcher = getJobQueueWatcherWithMockedInstances();
        /*
            Expectations:
            1. Make the mock of mockedJobQueueRepository.getUniqueSinkIds() return an ArrayList with 1 sink ID.
            2. Make the mock of mockedJobQueueRepository.getJobQueueEntitiesBySink(sinkId) return an ArrayList with 2 jobs where
               both are WAITING -> hence Sink is Available.

            Which triggers one job to be started!

        */
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);

        final Long sinkId = 1l;
        when(mockedJobQueueRepository.getUniqueSinkIds()).thenReturn(buildListOfSinkIds(sinkId));

        final JobEntity jobEntity1 = buildJobEntity("42");
        final JobQueueEntity firstWaitingJob = new JobQueueEntityBuilder().setJob(jobEntity1).build();

        when(mockedJobQueueRepository.getFirstWaitingJobQueueEntityBySink(sinkId)).thenReturn(firstWaitingJob);
        when(mockedJobQueueRepository.getJobEntityById(anyInt())).thenReturn(jobEntity1);
        when(mockedJobQueueRepository.getJobQueueEntityByJob(any(JobEntity.class))).thenReturn(new JobQueueEntity());

        // Subject Under Test
        jobQueueWatcher.doWatch();

        // Verifications
        verify(mockedJobStore, times(1)).handlePartitioningAsynchronously(any(PartitioningParam.class));
    }

    @Test
    public void doWatch_startTwoJobsOnTwoDifferentSinks() throws JobStoreException, URISyntaxException {

        final JobQueueWatcher jobQueueWatcher = getJobQueueWatcherWithMockedInstances();

        /*
            Expectations:
            1. Make the mock of mockedJobQueueRepository.getUniqueSinkIds() return an ArrayList with 2 sink ID's.
            2. Make the mock of mockedJobQueueRepository.getJobQueueEntitiesBySink(sinkId) return an ArrayList with 2 jobs where
               both are WAITING -> hence Sink is Available.

            Which triggers Two jobs to be started!

        */
        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);

        final Long sinkId1 = 1l;
        final Long sinkId2 = 2l;

        when(mockedJobQueueRepository.getUniqueSinkIds()).thenReturn(buildListOfSinkIds(sinkId1, sinkId2));

        final JobEntity jobEntity1 = buildJobEntity("42");
        final JobEntity jobEntity2 = buildJobEntity("43");
        final JobQueueEntity firstWaitingJobForFirstSink = new JobQueueEntityBuilder().setJob(jobEntity1).build();
        final JobQueueEntity firstWaitingJobForSecondSink = new JobQueueEntityBuilder().setJob(jobEntity2).build();

        when(mockedJobQueueRepository.getFirstWaitingJobQueueEntityBySink(sinkId1)).thenReturn(firstWaitingJobForFirstSink);
        when(mockedJobQueueRepository.getFirstWaitingJobQueueEntityBySink(sinkId2)).thenReturn(firstWaitingJobForSecondSink);

        when(mockedJobQueueRepository.getJobEntityById(anyInt())).thenReturn(jobEntity1);
        when(mockedJobQueueRepository.getJobQueueEntityByJob(any(JobEntity.class))).thenReturn(new JobQueueEntity());

        // Subject Under Test
        jobQueueWatcher.doWatch();

        // Verifications
        verify(mockedJobStore, times(2)).handlePartitioningAsynchronously(any(PartitioningParam.class));
    }

    @Test
    public void doWatch_startNoJob_becauseSinkIsOccupied() throws JobStoreException, URISyntaxException {

        final JobQueueWatcher jobQueueWatcher = getJobQueueWatcherWithMockedInstances();
        /*
            Expectations:
            1. Make the mock of mockedJobQueueRepository.getUniqueSinkIds() return an ArrayList with 1 sink ID.
            2. Make the mock of mockedJobQueueRepository.getJobQueueEntitiesBySink(sinkId) return an ArrayList with 2 jobs where one of them
               is IN_PROGRESS -> hence Sink is Occupied.

            which triggers no jobs to be started!
        */
        final Long sinkId = 1l;
        final List<Long> listOfSinkIdsWithOneSink = new ArrayList<>(Arrays.asList(sinkId));
        when(mockedJobQueueRepository.getUniqueSinkIds()).thenReturn(listOfSinkIdsWithOneSink);

        final JobEntity jobEntity1 = buildJobEntity("42");
        final JobQueueEntity firstWaitingJobForFirstSink = new JobQueueEntityBuilder().setJob(jobEntity1).build();
        when(mockedJobQueueRepository.getFirstWaitingJobQueueEntityBySink(sinkId)).thenReturn(firstWaitingJobForFirstSink);
        when(mockedJobQueueRepository.isSinkOccupied(sinkId)).thenReturn(true);

        // Subject Under Test
        jobQueueWatcher.doWatch();

        // Verifications - their is NO interactions with JobStore Bean (method: handlePartitioningAsynchronously)
        verifyZeroInteractions(mockedJobStore);
    }


    /*
        This simulates the call to jobQueueRepository.getUniqueSinkIds() in jobQueueWatcher.buildJobQueueEntitiesGroupedBySink();
     */
    private List<Long> getEmptyListOfSinkIds() {

        return new ArrayList<Long>();
    }

    private List<Long> buildListOfSinkIds(Long... sinkIds) {
        return new ArrayList<>(Arrays.asList(sinkIds));
    }

    private JobEntity buildJobEntity(String fileId) throws URISyntaxException {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(
                new JobSpecificationBuilder().setDataFile(
                        FileStoreUrn.create(fileId).toString()).build()
        );
        return jobEntity;
    }

    private JobQueueWatcher getJobQueueWatcherWithMockedInstances() {

        final JobQueueWatcher jobQueueWatcher = new JobQueueWatcher();

        jobQueueWatcher.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        jobQueueWatcher.jobQueueRepository = mockedJobQueueRepository;
        jobQueueWatcher.jobStoreRepository = mockedJobStoreRepository;
        jobQueueWatcher.jobStore = mockedJobStore;

        return jobQueueWatcher;
    }
}
