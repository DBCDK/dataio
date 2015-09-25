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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;

@Entity
@Table(name = "jobQueue")
@NamedQueries({
        @NamedQuery(name = JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK,
                query = "SELECT count(jq) FROM JobQueueEntity jq WHERE jq.state = :" + JobQueueEntity.FIELD_STATE + " AND jq.sinkId = :" + JobQueueEntity.FIELD_SINK_ID),

        @NamedQuery(name = JobQueueEntity.NQ_FIND_BY_JOB,
                query = "SELECT jq FROM JobQueueEntity jq WHERE jq.job = :" + JobQueueEntity.FIELD_JOB_ID),

        @NamedQuery(name = JobQueueEntity.NQ_FIND_WAITING_JOBS_BY_SINK,
                query = "SELECT jq FROM JobQueueEntity jq WHERE jq.sinkId = :" + JobQueueEntity.FIELD_SINK_ID + " AND jq.state = :" + JobQueueEntity.FIELD_STATE + " ORDER BY jq.timeOfEntry"),

        @NamedQuery(name = JobQueueEntity.NQ_FIND_UNIQUE_SINKS,
                query = "SELECT DISTINCT(jq.sinkId) FROM JobQueueEntity jq")
})
public class JobQueueEntity {

    public final static boolean OCCUPIED = true;
    public final static boolean AVAILABLE = false;

    public static final String NQ_FIND_NUMBER_OF_JOBS_BY_SINK = "NQ_FIND_NUMBER_OF_JOBS_BY_SINK";
    public static final String NQ_FIND_BY_JOB = "NQ_FIND_BY_JOB";
    public static final String NQ_FIND_WAITING_JOBS_BY_SINK = "NQ_FIND_WAITING_JOBS_BY_SINK";
    public static final String NQ_FIND_UNIQUE_SINKS = "NQ_FIND_UNIQUE_SINKS";

    public static final String FIELD_SINK_ID = "sinkId";
    public static final String FIELD_JOB_ID = "job";
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
    @JoinColumn(name="jobId", updatable = false)
    private JobEntity job;

    @Column(updatable = false)
    private long sinkId;

    @Enumerated(EnumType.STRING)
    private State state;

    @Column(updatable = false)
    private boolean sequenceAnalysis;

    @Column(updatable = false)
    @Enumerated(EnumType.STRING)
    private RecordSplitter recordSplitterType;

    public JobQueueEntity() {}
    public JobQueueEntity(long sinkId, JobEntity job, State state, boolean doSequenceAnalysis, RecordSplitter recordSplitterType) {
        this.sinkId = sinkId;
        this.job = job;
        this.state = state;
        this.timeOfEntry = new Timestamp(System.currentTimeMillis());
        this.sequenceAnalysis = doSequenceAnalysis;
        this.recordSplitterType = recordSplitterType;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public JobEntity getJob() {
        return job;
    }
    public void setJob(JobEntity job) {
        this.job = job;
    }

    public long getSinkId() {
        return sinkId;
    }
    public void setSinkId(long sinkId) {
        this.sinkId = sinkId;
    }

    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }

    public Timestamp getTimeOfEntry() {
        return timeOfEntry;
    }
    public void setTimeOfEntry(Timestamp timeOfEntry) {
        this.timeOfEntry = timeOfEntry;
    }

    public boolean isSequenceAnalysis() {
        return sequenceAnalysis;
    }
    public void setSequenceAnalysis(boolean sequenceAnalysis) {
        this.sequenceAnalysis = sequenceAnalysis;
    }

    public RecordSplitter getRecordSplitterType() {
        return recordSplitterType;
    }
    public void setRecordSplitterType(RecordSplitter recordSplitterType) {
        this.recordSplitterType = recordSplitterType;
    }
}