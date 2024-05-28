package dk.dbc.dataio.jobstore.service.ejb;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
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
import dk.dbc.dataio.jobstore.distributed.hz.serializer.RemoveWaitingOnSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.StatusChangeSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.TrackingKeySer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateCounterSer;
import dk.dbc.dataio.jobstore.distributed.hz.serializer.UpdateStatusSer;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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

    @After
    public void stopHz() {
        Hazelcast.shutdownNode();
    }

    @Test
    public void findChunksWaitingForMe() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService service = new DependencyTrackingService();
        List<TrackingKey> res = service.findChunksWaitingForMe(new TrackingKey(3, 0), 1);
        assertThat(res, containsInAnyOrder(new TrackingKey(2, 0), new TrackingKey(2, 1), new TrackingKey(2, 2), new TrackingKey(2, 3), new TrackingKey(2, 4)));
    }

    @Test
    public void multipleCallesToChunkXxxxxxDoneIsIgnored() throws Exception {
        startHazelcastWith(null);
        JPATestUtils.runSqlFromResource(entityManager, this, "JobSchedulerBeanArquillianIT_findWaitForChunks.sql");
        Function<Integer, DependencyTracking> f = i -> new DependencyTracking(new TrackingKey(3, i), 1).setStatus(ChunkSchedulingStatus.from(i)).setMatchKeys(Set.of("K8", "KK2", "C4"));
        Map<TrackingKey, DependencyTracking> dtTracker = Hazelcast.Objects.DEPENDENCY_TRACKING.get();
        IntStream.range(1, 6).mapToObj(f::apply).forEach(dt -> dtTracker.put(dt.getKey(), dt));
        JobSchedulerBean bean = new JobSchedulerBean();
        bean.dependencyTrackingService = new DependencyTrackingService().init();
        bean.jobSchedulerTransactionsBean = mock(JobSchedulerTransactionsBean.class);

        IntStream.range(1, 6).forEach(chunkId -> {
            bean.chunkProcessingDone(new ChunkBuilder(PROCESSED)
                    .setJobId(3).setChunkId(chunkId)
                    .appendItem(new ChunkItemBuilder().setData("ProcessedChunk").build())
                    .build()
            );
        });
        IntStream.range(1, 6).filter(i -> i != 2).mapToObj(i -> dtTracker.get(new TrackingKey(3, i)))
                .forEach(dt -> Assert.assertEquals(dt.getKey().getChunkId(), dt.getStatus().value.intValue()));
    }

    @Test
    public void tickleChunkDependency() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;
        bean.dependencyTrackingService = trackingService;
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean();
        jtbean.dependencyTrackingService = trackingService;
        bean.pgJobStoreRepository = newPgJobStoreRepository();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        jtbean.jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);
        jtbean.jobStoreRepository = bean.pgJobStoreRepository;
        bean.jobSchedulerTransactionsBean = jtbean;

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
        for (int chunkId : new int[]{0, 1, 2, 3, 4}) {
            final ChunkEntity chunkEntity = new ChunkEntity()
                    .withJobId(3)
                    .withChunkId(chunkId)
                    .withNumberOfItems((short) 1)
                    .withSequenceAnalysisData(makeSequenceAnalyceData(
                            String.format("CK%d", chunkId),
                            String.format("CK%d", chunkId - 1)));

            bean.scheduleChunk(chunkEntity, jobEntity);
        }
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

    @Test
    public void scheduleOnBlockedTest() throws Exception {
        startHazelcastWith("JobSchedulerBeanIT_findWaitForChunks.sql");
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        JobSchedulerBean bean = new JobSchedulerBean();
        bean.entityManager = entityManager;
        bean.dependencyTrackingService = trackingService;
        JobSchedulerTransactionsBean jtbean = new JobSchedulerTransactionsBean();
        jtbean.dependencyTrackingService = trackingService;
        bean.pgJobStoreRepository = newPgJobStoreRepository();
        jtbean.entityManager = bean.entityManager;
        jtbean.sinkMessageProducerBean = mock(SinkMessageProducerBean.class);
        jtbean.jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);
        jtbean.jobStoreRepository = bean.pgJobStoreRepository;
        bean.jobSchedulerTransactionsBean = jtbean;

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
        for (int chunkId : new int[]{0, 1, 2, 3, 4}) {
            final ChunkEntity chunkEntity = new ChunkEntity()
                    .withJobId(3)
                    .withChunkId(chunkId)
                    .withNumberOfItems((short) 1)
                    .withSequenceAnalysisData(makeSequenceAnalyceData(
                            String.format("CK%d", chunkId),
                            String.format("CK%d", chunkId - 1)));

            bean.scheduleChunk(chunkEntity, jobEntity);
        }
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


    @Test
    public void isScheduled() {
        Hazelcast.testInstance(createHazelcastInstance());
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

    @Test
    public void ensureLastChunkIsScheduled_alreadyScheduled() {
        final JobEntity jobEntity = newPersistedJobEntity();
        Hazelcast.testInstance(createHazelcastInstance());
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        jobEntity.setNumberOfChunks(43);
        newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));
        trackingService.add(newDependencyTrackingEntity(new TrackingKey(jobEntity.getId(), 42)));

        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.dependencyTrackingService = trackingService;
        jobSchedulerBean.entityManager = entityManager;

        // No key violation, so the isScheduled call must have returned true...
        jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());
    }

    @Test
    public void ensureLastChunkIsScheduled_notAlreadyScheduled() {
        Hazelcast.testInstance(createHazelcastInstance());
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();

        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(43);
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setPriority(Priority.HIGH);
        persist(jobEntity);

        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(42, jobEntity.getId()));

        final JobSchedulerTransactionsBean jobSchedulerTransactionsBean = mock(JobSchedulerTransactionsBean.class);
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        final JobSchedulerBean jobSchedulerBean = new JobSchedulerBean();
        jobSchedulerBean.entityManager = entityManager;
        jobSchedulerBean.dependencyTrackingService = trackingService;
        jobSchedulerBean.jobSchedulerTransactionsBean = jobSchedulerTransactionsBean;
        jobSchedulerTransactionsBean.dependencyTrackingService = trackingService;
        try(MockedStatic<JobsBean> jobsBeanMock = mockStatic(JobsBean.class)) {
            jobsBeanMock.when(() -> JobsBean.isAborted(jobEntity.getId())).thenReturn(false);
            jobSchedulerBean.ensureLastChunkIsScheduled(jobEntity.getId());

            verify(jobSchedulerTransactionsBean).persistDependencyEntity(
                    any(DependencyTracking.class), nullable(String.class));

            verify(jobSchedulerTransactionsBean).submitToProcessingIfPossibleAsync(
                    chunkEntity, sinkCacheEntity.getSink().getId(), jobEntity.getPriority().getValue());
        }
    }

    private TrackingKey mk(int jobId, int chunkId) {
        return new TrackingKey(jobId, chunkId);
    }

    private SequenceAnalysisData makeSequenceAnalyceData(String... s) {
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

    @Override
    protected HazelcastInstance createHazelcastInstance() {
        return createHazelcastInstance(makeConfig());
    }

    private Config makeConfig() {
        Config config = smallInstanceConfig();
        List<CompactSerializer<?>> compactSerializers = List.of(new RemoveWaitingOnSer(), new StatusChangeSer(), new TrackingKeySer(), new UpdateCounterSer(), new UpdateStatusSer());
        compactSerializers.forEach(ser -> config.getSerializationConfig().getCompactSerializationConfig().addSerializer(ser));
        return config;
    }
}
