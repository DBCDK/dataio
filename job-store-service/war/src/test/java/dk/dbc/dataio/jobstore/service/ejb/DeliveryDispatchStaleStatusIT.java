package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Delete this class whole, do not maintain it.</b>
 * <p>
 * Everything here covers one temporary situation: {@code dependencytracking} is a write-behind
 * projection of the Hazelcast map, so the table's {@code status} lags the map's by up to
 * {@code write-delay-seconds}, and the bulk sweep compensates for that in two ways — it re-checks
 * every candidate against the map before acting on it, and it asks for a candidate window wider
 * than the free queue slots. Those two compensations, {@code JobSchedulerBean.isStillAwaitingDelivery}
 * and {@code JobSchedulerBean.staleCandidateSlack}, are the subject of these tests and nothing else
 * is.
 * <p>
 * When the map goes and the table becomes the sole store, both compensations are deleted and this
 * file goes with them in the same commit. Nothing in it is a statement about how delivery dispatch
 * ought to work, so there is never a reason to port a test from here to
 * {@link DeliveryDispatchIT}, which holds the tests that outlive the map. It is deliberately
 * self-contained, fixtures included, so that deleting it takes nothing else with it.
 * <p>
 * The fixtures seed the table row <em>after</em> the map entry, on purpose: the MapStore would
 * otherwise overwrite the seeded row within {@code write-delay-seconds}, and the point of every
 * test here is to hold the two out of step.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Delivery Ordering".
 */
public class DeliveryDispatchStaleStatusIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4713;
    private static final long SUBMITTER = 820020;

    /**
     * A row whose status is stale because the chunk has already been dispatched is skipped, and a
     * genuinely schedulable chunk further down the order is dispatched in its place.
     * <p>
     * The second half is what the over-fetch exists for. With a window of exactly the free slots,
     * the stale rows sort first and fill it, and the live chunk behind them is never reached.
     */
    @org.junit.Test
    public void staleQueuedCandidateIsSkippedAndTheLiveOneBehindItIsDispatched() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();

        TrackingKey alreadyDispatched = new TrackingKey(job.getId(), 0);
        TrackingKey stillWaiting = new TrackingKey(job.getId(), 1);
        trackingService.add(tracker(alreadyDispatched, QUEUED_FOR_DELIVERY));
        trackingService.add(tracker(stillWaiting, SCHEDULED_FOR_DELIVERY));

        // After the map, so the seeded status is the one the query sees.
        seedCandidate(job, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY);
        seedCandidate(job, 1, Priority.NORMAL, SCHEDULED_FOR_DELIVERY);

        // Without this the test could pass because the query returned nothing at all, rather than
        // because the skip did its job.
        assertThat("the stale row is offered as a candidate", candidates(10),
                contains(alreadyDispatched, stillWaiting));

        JobSchedulerTransactionsBean transactions = dispatchingTransactionsBean();
        bulkSchedule(trackingService, transactions, newDeliveryDispatchRepository());

        verify(transactions, never()).submitToDeliveringNewTransaction(alreadyDispatched);
        verify(transactions, times(1)).submitToDeliveringNewTransaction(stillWaiting);
    }

    /**
     * The candidate window reaches past the free queue slots.
     * <p>
     * Asserted on the limit rather than on an outcome, because reproducing the failure it prevents
     * needs the sink's queue near its thousand-entry cap: only then are there more stale rows than
     * free slots, so that a window of exactly the free slots is filled by chunks that have already
     * gone and the sink stops delivering until the MapStore flushes.
     */
    @org.junit.Test
    public void candidateWindowReachesPastTheFreeSlots() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        seedCandidate(job, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY);

        int freeSlots = trackingService.capacity(SINK_ID, QUEUED_FOR_DELIVERY);
        DeliveryDispatchRepository repository = spy(newDeliveryDispatchRepository());
        bulkSchedule(trackingService, dispatchingTransactionsBean(), repository);

        verify(repository).findDeliveryCandidates(eq(SINK_ID), intThat(limit -> limit > freeSlots));
    }

    /**
     * A row left behind by a chunk that has already been delivered and removed from the map is
     * skipped, and looking it up does not load it back into the map.
     * <p>
     * Resurrecting it would be worse than a wasted cycle: the reloaded tracker carries the row's
     * stale {@code SCHEDULED_FOR_DELIVERY}, passes every check, and the chunk is delivered twice.
     * The lookup is {@code IMap.get}, a read-through, so what keeps the row from becoming a tracker
     * again is Hazelcast answering null for a key whose {@code DELETE} is still queued in the
     * write-behind staging area. This test is what fails if a Hazelcast upgrade changes that.
     */
    @org.junit.Test
    public void deliveredCandidateIsSkippedAndNotResurrected() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();

        TrackingKey delivered = new TrackingKey(job.getId(), 0);
        trackingService.add(tracker(delivered, QUEUED_FOR_DELIVERY));
        trackingService.remove(delivered);

        // The MapStore deletes the row on its own schedule, so it outlives the map entry.
        seedCandidate(job, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY);

        assertThat("the orphaned row is offered as a candidate", candidates(10), contains(delivered));

        JobSchedulerTransactionsBean transactions = dispatchingTransactionsBean();
        bulkSchedule(trackingService, transactions, newDeliveryDispatchRepository());

        verify(transactions, never()).submitToDeliveringNewTransaction(any());
        assertThat("chunk was not loaded back into the map",
                trackingService.getSnapshot(job.getId()), is(empty()));
    }

    // ---------------------------------------------------------------- fixtures

    private List<TrackingKey> candidates(int limit) {
        return newDeliveryDispatchRepository().findDeliveryCandidates(SINK_ID, limit);
    }

    private void bulkSchedule(DependencyTrackingService trackingService, JobSchedulerTransactionsBean transactions,
                              DeliveryDispatchRepository repository) {
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, transactions, null, null,
                trackingService, newJobGateBean(), repository);
        JobsBeanTest.notAborted(0, jb -> bean.bulkScheduleToDeliveringForSink(SINK_ID));
    }

    /**
     * Reports every dispatch as successful, so that a candidate reaching it is not then dropped from
     * dependency tracking by the caller's no-items branch.
     */
    private JobSchedulerTransactionsBean dispatchingTransactionsBean() {
        JobSchedulerTransactionsBean transactions = mock(JobSchedulerTransactionsBean.class);
        when(transactions.submitToDeliveringNewTransaction(any())).thenReturn(true);
        return transactions;
    }

    private DependencyTracking tracker(TrackingKey key, ChunkSchedulingStatus status) {
        return new DependencyTracking(key, SINK_ID, (int) SUBMITTER, null, Set.of())
                .setPriority(Priority.NORMAL.getValue())
                .setStatus(status);
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

    /**
     * Writes the chunk's {@code dependencytracking} row straight to PostgreSQL, with an open gate:
     * no test here is about the gate. The chunk row has to exist first,
     * {@code dependencytracking_jobid_fkey} is a foreign key on {@code (jobid, chunkid)}.
     */
    private void seedCandidate(JobEntity job, int chunkId, Priority priority,
                               ChunkSchedulingStatus status) throws SQLException {
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into dependencytracking (jobid, chunkid, sinkid, submitter, status, priority, gate_open) " +
                             "values (?, ?, ?, ?, ?, ?, true) " +
                             "on conflict on constraint dependencytracking_pkey do update " +
                             "  set status = excluded.status, priority = excluded.priority, " +
                             "      gate_open = excluded.gate_open")) {
            statement.setInt(1, job.getId());
            statement.setInt(2, chunkId);
            statement.setInt(3, SINK_ID);
            statement.setInt(4, (int) SUBMITTER);
            statement.setInt(5, status.value);
            statement.setInt(6, priority.getValue());
            statement.executeUpdate();
            // newConnection() hands out a connection with auto-commit off.
            connection.commit();
        }
    }
}
