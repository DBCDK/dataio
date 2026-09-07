package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * The hourly gate sweep, which is the backstop for every way a gate ends up closed with nothing left
 * to open it.
 * <p>
 * Two such ways, both documented under "Barrier Width - Per-Sink-Type Job Isolation" in
 * docs/chunk-scheduling-redesign.md. A termination row can be removed on a path that never lifted
 * its barrier, and a job's delivered data-chunk count can be lost when {@code chunkDeliveringDone}
 * rolls back after the map entry has already gone. Either leaves a job that never completes, so
 * these tests are about damage being bounded to one sweep interval rather than being permanent.
 */
public class JobGateSweepIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4711;
    private static final long SUBMITTER = 820010;

    /**
     * The data-chunk half. The barrier ahead was lifted without the re-trigger ever running, so the
     * sweep is the only thing left that will look at this gate.
     */
    @org.junit.Test
    public void sweepOpensDataChunkGateWithNoEarlierBarrier() throws Exception {
        JobEntity jobA = newTerminationJob();
        JobEntity jobB = newTerminationJob();
        closeDataChunkGate(jobB, 0);
        markBarrierLiftedWithoutReTrigger(jobA);

        assertThat("closed before the sweep", gateOpen(new TrackingKey(jobB.getId(), 0)), is(false));

        sweep();

        assertThat("opened by the sweep", gateOpen(new TrackingKey(jobB.getId(), 0)), is(true));
    }

    @org.junit.Test
    public void sweepLeavesDataChunkClosedWhileTheBarrierStands() throws Exception {
        JobEntity jobA = newTerminationJob();
        closeTerminationGate(jobA, 1);
        JobEntity jobB = newTerminationJob();
        closeDataChunkGate(jobB, 0);

        sweep();

        assertThat("job A still owes its job-end", gateOpen(new TrackingKey(jobB.getId(), 0)), is(false));
    }

    /**
     * The termination half. Opened on the earlier barrier alone, this chunk would be
     * dispatched while its own job's data chunks were still in flight, which is exactly what the
     * per-job gate exists to prevent.
     */
    @org.junit.Test
    public void sweepLeavesTerminationGateClosedWhileItsOwnDataChunksAreUndelivered() throws Exception {
        JobEntity job = newTerminationJob();
        closeDataChunkGate(job, 0);
        closeTerminationGate(job, 1);

        sweep();

        assertThat("its own data chunk is still tracked",
                gateOpen(new TrackingKey(job.getId(), 1)), is(false));
    }

    /**
     * Why the sweep may not read the counter it is repairing. The count is one short and no
     * delivery is left to arrive, so a sweep reading {@code data_chunks_delivered} would leave this
     * job closed forever. Reading the absence of data-chunk rows instead repairs precisely the
     * failure the sweep is there for, so this test fails on any implementation that reads the
     * counter.
     */
    @org.junit.Test
    public void sweepOpensTerminationGateWhenTheDeliveredCountWasLost() throws Exception {
        JobEntity job = newTerminationJob();
        closeTerminationGate(job, 1);
        setDataChunksExpected(job.getId(), 1);
        setDataChunksDelivered(job.getId(), 0);

        assertThat("the counter says one chunk is outstanding",
                dataChunksDelivered(job.getId()) < dataChunksExpected(job.getId()), is(true));

        sweep();

        assertThat("opened on the absence of data-chunk rows, not on the counter",
                gateOpen(new TrackingKey(job.getId(), 1)), is(true));
    }

    /**
     * The termination row is gone, so nothing will ever fire the lift for this job,
     * and every later job on its submitter and sink is held behind a barrier whose own chunk no
     * longer exists.
     */
    @org.junit.Test
    public void sweepLiftsBarrierForAJobWithNoTerminationRow() throws Exception {
        JobEntity jobA = newTerminationJob();
        setTerminationBarrierLifted(jobA.getId(), false);
        JobEntity jobB = newTerminationJob();
        closeDataChunkGate(jobB, 0);

        sweepBarriersThenGates();

        assertThat("barrier lifted", terminationBarrierLifted(jobA.getId()), is(true));
        assertThat("and the job behind it released", gateOpen(new TrackingKey(jobB.getId(), 0)), is(true));
    }

    /**
     * The guard on the lift. A job that never had a termination chunk carries NULL, which means
     * "never raised a barrier", and rewriting that to TRUE would collapse it into "raised one, now
     * lifted". The sweep must not touch it.
     */
    @org.junit.Test
    public void sweepDoesNotTouchAJobThatNeverHadABarrier() throws Exception {
        JobEntity job = newTerminationJob();

        sweepBarriersThenGates();

        assertThat("still NULL", terminationBarrierLifted(job.getId()), is((Boolean) null));
    }

    private void sweep() {
        persistenceContext.run(() -> newJobGateBean().sweepClosedGates());
    }

    private void sweepBarriersThenGates() {
        persistenceContext.run(() -> {
            JobGateBean jobGateBean = newJobGateBean();
            jobGateBean.sweepUnliftedBarriers();
            jobGateBean.sweepClosedGates();
        });
    }

    private JobEntity newTerminationJob() {
        JobEntity jobEntity = newJobEntity(SUBMITTER);
        jobEntity.setNumberOfChunks(1);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(SINK_ID)
                .setContent(new SinkContentBuilder().setSinkType(SinkContent.SinkType.TICKLE).build())
                .build()));
        persist(jobEntity);
        return jobEntity;
    }

    private void closeDataChunkGate(JobEntity job, int chunkId) {
        upsertClosedGate(job, chunkId, false);
    }

    private void closeTerminationGate(JobEntity job, int chunkId) {
        upsertClosedGate(job, chunkId, true);
        setTerminationBarrierLifted(job.getId(), false);
    }

    private void upsertClosedGate(JobEntity job, int chunkId, boolean isTermination) {
        TrackingKey key = new TrackingKey(job.getId(), chunkId);
        // dependencytracking (jobid, chunkid) is a foreign key into chunk, so the row needs its
        // chunk to exist first.
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        DependencyTracking tracker = new DependencyTracking(key, SINK_ID, (int) SUBMITTER, null, Set.of());
        persistenceContext.run(() -> newJobGateRepository().upsertGateRow(key, SINK_ID, (int) SUBMITTER,
                tracker.getStatus(), tracker.getMatchKeys(), isTermination, false));
    }

    /**
     * The failure the sweep exists for, staged directly: the barrier is standing but the termination
     * row that a lift would have fired on is already gone.
     */
    private void markBarrierLiftedWithoutReTrigger(JobEntity job) {
        setTerminationBarrierLifted(job.getId(), true);
    }

    private void setTerminationBarrierLifted(int jobId, Boolean value) {
        setJobColumn(jobId, "termination_barrier_lifted", value);
    }

    private void setDataChunksExpected(int jobId, int value) {
        setJobColumn(jobId, "data_chunks_expected", value);
    }

    private void setDataChunksDelivered(int jobId, int value) {
        setJobColumn(jobId, "data_chunks_delivered", value);
    }

    private void setJobColumn(int jobId, String column, Object value) {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "update job set " + column + " = ? where id = ?")) {
            statement.setObject(1, value);
            statement.setInt(2, jobId);
            statement.executeUpdate();
            // newConnection() hands out a connection with autoCommit off, as the base class's own
            // cleanup does, so a write that is not committed here is invisible to everything after.
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Boolean gateOpen(TrackingKey key) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select gate_open from dependencytracking where jobid = ? and chunkid = ?")) {
            statement.setInt(1, key.getJobId());
            statement.setInt(2, key.getChunkId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBoolean(1) : null;
            }
        }
    }

    private int dataChunksExpected(int jobId) throws SQLException {
        return (Integer) jobColumn(jobId, "data_chunks_expected");
    }

    private int dataChunksDelivered(int jobId) throws SQLException {
        return (Integer) jobColumn(jobId, "data_chunks_delivered");
    }

    private Boolean terminationBarrierLifted(int jobId) throws SQLException {
        return (Boolean) jobColumn(jobId, "termination_barrier_lifted");
    }

    private Object jobColumn(int jobId, String column) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + column + " from job where id = ?")) {
            statement.setInt(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1) : null;
            }
        }
    }
}
