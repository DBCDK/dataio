package dk.dbc.dataio.jobstore.service.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import dk.dbc.dataio.jobstore.types.Notification;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "notification")
@NamedQueries(
        @NamedQuery(name = NotificationEntity.SELECT_BY_TYPE,
                query = "SELECT notification FROM NotificationEntity notification WHERE notification.type = :type ORDER BY notification.timeOfCreation DESC")
)
public class NotificationEntity {
    public static final String SELECT_BY_TYPE = "NotificationEntity.byType";

    @Id
    @SequenceGenerator(
            name = "notification_id_seq",
            sequenceName = "notification_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "notification_id_seq")
    @Column(updatable = false)
    private Integer id;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;

    @Convert(converter = NotificationTypeConverter.class)
    private Notification.Type type;

    @Convert(converter = NotificationStatusConverter.class)
    private Notification.Status status;

    @Lob
    private String statusMessage;

    @Lob
    private String destination;

    @Lob
    private String content;

    @Lob
    @JsonRawValue
    private String context;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job")
    private JobEntity job;

    private Integer jobId;

    public NotificationEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public Notification.Type getType() {
        return type;
    }

    public void setType(Notification.Type type) {
        this.type = type;
    }

    public Notification.Status getStatus() {
        return status;
    }

    public void setStatus(Notification.Status status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public JobEntity getJob() {
        return job;
    }

    public void setJob(JobEntity job) {
        this.job = job;
        this.jobId = job.getId();
    }

    public Integer getJobId() {
        return jobId;
    }
}
