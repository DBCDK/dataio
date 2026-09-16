package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.IntStream;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Chunk states
 * 1a. READY_FOR_PROCESSING      ( marks chunk as partitioned and analyzed
 * 1b. SCHEDULED_FOR_PROCESSING  ( marks chunk as scheduled for bulk processing
 * 2.  QUEUED_FOR_PROCESSING     ( marks chunk as sent to processing JMS queue )
 * 3a. READY_FOR_DELIVERY        ( marks chunk as ready for sink delivery )
 * 3b. SCHEDULE_FOR_DELIVERY     ( marks chunk as scheduled for bulk delivery )
 * 4.  QUEUED_FOR_DELIVERY       ( marks chunk as sent to sink JMS queue )
 */
public class JobSchedulerBeanIT extends AbstractJobStoreIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanIT.class);

    @org.junit.Test
    public void testValidTransitions() throws Exception {
        startHazelcastWith(null);
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks2.sql");
        // One chunk per status, so chunkProcessingDone is asked to move each of them to
        // READY_FOR_DELIVERY and only the one valid predecessor may make the move.
        List<ChunkSchedulingStatus> initial = List.of(READY_FOR_PROCESSING, QUEUED_FOR_PROCESSING,
                SCHEDULED_FOR_DELIVERY, READY_FOR_DELIVERY, QUEUED_FOR_DELIVERY,
                SCHEDULED_FOR_PROCESSING, SCHEDULED_FOR_DELIVERY);
        DependencyTrackingRepository repository = newDependencyTrackingRepository();
        persistenceContext.run(() -> IntStream.range(1, 8).forEach(i -> repository.insert(
                new TrackingKey(3, i), 1, 0, initial.get(i - 1), Priority.NORMAL.getValue(), true)));
        JobSchedulerBean bean = new JobSchedulerBean(null, mock(JobSchedulerTransactionsBean.class), null, null, newDependencyTrackingService(), newJobGateBean(), newDeliveryDispatchRepository());

        persistenceContext.run(() -> IntStream.range(1, 8).forEach(chunkId -> {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkId)
                    .appendItem(new ChunkItemBuilder().setData("ProcessedChunk").build())
                    .build()
            );
        }));
        // Only QUEUED_FOR_PROCESSING may become READY_FOR_DELIVERY. Every other chunk stays where
        // it was, which is what the conditional status update is for.
        List<ChunkSchedulingStatus> expected = List.of(READY_FOR_PROCESSING, READY_FOR_DELIVERY, SCHEDULED_FOR_DELIVERY, READY_FOR_DELIVERY, QUEUED_FOR_DELIVERY, SCHEDULED_FOR_PROCESSING, SCHEDULED_FOR_DELIVERY);
        IntStream.range(1, 8).forEach(i -> Assert.assertEquals(expected.get(i - 1),
                repository.get(new TrackingKey(3, i)).orElseThrow().getStatus()));
    }

    @org.junit.Test
    public void scheduleChunk_processingQueueFull_parksTheRemainder() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        int maxCap = 10;
        DependencyTrackingService trackingService = new DependencyTrackingService() {
            @Override
            public int capacity(int sinkId, ChunkSchedulingStatus status) {
                return maxCap - getCount(sinkId, status);
            }
        }.withRepository(newDependencyTrackingRepository()).init();
        int startingCap = trackingService.getCount(1, QUEUED_FOR_PROCESSING);
        PgJobStoreRepository jobStoreRepository = newPgJobStoreRepository();
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean(entityManager, jobStoreRepository, mock(SinkMessageProducerBean.class), mock(JobProcessorMessageProducerBean.class), trackingService);
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, jtbean, jobStoreRepository, null, trackingService, newJobGateBean(), newDeliveryDispatchRepository());

        final JobEntity jobEntity = new JobEntity(3);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setSpecification(new JobSpecification().withSubmitterId(1));
        jobEntity.setState(new State());
        jobEntity.setCachedSink(SinkCacheEntity.create(new SinkBuilder()
                .setId(1)
                .setContent(new SinkContentBuilder()
                        .setSinkType(SinkContent.SinkType.TICKLE)
                        .build())
                .build()));

        entityManager.getTransaction().begin();
        int msgCount = 5;
        IntStream.range(0, msgCount).forEach(chunkId -> {
            ChunkEntity chunkEntity = new ChunkEntity()
                    .withJobId(3)
                    .withChunkId(chunkId)
                    .withNumberOfItems((short) 1);

            bean.scheduleChunk(chunkEntity, jobEntity);
        });
        bean.createAndScheduleTerminationChunk(jobEntity, jobEntity.getCachedSink().getSink(),
                5, ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();
        Assertions.assertEquals(maxCap, trackingService.getCount(1, QUEUED_FOR_PROCESSING));
        Assertions.assertEquals(startingCap + msgCount - maxCap, trackingService.getCount(1, SCHEDULED_FOR_PROCESSING));

        System.out.println("");
    }


    /**
     * A termination chunk enters dependency tracking ready to deliver, and is dispatched.
     * <p>
     * It is created already processed, so nothing in the processing phase ever advances its status.
     * Left in {@code READY_FOR_PROCESSING} the chunk is silently undeliverable, because
     * {@code QUEUED_FOR_DELIVERY} is not a valid change from there, and every job's end-of-job work
     * stops reaching its sink. Caught by {@code EmptyJobsIT} in integration-test/job-store-service
     * and asserted here so it does not need the deployed service to be noticed.
     */
    @org.junit.Test
    public void createAndScheduleTerminationChunk_isDispatchable() throws Exception {
        startHazelcastWith(null);
        DependencyTrackingService trackingService = newDependencyTrackingService();
        PgJobStoreRepository jobStoreRepository = newPgJobStoreRepository();
        SinkMessageProducerBean sinkMessageProducer = mock(SinkMessageProducerBean.class);
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean(entityManager,
                jobStoreRepository, sinkMessageProducer, mock(JobProcessorMessageProducerBean.class),
                trackingService);
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, jtbean, jobStoreRepository, null,
                trackingService, newJobGateBean(), newDeliveryDispatchRepository());

        // A job with no data chunks at all, so its gate opens on the insert and nothing holds the
        // termination chunk back but its own status. The sink cache is persisted rather than built
        // in memory, because job.cachedsink is a foreign key into it.
        JobEntity jobEntity = newJobEntity();
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(1)
                .setContent(new SinkContentBuilder()
                        .setSinkType(SinkContent.SinkType.PERIODIC_JOBS)
                        .build())
                .build()));
        persist(jobEntity);

        persistenceContext.run(() -> {
            try {
                bean.createAndScheduleTerminationChunk(jobEntity, jobEntity.getCachedSink().getSink(),
                        0, ChunkItem.Status.SUCCESS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        TrackingKey terminationChunk = new TrackingKey(jobEntity.getId(), 0);
        assertThat("the termination chunk left READY_FOR_PROCESSING",
                trackingService.get(terminationChunk).getStatus(), is(QUEUED_FOR_DELIVERY));
        verify(sinkMessageProducer).send(anyList(), any(JobEntity.class), anyInt());
    }

    @org.junit.Test
    public void isScheduled() {
        DependencyTrackingService service = newDependencyTrackingService();
        final ChunkEntity notScheduled = new ChunkEntity();
        notScheduled.setKey(new ChunkEntity.Key(42, 42));
        assertThat("not scheduled", service.isScheduled(notScheduled), is(false));

        final JobEntity jobEntity = newPersistedJobEntity();
        final ChunkEntity scheduled = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        scheduleChunk(service, newSchedulingRow(jobEntity.getId(), 0));
        assertThat("scheduled", service.isScheduled(scheduled), is(true));
    }

    /**
     * A chunk that already has a row is left alone, so nothing is dispatched a second time.
     */
    @org.junit.Test
    public void ensureLastChunkIsScheduled_alreadyScheduled() {
        final JobEntity jobEntity = newPersistedJobEntity();
        DependencyTrackingService trackingService = newDependencyTrackingService();
        jobEntity.setNumberOfChunks(43);
        newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));
        scheduleChunk(trackingService, newSchedulingRow(jobEntity.getId(), 42));

        final JobSchedulerTransactionsBean jobSchedulerTransactionsBean = mock(JobSchedulerTransactionsBean.class);
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager, jobSchedulerTransactionsBean, null, null, trackingService, newJobGateBean(), newDeliveryDispatchRepository());

        jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());

        verify(jobSchedulerTransactionsBean, never()).submitToProcessingIfPossibleAsync(any(), anyInt(), anyInt());
    }

    @org.junit.Test
    public void ensureLastChunkIsScheduled_notAlreadyScheduled() {
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();

        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(43);
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setPriority(Priority.HIGH);
        persist(jobEntity);

        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));

        final JobSchedulerTransactionsBean jobSchedulerTransactionsBean = mock(JobSchedulerTransactionsBean.class);
        DependencyTrackingService trackingService = newDependencyTrackingService();
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager, jobSchedulerTransactionsBean, null, null, trackingService, newJobGateBean(), newDeliveryDispatchRepository());
        JobsBeanTest.notAborted(jobEntity.getId(), jb -> {
            persistenceContext.run(() -> jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId()));

            verify(jobSchedulerTransactionsBean).submitToProcessingIfPossibleAsync(
                    chunkEntity, sinkCacheEntity.getSink().getId(), jobEntity.getPriority().getValue());
        });
    }

    @org.junit.Test
    public void scheduleChunk_chunkContainsNoLiveHeadOrSectionRecord_jobPriorityIsUsed() {
        assertScheduleChunkPriority(false, Priority.LOW, Priority.LOW);
    }

    @org.junit.Test
    public void scheduleChunk_chunkContainsLiveHeadOrSectionRecord_priorityIsOverriddenToHigh() {
        assertScheduleChunkPriority(true, Priority.LOW, Priority.HIGH);
    }

    private void assertScheduleChunkPriority(boolean containsLiveHeadOrSectionRecord,
                                             Priority jobPriority, Priority expectedPriority) {
        final int chunkId = 0;

        // Both rows are persisted rather than built in memory, since dependencytracking has a
        // foreign key on (jobid, chunkid) into chunk.
        final JobEntity jobEntity = newJobEntity();
        jobEntity.setPriority(jobPriority);
        jobEntity.setSpecification(new JobSpecification().withSubmitterId(1));
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder().setId(1).build()));
        persist(jobEntity);
        final int jobId = jobEntity.getId();

        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(chunkId, jobId))
                .withContainsLiveHeadOrSectionRecord(containsLiveHeadOrSectionRecord);

        final JobSchedulerTransactionsBean jobSchedulerTransactionsBean = mock(JobSchedulerTransactionsBean.class);
        DependencyTrackingService trackingService = newDependencyTrackingService();
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager, jobSchedulerTransactionsBean, null, null, trackingService, newJobGateBean(), newDeliveryDispatchRepository());

        JobsBeanTest.notAborted(jobId, jb ->
                persistenceContext.run(() -> jobSchedulerBean.scheduleChunk(chunkEntity, jobEntity)));

        // The priority handed to processing...
        verify(jobSchedulerTransactionsBean).submitToProcessingIfPossibleAsync(
                chunkEntity, jobEntity.getCachedSink().getSink().getId(), expectedPriority.getValue());
        // ...and the priority driving dispatch ordering downstream
        assertThat("dependency tracking priority",
                newDependencyTrackingRepository().get(new TrackingKey(jobId, chunkId)).orElseThrow().getPriority(),
                is(expectedPriority.getValue()));
    }

    private DependencyTracking newSchedulingRow(int jobId, int chunkId) {
        return new DependencyTracking(new TrackingKey(jobId, chunkId), 1, 0)
                .setStatus(READY_FOR_PROCESSING)
                .setPriority(Priority.NORMAL.getValue());
    }
}
