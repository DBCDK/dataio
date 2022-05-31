package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class FlowBinderIdent implements Serializable {
    private Long flowBinderId;
    private String flowBinderName;

    @JsonCreator
    public FlowBinderIdent(
            @JsonProperty("flowBinderName") String flowBinderName,
            @JsonProperty("flowBinderId") Long flowBinderId) {
        this.flowBinderName = flowBinderName;
        this.flowBinderId = flowBinderId;
    }

    // For GWT serialization
    private FlowBinderIdent() {
    }

    public String getFlowBinderName() {
        return flowBinderName;
    }

    public Long getFlowBinderId() {
        return flowBinderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowBinderIdent that = (FlowBinderIdent) o;
        return Objects.equals(flowBinderId, that.flowBinderId) &&
                Objects.equals(flowBinderName, that.flowBinderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowBinderId, flowBinderName);
    }

    @Override
    public String toString() {
        return "FlowBinderWithSubmitter{" +
                "flowBinderId=" + flowBinderId +
                ", flowBinderName='" + flowBinderName + '\'' +
                '}';
    }
}
