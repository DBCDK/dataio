package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class FlowStoreReferencesTest {

    @Test
    public void constructor_noArgs_returnsNewInstance() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        assertThat(flowStoreReferences, is(notNullValue()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.SUBMITTER), is(nullValue()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW), is(nullValue()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER), is(nullValue()));
        assertThat(flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK), is(nullValue()));
    }

    @Test
    public void setReference_flowBinderReference_flowBinderReferenceSet() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowStoreReference = new FlowStoreReference(1, 2, "FlowBinderName");
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, flowStoreReference);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        assertThat(flowBinderReference, is(notNullValue()));
        assertThat(flowBinderReference, is(flowStoreReference));
    }

    @Test
    public void setReference_flowReference_flowReferenceSet() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowStoreReference = new FlowStoreReference(1, 2, "FlowName");
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, flowStoreReference);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW);
        assertThat(flowBinderReference, is(notNullValue()));
        assertThat(flowBinderReference, is(flowStoreReference));
    }

    @Test
    public void setReference_submitterReference_submitterReferenceSet() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowStoreReference = new FlowStoreReference(1, 2, "submitterName");
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, flowStoreReference);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SUBMITTER);
        assertThat(flowBinderReference, is(notNullValue()));
        assertThat(flowBinderReference, is(flowStoreReference));
    }

    @Test
    public void setReference_sinkReference_sinkReferenceSet() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        FlowStoreReference flowStoreReference = new FlowStoreReference(1, 2, "SinkName");
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, flowStoreReference);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        assertThat(flowBinderReference, is(notNullValue()));
        assertThat(flowBinderReference, is(flowStoreReference));
    }


    @Test
    public void setReference_sinkReference_withSinkReferenceSet() {
        FlowStoreReference flowStoreReference = new FlowStoreReference(1, 2, "SinkName");
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences().withReference(FlowStoreReferences.Elements.SINK, flowStoreReference);
        FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        assertThat(flowBinderReference, is(notNullValue()));
        assertThat(flowBinderReference, is(flowStoreReference));
    }
}
