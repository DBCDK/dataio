package dk.dbc.dataio.sink.es.entity.inflight;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@IdClass(value = EsInFlightPK.class)
@NamedQueries({
    @NamedQuery(name = EsInFlight.FIND_ALL, query = EsInFlight.QUERY_FIND_ALL)
})
public class EsInFlight {
    public static final String FIND_ALL = "EsInFlight.findAll";
    public static final String QUERY_PARAMETER_SINKID = "sinkId";
    public static final String QUERY_FIND_ALL =
            "SELECT esInFlight FROM EsInFlight esInFlight WHERE esInFlight.sinkId = :"
                    + QUERY_PARAMETER_SINKID;

    @Id
    private Long sinkId;

    @Id
    private Integer targetReference;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long chunkId;

    @Column(nullable = false)
    private String chunk;

    @Column(nullable = false)
    private String databaseName;

    @Column(nullable = false)
    private int redelivered;

    public Long getSinkId() {
        return sinkId;
    }

    public void setSinkId(Long sinkId) {
        this.sinkId = sinkId;
    }

    public Integer getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(Integer targetReference) {
        this.targetReference = targetReference;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getIncompleteDeliveredChunk() {
        return chunk;
    }

    public void setIncompleteDeliveredChunk(String incompleteDeliveredChunk) {
        this.chunk = incompleteDeliveredChunk;
    }

    public int getRedelivered() {
        return redelivered;
    }

    public void setRedelivered(int redelivered) {
        this.redelivered = redelivered;
    }
}
