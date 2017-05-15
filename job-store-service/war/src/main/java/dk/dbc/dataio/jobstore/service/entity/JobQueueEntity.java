package dk.dbc.dataio.jobstore.service.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;

@Entity
@Table(name = "jobQueue")
@NamedQueries({
        @NamedQuery(name = JobQueueEntity.NQ_FIND_UNIQUE_SINKS,
                query = "SELECT DISTINCT(jq.sinkId) FROM JobQueueEntity jq"),

        @NamedQuery(name = JobQueueEntity.NQ_FIND_QUEUE_FOR_SINK,
                query = "SELECT jq FROM JobQueueEntity jq WHERE jq.sinkId = :" + JobQueueEntity.FIELD_SINK_ID + " ORDER BY jq.id ASC"),

        @NamedQuery(name = JobQueueEntity.NQ_FIND_BY_STATE,
                query = "SELECT jq FROM JobQueueEntity jq WHERE jq.state = :" + JobQueueEntity.FIELD_STATE),
})
public class JobQueueEntity {
    public static final String NQ_FIND_UNIQUE_SINKS = "NQ_FIND_UNIQUE_SINKS";
    public static final String NQ_FIND_QUEUE_FOR_SINK = "NQ_FIND_QUEUE_FOR_SINK";
    public static final String NQ_FIND_BY_STATE = "NQ_FIND_BY_STATE";

    public static final String FIELD_SINK_ID = "sinkId";
    public static final String FIELD_STATE = "state";

    // this is native sql because jpql doesn't support json operators.
    // finds jobqueue entities with submitter ids which are not already in jobs marked as in progress
    public static final String FIND_QUEUE_FOR_SINK_BY_AVAILABLE_SUBMITTER =
        "SELECT jq.* FROM jobqueue jq INNER JOIN job ON jq.jobid = job.id " +
        "WHERE job.specification->>'submitterId' NOT IN(SELECT specification->>'submitterId' " +
        "FROM jobqueue jq_join INNER JOIN job ON jq_join.jobid = job.id " +
        "WHERE jq_join.state = 'IN_PROGRESS' AND jq_join.sinkId = ?" + FIELD_SINK_ID + ") " +
        "AND jq.sinkId = ?" + FIELD_SINK_ID + " ORDER BY jq.id ASC;";

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
    @JoinColumn(name="jobId", updatable = false)
    private JobEntity job;

    @Column(updatable = false)
    private long sinkId;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(updatable = false)
    @Enumerated(EnumType.STRING)
    private RecordSplitter recordSplitterType;

    private int retries;

    public JobQueueEntity() {}

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
}
