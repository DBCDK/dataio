package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.ejb.DependencyTrackingRepository;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * The recovery sweep that re-drives a chunk whose dispatch attempt was fired and never arrived.
 * <p>
 * Both {@code READY_*} statuses are held for the length of one attempt, and the bulk submitters read
 * only the {@code SCHEDULED_*} statuses, so a chunk left in either is invisible to everything except
 * this sweep.
 */
public class AdminBeanIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 5021;
    private static final long SUBMITTER = 820030;

    /**
     * A chunk holds {@code READY_FOR_PROCESSING} only between its row committing and the
     * asynchronous dispatch call running, and that call is in-memory, so a crash or a redeploy in
     * that window strands it. The sweep moves it to {@code SCHEDULED_FOR_PROCESSING}, which is the
     * status the bulk submitter reads.
     */
    @org.junit.Test
    public void strandedInReadyForProcessing_becomesABulkCandidate() throws Exception {
        startHazelcastWith(null);
        JobEntity job = newPersistedJob();
        TrackingKey stranded = seed(job, 0, READY_FOR_PROCESSING, Instant.now().minus(Duration.ofMinutes(11)));

        persistenceContext.run(() -> newAdminBean(newDependencyTrackingService()).rescueChunksLeftReady());

        assertThat("handed to the bulk submitter", statusOf(stranded), is(SCHEDULED_FOR_PROCESSING.value));
        assertThat("and picked up by it", candidateKeys(), contains(stranded));
    }

    /**
     * A chunk stale in {@code READY_FOR_DELIVERY} for five minutes is parked in
     * {@code SCHEDULED_FOR_DELIVERY}, where the bulk delivery sweep takes it. Five minutes rather
     * than the processing side's ten because this window covers a real round trip to a sink, see
     * {@code AdminBean.rescueChunksLeftReady}.
     */
    @org.junit.Test
    public void strandedInReadyForDelivery_isParkedForTheDeliverySweep() throws Exception {
        startHazelcastWith(null);
        JobEntity job = newPersistedJob();
        TrackingKey stranded = seed(job, 0, READY_FOR_DELIVERY, Instant.now().minus(Duration.ofMinutes(6)));

        persistenceContext.run(() -> newAdminBean(newDependencyTrackingService()).rescueChunksLeftReady());

        assertThat(statusOf(stranded), is(SCHEDULED_FOR_DELIVERY.value));
    }

    /**
     * A chunk whose dispatch attempt is still in flight is not taken off it. The sweep and the
     * dispatcher would otherwise fight over the same chunks whenever a partitioning burst leaves the
     * asynchronous calls queued.
     */
    @org.junit.Test
    public void recentlyReady_isLeftForItsOwnDispatchAttempt() throws Exception {
        startHazelcastWith(null);
        JobEntity job = newPersistedJob();
        TrackingKey processing = seed(job, 0, READY_FOR_PROCESSING, Instant.now().minus(Duration.ofMinutes(9)));
        TrackingKey delivery = seed(job, 1, READY_FOR_DELIVERY, Instant.now().minus(Duration.ofMinutes(4)));

        persistenceContext.run(() -> newAdminBean(newDependencyTrackingService()).rescueChunksLeftReady());

        assertThat("processing", statusOf(processing), is(READY_FOR_PROCESSING.value));
        assertThat("delivery", statusOf(delivery), is(READY_FOR_DELIVERY.value));
    }

    /**
     * The write is validated, so a chunk that moved on between the query and the write is left where
     * it got to. Unvalidated, the sweep would drag a chunk the processor already holds back to
     * {@code SCHEDULED_FOR_PROCESSING} and the bulk submitter would send it a second time.
     * <p>
     * The race is arranged rather than hoped for: the service hands the sweep a stale row and
     * advances that same chunk before returning, which is exactly the interleaving a concurrent
     * dispatch produces.
     */
    @org.junit.Test
    public void chunkThatMovedOnBetweenQueryAndWrite_isLeftAlone() throws Exception {
        startHazelcastWith(null);
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING, Instant.now().minus(Duration.ofMinutes(11)));

        DependencyTrackingService dispatchingBehindTheSweep = new DependencyTrackingService() {
            @Override
            public List<DependencyTrackingRO> getStaleDependencies(ChunkSchedulingStatus status, Duration timeout) {
                List<DependencyTrackingRO> stale = super.getStaleDependencies(status, timeout);
                if (status == READY_FOR_PROCESSING) {
                    setValidatedStatus(key, QUEUED_FOR_PROCESSING);
                }
                return stale;
            }
        }.withRepository(newDependencyTrackingRepository()).init();

        persistenceContext.run(() -> newAdminBean(dispatchingBehindTheSweep).rescueChunksLeftReady());

        assertThat("left with the processor", statusOf(key), is(QUEUED_FOR_PROCESSING.value));
    }

    // ---------------------------------------------------------------- fixtures

    private AdminBean newAdminBean(DependencyTrackingService trackingService) {
        AdminBean adminBean = new AdminBean();
        adminBean.dependencyTrackingService = trackingService;
        return adminBean;
    }

    private List<TrackingKey> candidateKeys() {
        return newDependencyTrackingRepository().findProcessingCandidates(SINK_ID, 10).stream()
                .map(DependencyTrackingRepository.ProcessingCandidate::key)
                .toList();
    }

    /**
     * Writes the chunk's row straight to PostgreSQL with the {@code lastmodified} the test needs,
     * which is the whole point of the fixture: no statement lets a caller backdate it. The chunk row
     * has to exist first, {@code dependencytracking_jobid_fkey} is a foreign key on
     * {@code (jobid, chunkid)}.
     */
    private TrackingKey seed(JobEntity job, int chunkId, ChunkSchedulingStatus status, Instant lastModified)
            throws SQLException {
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into dependencytracking (jobid, chunkid, sinkid, submitter, status, priority, lastmodified) " +
                             "values (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, job.getId());
            statement.setInt(2, chunkId);
            statement.setInt(3, SINK_ID);
            statement.setInt(4, (int) SUBMITTER);
            statement.setInt(5, status.value);
            statement.setInt(6, Priority.NORMAL.getValue());
            statement.setTimestamp(7, Timestamp.from(lastModified));
            statement.executeUpdate();
            // newConnection() hands out a connection with auto-commit off.
            connection.commit();
        }
        return new TrackingKey(job.getId(), chunkId);
    }

    private int statusOf(TrackingKey key) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select status from dependencytracking where jobid = ? and chunkid = ?")) {
            statement.setInt(1, key.getJobId());
            statement.setInt(2, key.getChunkId());
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private JobEntity newPersistedJob() {
        JobEntity jobEntity = newJobEntity(SUBMITTER);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(SINK_ID)
                .setContent(new SinkContentBuilder().setSinkType(SinkContent.SinkType.DUMMY).build())
                .build()));
        persist(jobEntity);
        return jobEntity;
    }
}
