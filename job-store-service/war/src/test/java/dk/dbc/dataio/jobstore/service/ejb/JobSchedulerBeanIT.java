package dk.dbc.dataio.jobstore.service.ejb;

import com.hazelcast.map.IMap;
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
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.BLOCKED;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Chunk states
 * 1a. READY_FOR_PROCESSING      ( marks chunk as partitioned and analyzed
 * 1b. SCHEDULED_FOR_PROCESSING  ( marks chunk as scheduled for bulk processing
 * 2.  QUEUED_FOR_PROCESSING     ( marks chunk as sent to processing JMS queue )
 * 3a. READY_FOR_DELIVERY        ( marks chunk as ready for sink delivery )
 * 3b. SCHEDULE_FOR_DELIVERY     ( marks chunk as scheduled for bulk delivery )
 * 3c. BLOCKED                   ( marks chunk as waiting for delivery of one or more other chunks )
 * 4.  QUEUED_FOR_DELIVERY       ( marks chunk as sent to sink JMS queue )
 */
public class JobSchedulerBeanIT extends AbstractJobStoreIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBeanIT.class);

    @org.junit.Test
    public void findChunksWaitingForMe() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService service = new DependencyTrackingService();
        List<TrackingKey> res = service.findChunksWaitingForMe(new TrackingKey(3, 0), 1);
        assertThat(res, containsInAnyOrder(new TrackingKey(2, 1), new TrackingKey(2, 2), new TrackingKey(2, 3), new TrackingKey(2, 4)));
    }

    @org.junit.Test
    public void testValidTransitions() throws Exception {
        startHazelcastWith(null);
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanIT_findWaitForChunks2.sql");
        Function<Integer, DependencyTracking> f = i -> new DependencyTracking(new TrackingKey(3, i), 1, 0, Set.of("K8", "KK2", "C4")).setStatus(ChunkSchedulingStatus.from(i));
        Map<TrackingKey, DependencyTracking> dtTracker = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
        IntStream.range(1, 8).mapToObj(f::apply).forEach(dt -> dtTracker.put(dt.getKey(), dt));
        dtTracker.compute(new TrackingKey(3, 3), (k, dt) -> dt.setWaitingOn(Set.of(new TrackingKey(3, 5))));
        JobSchedulerBean bean = new JobSchedulerBean(null, mock(JobSchedulerTransactionsBean.class), null, null, new DependencyTrackingService().init());

        IntStream.range(1, 8).forEach(chunkId -> {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkId)
                    .appendItem(new ChunkItemBuilder().setData("ProcessedChunk").build())
                    .build()
            );
        });
        List<ChunkSchedulingStatus> expected = List.of(READY_FOR_PROCESSING, READY_FOR_DELIVERY, BLOCKED, READY_FOR_DELIVERY, QUEUED_FOR_DELIVERY, SCHEDULED_FOR_PROCESSING, SCHEDULED_FOR_DELIVERY);
        IntStream.range(1, 8).mapToObj(i -> dtTracker.get(new TrackingKey(3, i)))
                .forEach(dt -> Assert.assertEquals(expected.get(dt.getKey().getChunkId() -  1), dt.getStatus()));
    }

    @org.junit.Test
    public void tickleChunkDependency() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        PgJobStoreRepository jobStoreRepository = newPgJobStoreRepository();
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean(entityManager, jobStoreRepository, mock(SinkMessageProducerBean.class), mock(JobProcessorMessageProducerBean.class), trackingService);
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, jtbean, jobStoreRepository, null, trackingService);

        final JobEntity jobEntity = new JobEntity(3);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setSpecification(new JobSpecification()
                .withSubmitterId(1));
        jobEntity.setState(new State());
        jobEntity.setCachedSink(SinkCacheEntity.create(new SinkBuilder()
                .setId(1)
                .setContent(new SinkContentBuilder()
                        .setSinkType(SinkContent.SinkType.TICKLE)
                        .build())
                .build()));

        entityManager.getTransaction().begin();
        JobsBeanTest.notAborted(jobEntity.getId(), jb -> {
            for (int chunkId : new int[]{0, 1, 2, 3, 4}) {
                ChunkEntity chunkEntity = new ChunkEntity()
                        .withJobId(3)
                        .withChunkId(chunkId)
                        .withNumberOfItems((short) 1)
                        .withSequenceAnalysisData(makeSequenceAnalyseData(
                                String.format("CK%d", chunkId),
                                String.format("CK%d", chunkId - 1)));

                bean.scheduleChunk(chunkEntity, jobEntity);
            }
        });
        bean.createAndScheduleTerminationChunk(jobEntity, jobEntity.getCachedSink().getSink(),
                5, "1", ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();

        assertThat("check match key for chunk0",
                getDependencyTrackingEntity(3, 0).getMatchKeys(),
                containsInAnyOrder("CK-1", "CK0", "1"));
        assertThat("check barrier match key for chunk1",
                getDependencyTrackingEntity(3, 1).getMatchKeys(),
                containsInAnyOrder("CK0", "CK1"));
        assertThat("check barrier match key for chunk2",
                getDependencyTrackingEntity(3, 2).getMatchKeys(),
                containsInAnyOrder("CK1", "CK2"));
        assertThat("check barrier match key for chunk3",
                getDependencyTrackingEntity(3, 3).getMatchKeys(),
                containsInAnyOrder("CK2", "CK3"));
        assertThat("check barrier match key for chunk5",
                getDependencyTrackingEntity(3, 5).getMatchKeys(),
                containsInAnyOrder("1"));

        assertThat("check waitingOn for chunk1", getDependencyTrackingEntity(3, 0).getWaitingOn().size(), is(0));
        assertThat("check waitingOn for chunk2", getDependencyTrackingEntity(3, 1).getWaitingOn(), containsInAnyOrder(
                mk(3, 0)));
        assertThat("check waitingOn for chunk3", getDependencyTrackingEntity(3, 2).getWaitingOn(), containsInAnyOrder(
                mk(3, 1)));
        assertThat("check waitingOn for chunk4", getDependencyTrackingEntity(3, 3).getWaitingOn(), containsInAnyOrder(
                mk(3, 0),
                mk(3, 2)));
        assertThat("check waitingOn for chunk5", getDependencyTrackingEntity(3, 5).getWaitingOn(), containsInAnyOrder(
                mk(3, 4)));
    }

    @org.junit.Test
    public void scheduleOnBlockedTest() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        int maxCap = 10;
        DependencyTrackingService trackingService = new DependencyTrackingService() {
            @Override
            public int capacity(int sinkId, ChunkSchedulingStatus status) {
                return maxCap - getCount(sinkId, status);
            }
        }.init();
        int startingCap = trackingService.getCount(1, QUEUED_FOR_PROCESSING);
        PgJobStoreRepository jobStoreRepository = newPgJobStoreRepository();
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean(entityManager, jobStoreRepository, mock(SinkMessageProducerBean.class), mock(JobProcessorMessageProducerBean.class), trackingService);
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, jtbean, jobStoreRepository, null, trackingService);

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
                    .withNumberOfItems((short) 1)
                    .withSequenceAnalysisData(makeSequenceAnalyseData(String.format("CK%d", chunkId)));

            bean.scheduleChunk(chunkEntity, jobEntity);
        });
        bean.createAndScheduleTerminationChunk(jobEntity, jobEntity.getCachedSink().getSink(),
                5, "1", ChunkItem.Status.SUCCESS);
        entityManager.getTransaction().commit();
        Assertions.assertEquals(maxCap, trackingService.getCount(1, QUEUED_FOR_PROCESSING));
        Assertions.assertEquals(startingCap + msgCount - maxCap, trackingService.getCount(1, SCHEDULED_FOR_PROCESSING));

        System.out.println("");
    }


    @org.junit.Test
    public void isScheduled() {
        DependencyTrackingService service = new DependencyTrackingService().init();
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.dependencyTrackingService = service;
        final ChunkEntity notScheduled = new ChunkEntity();
        notScheduled.setKey(new ChunkEntity.Key(42, 42));
        assertThat("not scheduled", service.isScheduled(notScheduled), is(false));

        final ChunkEntity scheduled = new ChunkEntity();
        notScheduled.setKey(new ChunkEntity.Key(1, 1));
        assertThat("scheduled", service.isScheduled(scheduled), is(false));
    }

    @org.junit.Test
    public void ensureLastChunkIsScheduled_alreadyScheduled() {
        final JobEntity jobEntity = newPersistedJobEntity();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        jobEntity.setNumberOfChunks(43);
        newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));
        trackingService.add(newDependencyTrackingEntity(new TrackingKey(jobEntity.getId(), 42)));

        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager, null, null, null, trackingService);

        // No key violation, so the isScheduled call must have returned true...
        jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());
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
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager, jobSchedulerTransactionsBean, null, null, trackingService);
        jobSchedulerTransactionsBean.dependencyTrackingService = trackingService;
        JobsBeanTest.notAborted(jobEntity.getId(), jb -> {
            jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());

            verify(jobSchedulerTransactionsBean).submitToProcessingIfPossibleAsync(
                    chunkEntity, sinkCacheEntity.getSink().getId(), jobEntity.getPriority().getValue());
        });
    }

    private TrackingKey mk(int jobId, int chunkId) {
        return new TrackingKey(jobId, chunkId);
    }

    private SequenceAnalysisData makeSequenceAnalyseData(String... s) {
        return new SequenceAnalysisData(makeSet(s));
    }

    private Set<String> makeSet(String... s) {
        Set<String> res = new HashSet<>();
        Collections.addAll(res, s);
        return res;
    }

    @SuppressWarnings("SameParameterValue")
    private DependencyTracking getDependencyTrackingEntity(int jobId, int chunkId) {
        IMap<TrackingKey, DependencyTracking> map = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
        return map.get(new TrackingKey(jobId, chunkId));
    }

}
