package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class FlowStoreReferences {

    @JsonProperty
    private final Map<Elements, FlowStoreReference> references;

    @JsonCreator
    public FlowStoreReferences() {
        this.references = new HashMap<>(Elements.values().length);
    }

    public enum Elements {FLOW_BINDER, FLOW, SUBMITTER, SINK}

    /**
     * Sets the specified reference to a flow store element
     *
     * @param element            the flow store element
     * @param flowStoreReference the flow store reference
     */
    public void setReference(FlowStoreReferences.Elements element, FlowStoreReference flowStoreReference) {
        references.put(element, flowStoreReference);
    }

    public FlowStoreReferences withReference(FlowStoreReferences.Elements element, FlowStoreReference flowStoreReference) {
        this.setReference(element, flowStoreReference);
        return this;
    }


    /**
     * Retrieves the specified flowStoreReference from references
     *
     * @param elements (flow binder, flow, submitter, sink)
     * @return the flow store reference for the specified reference
     */
    public FlowStoreReference getReference(Elements elements) {
        return references.get(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStoreReferences)) return false;

        FlowStoreReferences that = (FlowStoreReferences) o;

        if (!references.equals(that.references)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return references.hashCode();
    }
}
