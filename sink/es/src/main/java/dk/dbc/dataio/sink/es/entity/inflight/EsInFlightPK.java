package dk.dbc.dataio.sink.es.entity.inflight;

public class EsInFlightPK {
    private Long sinkId;
    private Integer targetReference;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EsInFlightPK)) return false;

        EsInFlightPK that = (EsInFlightPK) o;

        if (sinkId != null ? !sinkId.equals(that.sinkId) : that.sinkId != null) return false;
        return targetReference != null ? targetReference.equals(that.targetReference) : that.targetReference == null;

    }

    @Override
    public int hashCode() {
        int result = sinkId != null ? sinkId.hashCode() : 0;
        result = 31 * result + (targetReference != null ? targetReference.hashCode() : 0);
        return result;
    }
}
