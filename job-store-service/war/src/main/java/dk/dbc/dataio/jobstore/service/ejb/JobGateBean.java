package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalInt;

/**
 * The delivery half of the per-job gate on termination chunks.
 * <p>
 * A termination chunk must not be dispatched until every data chunk of its own job has been
 * acknowledged as delivered, and until no earlier job with the same submitter on the same sink
 * still holds an unlifted termination barrier. This bean owns the evaluation site that fires on
 * chunk delivery. The other site is the insert of the termination chunk itself, in
 * {@link PgJobStoreRepository#createJobTerminationChunkEntity}. Which of the two opens a given
 * job's gate comes down to the order of two events, the job's last data-chunk delivery and the
 * insert of its termination chunk. Delivery last, and this site opens the gate. Insert last, and
 * there is no delivery left to fire on, so the insert has to evaluate the gate itself. A job with
 * no data chunks at all has no delivery either way.
 * <p>
 * This bean writes gate state and nothing else. It dispatches no chunk and holds none back
 * directly: the dispatch path does that, by filtering on {@code gate_open}.
 * <p>
 * <b>Lock ordering.</b> Every site that touches gate state takes its locks in one order:
 * <ol>
 * <li>the job row, at most one per transaction,</li>
 * <li>then the barrier scope's advisory lock,</li>
 * <li>then {@code dependencytracking} rows, only ever inside the advisory lock.</li>
 * </ol>
 * No transaction may wait for a job row while holding the advisory lock. That is what rules out
 * the cycle, and it is not academic: a job's last data chunk and its termination chunk can be
 * acknowledged concurrently, and the two would then hold and want the same job row and the same
 * barrier scope in opposite orders. Deliveries within a job are not ordered, because
 * {@code optimizeDependencies} prunes the termination chunk's {@code waitingOn} down to the chunks
 * that transitively cover the rest, so it can be dispatched while an earlier data chunk of its own
 * job is still in flight.
 * <p>
 * Two consequences that look wrong until read against that rule. The re-trigger writes
 * {@code termination_barrier_lifted} <i>before</i> taking the advisory lock, and
 * {@link PgJobStoreRepository#createJobTerminationChunkEntity} takes it only after the job row lock
 * it needs anyway. Both still decide under the lock, which is what correctness turns on.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Barrier Chunks - Per-Job Gate".
 */
@Stateless
public class JobGateBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobGateBean.class);

    @EJB
    JobGateRepository jobGateRepository;

    public JobGateBean() {
    }

    public JobGateBean(JobGateRepository jobGateRepository) {
        this.jobGateRepository = jobGateRepository;
    }

    /**
     * Advances the gate state for a chunk whose delivery has just been acknowledged.
     * <p>
     * Called from {@link JobSchedulerBean#chunkDeliveringDone} after the chunk's dependency
     * tracking entry has been removed, in the {@code JobsBean} transaction rather than in the
     * {@code REQUIRES_NEW} transaction that wrote the item's delivery result. That gives two
     * orderings: the chunk's DELIVERING write is committed before it is counted, and the
     * transaction commits before JAX-RS writes the response, so no sink can acknowledge a message
     * whose increment has not committed.
     * <p>
     * Not idempotent, and nothing here detects a repeat. The caller arrives after a {@code get}, a
     * status check and a {@code remove} on the chunk's dependency tracking entry, and those three
     * are not atomic, so two concurrent acknowledgements of one chunk can both pass them and both
     * be counted.
     *
     * @param key       delivered chunk
     * @param sinkId    sink the chunk was delivered to
     * @param submitter submitter the chunk's job belongs to
     */
    @Stopwatch
    public void advanceGateState(TrackingKey key, int sinkId, int submitter) {
        if (jobGateRepository.isTerminationChunk(key)) {
            liftBarrierAndRetrigger(key.getJobId(), sinkId, submitter);
        } else {
            countDataChunk(key.getJobId(), sinkId, submitter);
        }
    }

    /**
     * The job's termination chunk does not count itself, so only data chunks reach here.
     * <p>
     * The increment is unconditional: not guarded by the job having a termination chunk, and not by
     * {@code gate_open}. Partitioning and delivery overlap, so data chunks are delivered before the
     * termination chunk exists, and a conditional increment would lose every one of them, leaving
     * the counter permanently short once {@code data_chunks_expected} is written.
     * <p>
     * Work is ordered cheapest first so the lock tail stays short. The increment's row lock
     * serializes the counter read that follows it, and the advisory lock and the barrier query only
     * run on the one delivery that finds the counters complete and a closed gate to open. That is
     * once per job in the normal case: while the job is still partitioning there is no termination
     * row to find, and once the gate is open the row no longer matches either.
     * <p>
     * This is the branch that fixes the lock order for the other two, since the increment has to
     * take the job row before the counters can be compared at all.
     */
    private void countDataChunk(int jobId, int sinkId, int submitter) {
        jobGateRepository.incrementDataChunksDelivered(jobId);
        if (!jobGateRepository.dataChunksAccountedFor(jobId)) {
            return;
        }
        OptionalInt terminationChunkId = jobGateRepository.closedTerminationChunkId(sinkId, submitter, jobId);
        if (terminationChunkId.isEmpty()) {
            return;
        }
        jobGateRepository.advisoryLock(sinkId, submitter);
        openGateIfReady(new TrackingKey(jobId, terminationChunkId.getAsInt()), sinkId, submitter);
    }

    /**
     * The removed chunk was the job's own termination chunk, so this job stops holding later jobs
     * back and the jobs queued behind it get their gates re-evaluated.
     * <p>
     * Without the re-trigger, a job whose counter completed while an earlier termination chunk was
     * still in flight would stay closed forever: its own delivery-side evaluation has already run
     * and declined, and nothing else would look at it again.
     */
    private void liftBarrierAndRetrigger(int jobId, int sinkId, int submitter) {
        // Mark before locking the barrier scope, never after, see the lock ordering note on this
        // class. The mark is still atomic with the scan below because both commit together, and
        // what the advisory lock has to make mutually exclusive is the scan against another
        // transaction's barrier read, not the mark itself. Whichever of the two commits first, the
        // other sees it: a barrier read taken before this transaction commits reads the old flag
        // but is then followed by this scan finding that transaction's committed row, and a
        // barrier read taken after it commits reads the lifted flag directly.
        jobGateRepository.markTerminationBarrierLifted(jobId);
        jobGateRepository.advisoryLock(sinkId, submitter);
        for (TrackingKey candidate : jobGateRepository.laterClosedGates(sinkId, submitter, jobId)) {
            // openGateIfReady runs the barrier check itself, and opening a gate does not lift its
            // barrier, so only the next eligible job opens and the cascade stops by itself.
            openGateIfReady(candidate, sinkId, submitter);
        }
    }

    /**
     * Opens a closed termination chunk's gate if both halves of the gate now agree: the job's own
     * data chunks are all accounted for, and no earlier job on this submitter and sink still holds
     * an unlifted barrier.
     * <p>
     * Must be called with the barrier scope's advisory lock held.
     */
    private void openGateIfReady(TrackingKey terminationChunk, int sinkId, int submitter) {
        int jobId = terminationChunk.getJobId();
        if (!jobGateRepository.dataChunksAccountedFor(jobId)) {
            return;
        }
        if (jobGateRepository.hasEarlierUndeliveredTermination(sinkId, submitter, jobId)) {
            return;
        }
        jobGateRepository.openGate(terminationChunk);
        LOGGER.info("gate opened for termination chunk {}", terminationChunk);
    }
}
