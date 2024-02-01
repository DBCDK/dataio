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
                "SELECT jq.* FROM jobqueue jq INNER JOIN job ON jq.jobid = job.id " +
                        "WHERE job.specification->>'submitterId' NOT IN(" +
                        "SELECT specification->>'submitterId' FROM jobqueue jq_join INNER JOIN job ON jq_join.jobid = job.id " +
                        "WHERE jq_join.state = 'IN_PROGRESS' AND jq_join.sinkId = ?" + JobQueueEntity.FIELD_SINK_ID + ") " +
                        "AND jq.sinkId = ?" + JobQueueEntity.FIELD_SINK_ID + " ORDER BY jq.id ASC LIMIT 1 FOR UPDATE;",
                resultClass = JobQueueEntity.class),
})
public class JobQueueEntity {
    public static final String NQ_FIND_BY_STATE = "NQ_FIND_BY_STATE";
    // this is native sql because jpql doesn't support json operators.
    // finds jobqueue entities with submitter ids which are not already in jobs marked as in progress
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
    private long sinkId;

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

    public long getSinkId() {
        return sinkId;
    }

    public JobQueueEntity withSinkId(long sinkId) {
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
