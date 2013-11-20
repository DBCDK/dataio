package dk.dbc.dataio.sink.es.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@IdClass(value = EsInFlightPK.class)
@NamedQueries({
    @NamedQuery(name = EsInFlight.QUERY_FIND_ALL, query = "SELECT esInFlight FROM EsInFlight esInFlight")
})
public class EsInFlight {
    public static final String QUERY_FIND_ALL = "EsInFlight.findAll";

    @Id
    @Lob
    private String resourceName;

    @Id
    private Integer targetReference;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long chunkId;

    @Column(nullable = false)
    private Integer recordSlots;

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getRecordSlots() {
        return recordSlots;
    }

    public void setRecordSlots(Integer recordSlots) {
        this.recordSlots = recordSlots;
    }

    public Integer getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(Integer targetReference) {
        this.targetReference = targetReference;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
