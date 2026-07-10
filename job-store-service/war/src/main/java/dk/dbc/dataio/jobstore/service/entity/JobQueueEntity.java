package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.RecordSplitter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "jobQueue")
@NamedQueries({
        @NamedQuery(name = JobQueueEntity.NQ_FIND_BY_STATE,
                query = "SELECT jq FROM JobQueueEntity jq WHERE jq.state = :" + JobQueueEntity.FIELD_STATE),
        @NamedQuery(name = JobQueueEntity.DELETE_BY_JOBID,
                query = "DELETE FROM JobQueueEntity jq WHERE jq.job.id=:jobId")
})
@NamedNativeQueries({
        @NamedNativeQuery(name = JobQueueEntity.NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER, query =
                // This query uses three cooperating predicates to prevent a cross-system deadlock
                // while upholding the same-submitter ordering guarantee.  See the comment on
                // NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER below for the full explanation.
                "SELECT jq.* FROM jobqueue jq INNER JOIN job ON jq.jobid = job.id " +
                        "WHERE jq.sinkId = ?" + JobQueueEntity.FIELD_SINK_ID + " " +
                        // (1) State filter — the deadlock fix.
                        //     Under READ COMMITTED, when FOR UPDATE finds a candidate row locked by
                        //     another transaction it waits, then re-evaluates (EvalPlanQual, EPQ) all
                        //     WHERE predicates against the *newly committed* row version.  Without this
                        //     predicate, a row that just transitioned to IN_PROGRESS still passes all
                        //     other checks (sinkId matches; the NOT IN subquery ran against the stale
                        //     snapshot where the submitter was not yet excluded), so FOR UPDATE locks
                        //     it and hands it to EclipseLink for cloning.  With this predicate, EPQ
                        //     re-checks state='WAITING' against the committed version, sees IN_PROGRESS,
                        //     and rejects the row — FOR UPDATE never acquires the lock, EclipseLink
                        //     never touches it, and the cross-system cycle cannot form.
                        "AND jq.state = 'WAITING' " +
                        // (2) Prior-entry guard — the ordering guarantee.  A candidate row is only
                        // eligible if no earlier queue entry (lower id) exists for the same submitter
                        // and sink, regardless of that earlier entry's state.  Presence is
                        // snapshot-stable: the earlier row only disappears after the DELETE commits,
                        // so a submitter's later rows stay excluded for the full lifetime of any
                        // older entry — even across the stale-snapshot window that (1) and SKIP
                        // LOCKED open by stepping over IN_PROGRESS or locked head rows.
                        "AND NOT EXISTS (" +
                        "SELECT 1 FROM jobqueue prior INNER JOIN job pjob ON prior.jobid = pjob.id " +
                        "WHERE prior.sinkid = jq.sinkid AND prior.id < jq.id " +
                        "AND pjob.specification->>'submitterId' = job.specification->>'submitterId') " +
                        // (3) Submitter-exclusion subquery — belt-and-braces defence of the same
                        // invariant as (2).  Logically redundant now (an IN_PROGRESS row fails (1);
                        // any later same-submitter row is excluded by (2)), but kept as defence-in-depth.
                        "AND job.specification->>'submitterId' NOT IN (" +
                        "SELECT specification->>'submitterId' FROM jobqueue jq_join INNER JOIN job ON jq_join.jobid = job.id " +
                        "WHERE jq_join.state = 'IN_PROGRESS' AND jq_join.sinkId = ?" + JobQueueEntity.FIELD_SINK_ID + ") " +
                        "ORDER BY jq.id ASC LIMIT 1 " +
                        // FOR UPDATE OF jq — restricts the row lock to jobqueue; an unqualified
                        // FOR UPDATE would also lock the joined job row, recreating the round-1
                        // deadlock against chunk creation (fixed in PR #294).
                        // SKIP LOCKED — safe once the prior-entry guard (2) is in place.  A
                        // concurrent seize that already holds the head row's lock does not stall
                        // other seizes; they skip to a different submitter's head or return empty.
                        "FOR UPDATE OF jq SKIP LOCKED;",
                resultClass = JobQueueEntity.class),
})
public class JobQueueEntity {
    public static final String NQ_FIND_BY_STATE = "NQ_FIND_BY_STATE";
    // Native SQL because JPQL does not support PostgreSQL JSON operators.
    //
    // DEADLOCK HISTORY — two rounds, identical cross-system structure, different rows:
    //
    // The deadlock involves two locking systems that cannot observe each other:
    //   PostgreSQL row locks     — held by FOR UPDATE while EclipseLink materialises the result set
    //   EclipseLink cache-key WriteLocks — held during merge/remove before the SQL DELETE runs
    //
    // Round 1 (PR #294 fix): the unqualified FOR UPDATE locked both jq and job rows; chunk
    //   creation also locked job rows, forming the cycle.  Fixed by restricting to FOR UPDATE OF jq.
    //
    // Round 2 (this fix): the cycle moved to the jobqueue row itself.
    //   Seize thread S:   holds PG row-lock on jobqueue row J → needs EclipseLink cache-key on J
    //   Remove thread R:  holds EclipseLink cache-key on J    → needs PG row-lock on J (the DELETE)
    //
    //   Neither half times out: PostgreSQL sees S as idle-in-transaction; the JVM cannot observe
    //   the PG lock wait.  Both threads hang permanently; the queue entry stays IN_PROGRESS until
    //   a pod restart.
    //
    // ROOT CAUSE: the old query had no jq.state = 'WAITING' predicate.  Under READ COMMITTED,
    // a seize whose snapshot was taken just before another seize's commit can be handed an
    // IN_PROGRESS row via PostgreSQL's EvalPlanQual recheck: the sinkId still matches, and the
    // NOT IN subquery runs against the stale snapshot in which the submitter was not yet excluded.
    // FOR UPDATE then locks the row and hands it to EclipseLink for cloning — if the remove
    // transaction is already past its merge() call (holding the cache-key lock), the cycle closes.
    //
    // WHY THE NAIVE FIX BREAKS THE ORDERING GUARANTEE:
    //   Requirement: jobs from the same submitter on the same sink must be partitioned strictly
    //   one at a time, in queue-id order.
    //
    //   Adding AND state='WAITING' alone (or SKIP LOCKED alone) lets the scan step over a
    //   locked/IN_PROGRESS head row and land on a same-submitter later entry whose submitter
    //   still appears free in the stale snapshot — two concurrent partitionings of the same
    //   submitter's jobs, violating the ordering guarantee.
    //
    // FIX — three cooperating predicates; see inline query comments for per-clause detail:
    //
    //   (1) AND jq.state = 'WAITING'
    //       Under READ COMMITTED, when FOR UPDATE finds a candidate row locked by another
    //       transaction it waits, then re-evaluates (EvalPlanQual, EPQ) all WHERE predicates
    //       against the *newly committed* row version.  This predicate is re-checked by EPQ,
    //       so a row that just became IN_PROGRESS is rejected before FOR UPDATE acquires the
    //       lock.  Seize and remove are therefore disjoint at the row level → cycle impossible.
    //
    //   (2) NOT EXISTS prior-entry guard
    //       Asks whether any earlier queue entry (lower id) exists for this submitter and sink,
    //       regardless of state.  Presence is snapshot-stable: the row only vanishes after DELETE
    //       commits.  So a submitter's later rows stay blocked for the full lifetime of any older
    //       entry, closing the ordering-violation window that (1) and SKIP LOCKED open.
    //
    //   (3) SKIP LOCKED
    //       Safe only because (2) is in place.  Concurrent seizes skip locked head rows and take
    //       a different submitter's head instead of queuing behind the lock.
    //
    // The original NOT IN subquery is now logically redundant but kept as belt-and-braces defence.
    public static final String NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER = "NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER";
    public static final String DELETE_BY_JOBID = "JobQueueEntity.deleteByJobId";

    public static final String FIELD_SINK_ID = "sinkId";
    public static final String FIELD_STATE = "state";

    public enum State {IN_PROGRESS, WAITING}

    @Id
    @SequenceGenerator(
            name = "jobqueue_id_seq",
            sequenceName = "jobqueue_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "jobqueue_id_seq")
    @Column(updatable = false)
    private int id;

    @Column(updatable = false)
    private Timestamp timeOfEntry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jobId", updatable = false)
    private JobEntity job;

    @Column(updatable = false)
    private int sinkId;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(updatable = false)
    @Enumerated(EnumType.STRING)
    private RecordSplitter recordSplitterType;

    private int retries;

    @Column(updatable = false)
    private byte[] includeFilter;

    public JobQueueEntity() {
    }

    @PrePersist
    public void setTimeOfEntry() {
        timeOfEntry = new Timestamp(System.currentTimeMillis());
    }

    public int getId() {
        return id;
    }

    public JobQueueEntity withId(int id) {
        this.id = id;
        return this;
    }

    public JobEntity getJob() {
        return job;
    }

    public JobQueueEntity withJob(JobEntity job) {
        this.job = job;
        return this;
    }

    public int getSinkId() {
        return sinkId;
    }

    public JobQueueEntity withSinkId(int sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public State getState() {
        return state;
    }

    public JobQueueEntity withState(State state) {
        this.state = state;
        return this;
    }

    public Timestamp getTimeOfEntry() {
        return timeOfEntry;
    }

    public RecordSplitter getTypeOfDataPartitioner() {
        return recordSplitterType;
    }

    public JobQueueEntity withTypeOfDataPartitioner(RecordSplitter recordSplitterType) {
        this.recordSplitterType = recordSplitterType;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public JobQueueEntity withRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public byte[] getIncludeFilter() {
        return this.includeFilter;
    }

    public JobQueueEntity withIncludeFilter(byte[] includeFilter) {
        this.includeFilter = includeFilter;
        return this;
    }
}
