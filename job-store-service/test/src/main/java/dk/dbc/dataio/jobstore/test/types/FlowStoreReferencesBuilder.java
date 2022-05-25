package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;

import java.util.HashMap;
import java.util.Map;

public class FlowStoreReferencesBuilder {

    private final Map<FlowStoreReferences.Elements, FlowStoreReference> references = createReferences();

    public FlowStoreReferencesBuilder setFlowStoreReference(FlowStoreReferences.Elements element, FlowStoreReference flowStoreReference) {
        this.references.put(element, flowStoreReference);
        return this;
    }

    public FlowStoreReferences build() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, references.get(FlowStoreReferences.Elements.FLOW_BINDER));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, references.get(FlowStoreReferences.Elements.FLOW));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, references.get(FlowStoreReferences.Elements.SINK));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, references.get(FlowStoreReferences.Elements.SUBMITTER));
        return flowStoreReferences;
    }

    private Map<FlowStoreReferences.Elements, FlowStoreReference> createReferences() {
        Map<FlowStoreReferences.Elements, FlowStoreReference> flowStoreReferences = new HashMap<>(4);
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW_BINDER, new FlowStoreReferenceBuilder().setName("FlowBinderName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().setName("FlowName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SINK, new FlowStoreReferenceBuilder().setName("SinkName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SUBMITTER, new FlowStoreReferenceBuilder().setName("SubmitterName").build());
        return flowStoreReferences;
    }
}

