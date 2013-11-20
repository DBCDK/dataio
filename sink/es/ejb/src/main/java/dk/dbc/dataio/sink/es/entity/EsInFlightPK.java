package dk.dbc.dataio.sink.es.entity;

public class EsInFlightPK {
    private String resourceName;
    private Integer targetReference;

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Integer getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(Integer targetReference) {
        this.targetReference = targetReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EsInFlightPK that = (EsInFlightPK) o;

        if (resourceName != null ? !resourceName.equals(that.resourceName) : that.resourceName != null) {
            return false;
        }
        if (targetReference != null ? !targetReference.equals(that.targetReference) : that.targetReference != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = resourceName != null ? resourceName.hashCode() : 0;
        result = 31 * result + (targetReference != null ? targetReference.hashCode() : 0);
        return result;
    }
}
