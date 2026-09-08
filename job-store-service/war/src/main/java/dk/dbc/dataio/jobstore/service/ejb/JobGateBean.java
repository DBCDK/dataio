package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalInt;

/**
 * Owns the per-job gate, the state that decides whether a chunk may be delivered, and the writes
 * that close and reopen it.
 * <p>
 * <b>Not only termination chunks.</b> A termination chunk is gated until every data chunk of its own
 * job has been acknowledged as delivered, and until no earlier job with the same submitter on the
 * same sink still holds an unlifted termination barrier. A <i>data</i> chunk is gated on the second
 * of those alone, having no count of its own, and only for the sink types in
 * {@code JobSchedulerBean.REQUIRES_FULL_WIDTH_BARRIER}. On every other sink type a data chunk is never
 * gated at all.
 * <p>
 * This bean owns three of the four evaluation sites: the one that fires on chunk delivery
 * ({@link #advanceGateState}), the one that closes a data chunk's gate as it is scheduled
 * ({@link #closeDataChunkGateIfBlocked}), and the sweep that reopens gates nothing else will
 * ({@link #sweepClosedGates}, {@link #sweepUnliftedBarriers}). The fourth is the insert of the
 * termination chunk itself, in {@link PgJobStoreRepository#createJobTerminationChunkEntity}.
 * <p>
 * Which site opens a given job's termination gate comes down to the order of two events, the job's
 * last data-chunk delivery and the insert of its termination chunk. Delivery last, and the delivery
 * site opens the gate. Insert last, and there is no delivery left to fire on, so the insert has to
 * evaluate the gate itself. A job with no data chunks at all has no delivery either way.
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
 * barrier scope in opposite orders. Deliveries within a job are not ordered against each other, so
 * a job's termination chunk can be dispatched while an earlier data chunk of that same job is still
 * in flight.
 * <p>
 * Two consequences that look wrong until read against that rule. The re-trigger writes
 * {@code termination_barrier_lifted} <i>before</i> taking the advisory lock, and
 * {@link PgJobStoreRepository#createJobTerminationChunkEntity} takes it only after the job row lock
 * it needs anyway. Both still decide under the lock, which is what correctness turns on.
 * <p>
 * <b>Where the lock is taken is a boundary, not an ordering.</b> The advisory lock releases at
 * commit, so a caller that takes it in a long-running transaction holds it for that whole
 * transaction rather than for the gate work. Two methods therefore run in their own transaction and
 * say so in their own comments: {@link #closeDataChunkGateIfBlocked}, reached from
 * {@code scheduleChunk} inside a transaction open for the whole of a job's partitioning, and
 * {@link #sweepScope}, reached from an hourly recheck that walks every scope in one transaction.
 * Neither may take the lock in its caller's transaction at all, and
 * {@link #closeDataChunkGateIfBlocked} takes no job row lock either.
 * <p>
 * The nested boundary imposes an obligation on the callers rather than only on the callees: a
 * transaction that will nest one of those methods must not itself be holding the scope's lock, or
 * the nested transaction waits on a second connection for a transaction that cannot commit until it
 * returns. PostgreSQL cannot see that as a cycle, because the wait is on an EJB call rather than on
 * a database lock, so it is an undetectable hang rather than a detected deadlock. That is what
 * {@link #liftBarrierForRemovedJob} exists for.
 * <p>
 * See docs/chunk-scheduling-redesign.md, "Barrier Chunks - Per-Job Gate", and "Barrier Width -
 * Per-Sink-Type Job Isolation".
 */
@Stateless
public class JobGateBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobGateBean.class);

    @EJB
    JobGateRepository jobGateRepository;

    @Resource
    SessionContext sessionContext;

    public JobGateBean() {
    }

    public JobGateBean(JobGateRepository jobGateRepository) {
        this.jobGateRepository = jobGateRepository;
    }

    /**
     * The container's view of this bean, so that a call from one of its own methods to a
     * {@code REQUIRES_NEW} one is intercepted rather than bypassed as a plain self-call would be.
     * <p>
     * Load-bearing wherever it is used here: the whole point of those methods is the transaction
     * boundary, and a self-call silently runs them in the caller's transaction instead.
     * <p>
     * Null when the bean is constructed directly in a unit test, which is why the two callers fall
     * back to a direct call. That fallback runs the work in one transaction, which is correct for a
     * test holding no advisory lock and wrong in the container, so it must never be relied on
     * outside a test.
     */
    private JobGateBean self() {
        return sessionContext == null ? this : sessionContext.getBusinessObject(JobGateBean.class);
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
     * Counts once per chunk, because it is reached only by the caller that removed the chunk's
     * dependency tracking entry. That removal is atomic per key, so of two concurrent
     * acknowledgements of one chunk only one arrives here and the other is told it did not remove
     * anything. Every fact this method works from comes off the removed entry, including whether the
     * chunk was its job's termination chunk.
     *
     * @param removed the delivered chunk's removed dependency tracking entry
     */
    @Stopwatch
    public void advanceGateState(DependencyTracking removed) {
        int jobId = removed.getKey().getJobId();
        if (removed.isTermination()) {
            liftBarrierAndRetrigger(jobId, removed.getSinkId(), removed.getSubmitter());
        } else {
            countDataChunk(jobId, removed.getSinkId(), removed.getSubmitter());
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
     * The job stops holding later jobs back, and the jobs queued behind it get their gates
     * re-evaluated.
     * <p>
     * Without the re-trigger, a job whose counter completed while an earlier termination chunk was
     * still in flight would stay closed forever: its own delivery-side evaluation has already run
     * and declined, and nothing else would look at it again.
     * <p>
     * <b>Delivery is only the usual way to get here, not the only one.</b> Any removal of a
     * termination row lifts the barrier, so {@link JobsBean#abortJob} and
     * {@link dk.dbc.dataio.jobstore.service.rs.AdminBean#recheckBlocks} call this too, both of them
     * for jobs that may never have had a termination chunk at all. Skipping the lift on those paths
     * would leave the aborted job reading as still blocking for the whole of the MapStore's delete
     * delay, which is exactly the window the edge-triggered re-trigger fires in, so it would decline
     * and never fire again.
     *
     * @param jobId     job whose barrier is lifted
     * @param sinkId    sink the barrier applies to
     * @param submitter submitter the barrier applies to
     */
    public void liftBarrierAndRetrigger(int jobId, int sinkId, int submitter) {
        // Mark before locking the barrier scope, never after, see the lock ordering note on this
        // class. The mark is still atomic with the scan below because both commit together, and
        // what the advisory lock has to make mutually exclusive is the scan against another
        // transaction's barrier read, not the mark itself. Whichever of the two commits first, the
        // other sees it: a barrier read taken before this transaction commits reads the old flag
        // but is then followed by this scan finding that transaction's committed row, and a
        // barrier read taken after it commits reads the lifted flag directly.
        //
        // The guard on the update is what makes this callable for any job: no row affected means
        // this job was holding no barrier, so there is nothing behind it to release and no reason
        // to take the lock.
        if (jobGateRepository.markTerminationBarrierLifted(jobId) == 0) {
            return;
        }
        jobGateRepository.advisoryLock(sinkId, submitter);
        for (TrackingKey candidate : jobGateRepository.laterClosedGates(sinkId, submitter, jobId)) {
            // openGateIfReady runs the barrier check itself, and opening a gate does not lift its
            // barrier, so only the next eligible job opens and the cascade stops by itself.
            openGateIfReady(candidate, sinkId, submitter);
        }
        // Data chunks of later jobs, which only exist as closed gates under the full barrier width.
        // One statement rather than a loop, because a data chunk has no counter of its own, so the
        // earlier barrier is the whole condition. The width set is not consulted here and does not
        // need to be, see the note on JobGateRepository#openLaterDataChunkGates.
        int opened = jobGateRepository.openLaterDataChunkGates(sinkId, submitter, jobId);
        if (opened > 0) {
            LOGGER.info("barrier lifted for job {} opened {} data chunk gates on sink {} submitter {}",
                    jobId, opened, sinkId, submitter);
        }
    }

    /**
     * Whether a chunk scheduled into this scope has to have its gate closed, asked without taking
     * the barrier scope's lock.
     * <p>
     * The cheap half of the decision, run in the caller's transaction so the common answer costs one
     * indexed read and nothing else. It is sound in one direction only, and that is the direction it
     * is used in. A "nothing is blocking" answer cannot go stale into "blocking": a barrier for an
     * <i>earlier</i> job in this scope only comes into existence at that job's own termination
     * insert, and the jobqueue partitions jobs in one scope strictly one at a time in job id order,
     * so that job finished partitioning before this one started. A "blocking" answer can go stale
     * the other way, which is the lost wakeup, and that is precisely the answer that goes on to take
     * the lock and read again in {@link #closeDataChunkGateIfBlocked}.
     * <p>
     * It reads a fresh snapshot per statement only because the isolation level is READ COMMITTED.
     * Under REPEATABLE READ this would be pinned to the opening snapshot of a transaction that spans
     * the whole of a job's partitioning, and would go stale in both directions.
     *
     * @param sinkId    sink the chunk is destined for
     * @param submitter submitter the chunk's job belongs to
     * @param jobId     job the chunk belongs to
     * @return true if an earlier job in this scope still holds an unlifted barrier
     */
    public boolean isBlockedByEarlierBarrier(int sinkId, int submitter, int jobId) {
        return jobGateRepository.hasEarlierUndeliveredTermination(sinkId, submitter, jobId);
    }

    /**
     * Closes a data chunk's gate if an earlier job in its scope still holds a barrier.
     * <p>
     * Reached only for sink types whose job-end work is not scoped to its own job, and only after
     * {@link #isBlockedByEarlierBarrier} has already said there is a barrier to wait for. It takes
     * the lock, asks again, and writes the row only if the answer still holds. Nothing is written
     * when it does not: an unwritten gate is an open gate, so there is no such thing as writing
     * {@code gate_open = TRUE} here.
     * <p>
     * <b>{@code REQUIRES_NEW} is essential. Do not remove it.</b> The caller's transaction is the
     * one partitioning opened, and it stays open for the whole of the job, because
     * {@code partitionNextJobForSinkIfAvailable} is {@code REQUIRED} and {@code partition} is
     * {@code SUPPORTS}, which is why {@code createChunkEntity} is itself {@code REQUIRES_NEW} "to
     * enable external visibility of job creation progress". Taking the barrier scope's advisory lock
     * in that transaction would hold it until the job finished partitioning, and
     * {@code markJobAsPartitioned} is {@code REQUIRES_NEW}, so the insert of this job's own
     * termination chunk would then ask for the same lock on a different connection and wait for a
     * transaction that cannot commit until that call returns. PostgreSQL sees no lock cycle, because
     * the wait is on an EJB call rather than on a database lock, so nothing is detected and nothing
     * times out. Every job on a full-width sink would hang there.
     * <p>
     * Two further things the boundary buys. The lock is released at commit, so it is held for one
     * short transaction per closed chunk rather than for the job, which keeps it out of the way of
     * the delivery-side gate work of the job ahead. And the row is committed by the time
     * {@code scheduleChunk} returns, rather than at the end of partitioning, so it is visible to
     * dispatch for the whole window in which it is needed.
     * <p>
     * <b>This method takes no job row lock at all</b>, which is what keeps it out of the lock
     * ordering the other sites have to observe.
     * <p>
     * See docs/chunk-scheduling-redesign.md, "Barrier Width - Per-Sink-Type Job Isolation".
     *
     * @param key       data chunk being scheduled
     * @param sinkId    sink the chunk is destined for
     * @param submitter submitter the chunk's job belongs to
     * @param status    status the chunk enters dependency tracking with
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void closeDataChunkGateIfBlocked(TrackingKey key, int sinkId, int submitter,
                                            ChunkSchedulingStatus status) {
        jobGateRepository.advisoryLock(sinkId, submitter);
        if (!jobGateRepository.hasEarlierUndeliveredTermination(sinkId, submitter, key.getJobId())) {
            return;
        }
        jobGateRepository.upsertGateRow(key, sinkId, submitter, status, false, false);
        LOGGER.info("gate closed for data chunk {} behind an earlier barrier on sink {} submitter {}",
                key, sinkId, submitter);
    }

    /**
     * {@link #liftBarrierAndRetrigger} for a caller whose transaction must not be holding the
     * barrier scope's advisory lock when it returns.
     * <p>
     * The delivery path wants the opposite and keeps calling {@link #liftBarrierAndRetrigger}
     * directly: there the lift belongs in the {@code JobsBean} transaction, so that it commits
     * before JAX-RS writes the response and no sink can acknowledge a message whose gate work has
     * not committed.
     * <p>
     * The removal paths have no such response to be ordered against, and one of them goes on to
     * sweep the very scopes it has just locked, see {@link #sweepScope}. Committing the lift at once
     * is also the better failure mode, since a lift that rolls back after the tracking rows are
     * already gone is one of the two leaks the sweep exists to repair.
     *
     * @param jobId     job whose barrier is lifted
     * @param sinkId    sink the barrier applies to
     * @param submitter submitter the barrier applies to
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void liftBarrierForRemovedJob(int jobId, int sinkId, int submitter) {
        liftBarrierAndRetrigger(jobId, sinkId, submitter);
    }

    /**
     * Opens every gate that is closed with nothing left to open it.
     * <p>
     * The backstop for two ways a gate outlives its reason to be shut. A termination row can be
     * removed on a path that never lifted its barrier, and the count of a job's delivered data
     * chunks can be lost when {@code chunkDeliveringDone} rolls back after the map entry is already
     * gone. Both leave a gate closed and no edge left to fire on, which is a job that never
     * completes, so the sweep is what bounds the damage to one sweep interval. It is the same role
     * the hourly recheck already plays for chunks left blocked on dependencies that no longer exist.
     * <p>
     * Ordered per scope and under that scope's lock, so a gate is never opened on a barrier reading
     * that another transaction is in the middle of changing.
     *
     * @return the number of gates opened
     */
    public int sweepClosedGates() {
        int opened = 0;
        for (JobGateRepository.BarrierScope scope : jobGateRepository.closedGateScopes()) {
            opened += self().sweepScope(scope.sinkId(), scope.submitter());
        }
        return opened;
    }

    /**
     * Opens every gate in one barrier scope that has nothing left holding it, under that scope's
     * advisory lock.
     * <p>
     * A data chunk's gate has one condition, that no earlier job in the scope still holds an
     * unlifted barrier, so a single statement settles every data chunk in the scope at once. A
     * termination chunk's gate has that same condition and a second one, that its own job's data
     * chunks have all left dependency tracking, so those are walked a job at a time. Opened on the
     * earlier barrier alone, a termination chunk would be dispatched while its own job's data was
     * still in flight, which is the thing the per-job gate exists to prevent.
     * <p>
     * That second condition is read as the absence of the job's data-chunk rows rather than from
     * {@code data_chunks_delivered}, because a lost increment is one of the two failures this sweep
     * exists to repair, and it cannot repair that by reading the counter it left short.
     * <p>
     * <b>{@code REQUIRES_NEW} is what makes the lock per scope rather than per sweep.</b> The
     * advisory lock releases at commit, so taken in the hourly recheck's own transaction it would
     * be held until the whole recheck finished, and every scope the sweep had visited would still
     * be locked while the last one was being swept. A delivery acknowledgement for any of those
     * scopes takes the same lock in the JAX-RS transaction, so the sink would sit on an open HTTP
     * request for the rest of the recheck, with no {@code lock_timeout} to cut it short. One
     * transaction per scope bounds that wait to the scope actually being worked on.
     * <p>
     * Committing per scope loses nothing. Scopes share no invariant, so a sweep that dies halfway
     * leaves the scopes it finished correctly swept instead of rolling all of them back, and
     * opening a gate is idempotent against the next sweep either way.
     * <p>
     * Safe only because the caller's transaction holds no advisory lock of its own. Both of the
     * recheck's other lock takers, {@link #liftBarrierForRemovedJob} and the lifts inside
     * {@link #sweepUnliftedBarriers}, run in their own transactions for that reason. Were one of
     * them to lock a scope in the recheck's transaction, this method would ask for that lock on a
     * second connection and wait for a transaction that cannot commit until it returns, which
     * PostgreSQL cannot see as a cycle because the wait is on an EJB call.
     *
     * @return the number of gates opened in this scope
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int sweepScope(int sinkId, int submitter) {
        jobGateRepository.advisoryLock(sinkId, submitter);
        int opened = jobGateRepository.openDataChunkGates(sinkId, submitter);
        for (TrackingKey terminationChunk : jobGateRepository.closedTerminationGates(sinkId, submitter)) {
            int jobId = terminationChunk.getJobId();
            if (jobGateRepository.hasEarlierUndeliveredTermination(sinkId, submitter, jobId)) {
                continue;
            }
            if (jobGateRepository.hasUndeliveredDataChunks(jobId)) {
                continue;
            }
            jobGateRepository.openGate(terminationChunk);
            opened++;
            LOGGER.info("sweep opened the gate of termination chunk {}", terminationChunk);
        }
        if (opened > 0) {
            LOGGER.info("sweep opened {} gates on sink {} submitter {}", opened, sinkId, submitter);
        }
        return opened;
    }

    /**
     * Lifts the barriers that nothing else will ever lift, because the termination row a lift would
     * have fired on is already gone.
     * <p>
     * Runs before {@link #sweepClosedGates} in both of its callers, since lifting these is what
     * makes the gates queued behind them openable in the same pass.
     *
     * @return the number of barriers lifted
     */
    public int sweepUnliftedBarriers() {
        int lifted = 0;
        for (JobGateRepository.JobBarrier barrier : jobGateRepository.jobsWithUnliftedBarrierAndNoTerminationRow()) {
            LOGGER.info("sweep lifting the barrier of job {}, which has no termination chunk left to lift it",
                    barrier.jobId());
            // Its own transaction, so this scope's lock is not still held when sweepClosedGates
            // asks for it on another connection. See the note on sweepScope.
            self().liftBarrierForRemovedJob(barrier.jobId(), barrier.sinkId(), barrier.submitter());
            lifted++;
        }
        return lifted;
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
