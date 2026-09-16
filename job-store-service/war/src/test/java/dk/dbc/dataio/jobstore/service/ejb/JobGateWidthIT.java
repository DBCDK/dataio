package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Barrier width per sink type: closing a data chunk's gate as it is scheduled, the widened
 * re-trigger, and what a closed data-chunk gate withholds from dispatch.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Barrier Width - Per-Sink-Type Job Isolation".
 * <p>
 * The assertions read the gate columns straight from PostgreSQL on their own connections, so they
 * see committed state and nothing else.
 * <p>
 * <b>What these tests cannot cover.</b> The beans are built by hand here rather than by a container,
 * so {@code REQUIRES_NEW} on {@link JobGateBean#insertDataChunkRow} is not honoured and
 * every site shares one entity manager and one connection. The verdicts and the rows written are
 * therefore exercised, but the transaction boundary that keeps the gate write from deadlocking
 * against the termination insert is not, and cannot be at this level: a single connection takes the
 * advisory lock reentrantly and passes whatever the annotation says.
 */
public class JobGateWidthIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4711;
    private static final long SUBMITTER = 820010;
    private static final long OTHER_SUBMITTER = 820011;

    /**
     * The full-width case. Job B's data chunks are inserted closed while job A still owes its
     * job-end, which is what keeps a later tickle job's first delivered item from opening a second
     * batch on a dataset job A is still sweeping.
     */
    @org.junit.Test
    public void dataChunkGateClosedBehindAnEarlierBarrier() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);

        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);

        assertThat("job B's data chunk is held by job A's barrier",
                gateOpen(new TrackingKey(jobB.getId(), 0)), is(false));
    }

    /**
     * One statement writes the row and its gate verdict together, so there is no instant in which
     * the chunk carries the column default TRUE and dispatches. Every column the barrier query and
     * the dispatch query read is supplied by that same statement.
     */
    @org.junit.Test
    public void closedDataChunkGateIsWrittenWithTheRow() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);

        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);

        assertThat("gate column", gateOpen(new TrackingKey(jobB.getId(), 0)), is(false));
        assertThat("written as a data chunk", isTermination(new TrackingKey(jobB.getId(), 0)), is(false));
        assertThat("submitter, which the barrier query reads",
                trackingColumn(new TrackingKey(jobB.getId(), 0), "submitter"), is((int) SUBMITTER));
        assertThat("sinkid, which is NOT NULL with no default",
                trackingColumn(new TrackingKey(jobB.getId(), 0), "sinkid"), is(SINK_ID));
    }

    /**
     * The narrow case. Marcconv finalizes by job id, so its job-end work is not
     * threatened by a later job's data landing first and its data chunks are dispatchable at once.
     * No barrier is even read here, and the row is inserted with its gate open.
     */
    @org.junit.Test
    public void dataChunkGateUntouchedForANarrowSinkType() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.MARCCONV);
        markJobAsPartitioned(jobA);

        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.MARCCONV);
        scheduleChunk(jobB, 0);

        assertThat("dispatchable at once", gateOpen(new TrackingKey(jobB.getId(), 0)), is(true));
    }

    @org.junit.Test
    public void dataChunkGateOpenWithNoEarlierBarrier() throws Exception {
        JobEntity job = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(job, 0);

        assertThat("nothing ahead of it, so the gate is open",
                gateOpen(new TrackingKey(job.getId(), 0)), is(true));
    }

    @org.junit.Test
    public void dataChunkGateOpenForAnotherSubmitter() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);

        JobEntity jobB = newPersistedJob(OTHER_SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);

        assertThat("a different submitter is outside the barrier scope",
                gateOpen(new TrackingKey(jobB.getId(), 0)), is(true));
    }

    /**
     * The three-job case the NOT EXISTS exists for. Lifting job A's barrier
     * releases job B's data chunks and leaves job C's behind job B's own, still unlifted.
     */
    @org.junit.Test
    public void reTriggerOpensDataChunksOfTheNextJobOnly() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);
        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);
        markJobAsPartitioned(jobB);
        JobEntity jobC = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobC, 0);
        markJobAsPartitioned(jobC);

        assertThat("B closed", gateOpen(new TrackingKey(jobB.getId(), 0)), is(false));
        assertThat("C closed", gateOpen(new TrackingKey(jobC.getId(), 0)), is(false));

        liftBarrierImposedBy(jobA);

        assertThat("B's data chunk opened by the re-trigger",
                gateOpen(new TrackingKey(jobB.getId(), 0)), is(true));
        assertThat("C's data chunk still behind B's barrier",
                gateOpen(new TrackingKey(jobC.getId(), 0)), is(false));

        liftBarrierImposedBy(jobB);

        assertThat("C's data chunk opened in turn",
                gateOpen(new TrackingKey(jobC.getId(), 0)), is(true));
    }

    /**
     * The bulk dispatch query filters on the gate, so a closed data
     * chunk is not a delivery candidate however ready it otherwise is, and becomes one the moment
     * the barrier ahead of it lifts.
     */
    @org.junit.Test
    public void closedDataChunkIsNotADeliveryCandidateUntilTheBarrierLifts() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);
        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);
        TrackingKey dataChunk = new TrackingKey(jobB.getId(), 0);
        setStatus(dataChunk, ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY);

        assertThat("held back by the gate",
                newDeliveryDispatchRepository().findDeliveryCandidates(SINK_ID, 10), is(List.of()));
        assertThat("and the direct path agrees, reading the gate off the chunk's own row",
                newDependencyTrackingService().get(dataChunk).isGateOpen(), is(false));

        liftBarrierImposedBy(jobA);

        assertThat("dispatchable once the barrier lifts",
                newDeliveryDispatchRepository().findDeliveryCandidates(SINK_ID, 10), is(List.of(dataChunk)));
    }

    /**
     * The gate holds delivery and nothing else, so a chunk behind one is still processed and simply
     * accumulates in SCHEDULED_FOR_DELIVERY, which has no capacity cap. That is why the delivery
     * candidate query pins the status rather than taking it as a parameter, and why the processing
     * candidate query does not mention the gate at all.
     */
    @org.junit.Test
    public void closedGateDoesNotHoldBackProcessing() throws Exception {
        JobEntity jobA = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        markJobAsPartitioned(jobA);
        JobEntity jobB = newPersistedJob(SUBMITTER, 1, SinkContent.SinkType.TICKLE);
        scheduleChunk(jobB, 0);
        TrackingKey dataChunk = new TrackingKey(jobB.getId(), 0);

        assertThat("closed", gateOpen(dataChunk), is(false));
        setStatus(dataChunk, ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING);

        assertThat("a closed gate is still a processing candidate",
                newDependencyTrackingRepository().findProcessingCandidates(SINK_ID, 10).stream()
                        .map(DependencyTrackingRepository.ProcessingCandidate::key).toList(),
                is(List.of(dataChunk)));

        setStatus(dataChunk, ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY);

        assertThat("parked in SCHEDULED_FOR_DELIVERY rather than held out of processing",
                trackingColumn(dataChunk, "status"),
                is(ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY.value));
        assertThat("and still closed once it gets there", gateOpen(dataChunk), is(false));
    }

    private void scheduleChunk(JobEntity job, int chunkId) {
        ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        persistenceContext.run(() -> newSchedulingBean().scheduleChunk(chunkEntity, job));
    }

    private void liftBarrierImposedBy(JobEntity job) {
        persistenceContext.run(() -> newJobGateBean().liftBarrierAndRetrigger(
                job.getId(), SINK_ID, (int) job.getSpecification().getSubmitterId()));
    }

    private void markJobAsPartitioned(JobEntity job) throws JobStoreException {
        newSchedulingBean().markJobAsPartitioned(job);
    }

    private JobSchedulerBean newSchedulingBean() {
        return new JobSchedulerBean(entityManager, mock(JobSchedulerTransactionsBean.class),
                newPgJobStoreRepository(entityManager), null, newDependencyTrackingService(),
                newJobGateBean(entityManager), newDeliveryDispatchRepository(entityManager));
    }

    private void setStatus(TrackingKey key, ChunkSchedulingStatus status) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "update dependencytracking set status = ? where jobid = ? and chunkid = ?")) {
            statement.setInt(1, status.value);
            statement.setInt(2, key.getJobId());
            statement.setInt(3, key.getChunkId());
            statement.executeUpdate();
            // newConnection() hands out a connection with autoCommit off, as the base class's own
            // cleanup does, so a write that is not committed here is invisible to everything after.
            connection.commit();
        }
    }

    private JobEntity newPersistedJob(long submitterId, int numberOfChunks, SinkContent.SinkType sinkType) {
        JobEntity jobEntity = newJobEntity(submitterId);
        jobEntity.setNumberOfChunks(numberOfChunks);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(SINK_ID)
                .setContent(new SinkContentBuilder().setSinkType(sinkType).build())
                .build()));
        persist(jobEntity);
        return jobEntity;
    }

    private Boolean isTermination(TrackingKey key) throws SQLException {
        return (Boolean) trackingColumn(key, "is_termination");
    }

    private Boolean gateOpen(TrackingKey key) throws SQLException {
        return (Boolean) trackingColumn(key, "gate_open");
    }

    /**
     * @return the column value, or null if the chunk has no dependencytracking row at all, which for
     * a gate column means an open gate
     */
    private Object trackingColumn(TrackingKey key, String column) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + column + " from dependencytracking where jobid = ? and chunkid = ?")) {
            statement.setInt(1, key.getJobId());
            statement.setInt(2, key.getChunkId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1) : null;
            }
        }
    }
}
