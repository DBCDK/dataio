package dk.dbc.dataio.jobstore.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "rerun")
@NamedQueries({
        @NamedQuery(name = RerunEntity.FIND_HEAD_QUERY_NAME,
                query = "SELECT rerun FROM RerunEntity rerun ORDER BY rerun.id ASC"),

        @NamedQuery(name = RerunEntity.FIND_BY_STATE_QUERY_NAME,
                query = "SELECT rerun FROM RerunEntity rerun WHERE rerun.state = :" + RerunEntity.FIELD_STATE),
})
public class RerunEntity {
    public static final String FIND_HEAD_QUERY_NAME = "RerunEntity.findHead";
    public static final String FIND_BY_STATE_QUERY_NAME = "RerunEntity.findByState";
    public static final String FIELD_STATE = "state";

    public enum State {IN_PROGRESS, WAITING}

    @Id
    @SequenceGenerator(
            name = "rerun_id_seq",
            sequenceName = "rerun_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rerun_id_seq")
    @Column(updatable = false)
    private int id;

    @Convert(converter = RerunStateConverter.class)
    private State state;

    @Column(updatable = false)
    private Timestamp timeOfCreation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jobId", updatable = false)
    private JobEntity job;

    private Boolean includeFailedOnly;

    public RerunEntity() {
    }

    public int getId() {
        return id;
    }

    public RerunEntity withId(int id) {
        this.id = id;
        return this;
    }

    public JobEntity getJob() {
        return job;
    }

    public RerunEntity withJob(JobEntity job) {
        this.job = job;
        return this;
    }

    public State getState() {
        return state;
    }

    public RerunEntity withState(State state) {
        this.state = state;
        return this;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Boolean isIncludeFailedOnly() {
        return includeFailedOnly != null && includeFailedOnly;
    }

    public RerunEntity withIncludeFailedOnly(Boolean includeFailedOnly) {
        this.includeFailedOnly = includeFailedOnly;
        return this;
    }
}
