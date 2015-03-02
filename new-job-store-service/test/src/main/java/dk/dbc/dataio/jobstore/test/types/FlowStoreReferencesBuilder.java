package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;

import java.util.HashMap;
import java.util.Map;

public class FlowStoreReferencesBuilder {

    private Map<FlowStoreReferences.Elements, FlowStoreReference> references = createFlowStoreReferences();

    public FlowStoreReferencesBuilder setFlowStoreReferences(Map<FlowStoreReferences.Elements, FlowStoreReference> references) {
        this.references = references;
        return this;
    }

    public FlowStoreReferences build() {
        return new FlowStoreReferences(references);
    }

    private Map<FlowStoreReferences.Elements, FlowStoreReference> createFlowStoreReferences() {
        Map<FlowStoreReferences.Elements, FlowStoreReference> flowStoreReferences = new HashMap<>(4);
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW_BINDER, new FlowStoreReferenceBuilder().setName("FlowBinderName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().setName("FlowName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SINK, new FlowStoreReferenceBuilder().setName("SinkName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SUBMITTER, new FlowStoreReferenceBuilder().setName("SubmitterName").build());
        return flowStoreReferences;
    }
}

