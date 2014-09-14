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
    @NamedQuery(name = EsInFlight.FIND_ALL, query = EsInFlight.QUERY_FIND_ALL)
})
public class EsInFlight {
    public static final String FIND_ALL = "EsInFlight.findAll";
    public static final String QUERY_PARAMETER_RESOURCENAME = "resourceName";
    public static final String QUERY_FIND_ALL =
            "SELECT esInFlight FROM EsInFlight esInFlight WHERE esInFlight.resourceName = :"
                    + QUERY_PARAMETER_RESOURCENAME;

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

    @Lob
    @Column(nullable = false)
    private String sinkChunkResult;

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

    public String getSinkChunkResult() {
        return sinkChunkResult;
    }

    public void setSinkChunkResult(String sinkChunkResult) {
        this.sinkChunkResult = sinkChunkResult;
    }
}
