package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.Map;

public class FlowStoreReferences {

    @JsonProperty
    private final Map<Elements, FlowStoreReference> references;


    @JsonCreator
    public FlowStoreReferences(@JsonProperty("references") Map<Elements, FlowStoreReference> references) throws NullPointerException {
        this.references = InvariantUtil.checkNotNullOrThrow(references, "references");
    }

    public enum Elements { FLOW_BINDER, FLOW, SUBMITTER, SINK }


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
