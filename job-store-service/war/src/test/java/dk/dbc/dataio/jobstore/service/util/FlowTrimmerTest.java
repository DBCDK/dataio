package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class FlowTrimmerTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test(expected = NullPointerException.class)
    public void constructor_jsonbContextArgIsNull_throws() {
        new FlowTrimmer(null);
    }

    @Test
    public void trim_flowJsonArgDoesNotRepresentJsonObject_throws() throws JSONBException {
        final FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        try {
            flowTrimmer.trim("[]");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void trim_flowJsonArgDoesNotRepresentFlow_returnsArgUnchanged() throws JSONBException {
        // the "returnsUnchanged" part is not strictly true, since
        // any whitespaces between key/value pairs will actually be stripped.
        final String expectedOutput = "{\"type\":\"NOT_FLOW\"}";
        final FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        assertThat(flowTrimmer.trim(expectedOutput), is(expectedOutput));
    }

    @Test
    public void trim_flowJsonArgHasComponentsWithNext_returnsFlowWithNextComponentsRemoved() throws JSONBException {
        final FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Arrays.asList(
                        new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next1").build()).build(),
                        new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next1").build()).build(),
                        new FlowComponentBuilder().setNext(FlowComponent.UNDEFINED_NEXT).build()
                ))
                .build();
        final Flow flow = new FlowBuilder()
                .setContent(flowContent)
                .build();

        final FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        final String trimmedFlowJson = flowTrimmer.trim(jsonbContext.marshall(flow));
        final Flow trimmedFlow = jsonbContext.unmarshall(trimmedFlowJson, Flow.class);
        final List<FlowComponent> components = trimmedFlow.getContent().getComponents();
        assertThat("Number of trimmed components", components.size(), is(3));
        for (FlowComponent flowComponent : components) {
            assertThat("FlowComponent next property", flowComponent.getNext(), is(nullValue()));
        }
    }
}
