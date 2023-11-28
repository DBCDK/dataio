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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlowTrimmerTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void constructor_jsonbContextArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowTrimmer(null));
    }

    @Test
    public void trim_flowJsonArgDoesNotRepresentJsonObject_throws() throws JSONBException {
        FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        try {
            flowTrimmer.trim("[]");
            Assertions.fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void trim_flowJsonArgDoesNotRepresentFlow_returnsArgUnchanged() throws JSONBException {
        // the "returnsUnchanged" part is not strictly true, since
        // any whitespaces between key/value pairs will actually be stripped.
        final String expectedOutput = "{\"type\":\"NOT_FLOW\"}";
        FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        assertThat(flowTrimmer.trim(expectedOutput), is(expectedOutput));
    }

    @Test
    public void trim_flowJsonArgHasComponentsWithNext_returnsFlowWithNextComponentsRemoved() throws JSONBException {
        FlowContent flowContent = new FlowContentBuilder()
                .setComponents(Arrays.asList(
                        new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next1").build()).build(),
                        new FlowComponentBuilder().setNext(new FlowComponentContentBuilder().setName("next1").build()).build(),
                        new FlowComponentBuilder().setNext(FlowComponent.UNDEFINED_NEXT).build()
                ))
                .build();
        Flow flow = new FlowBuilder()
                .setContent(flowContent)
                .build();

        FlowTrimmer flowTrimmer = new FlowTrimmer(jsonbContext);
        String trimmedFlowJson = flowTrimmer.trim(jsonbContext.marshall(flow));
        Flow trimmedFlow = jsonbContext.unmarshall(trimmedFlowJson, Flow.class);
        List<FlowComponent> components = trimmedFlow.getContent().getComponents();
        assertThat("Number of trimmed components", components.size(), is(3));
        for (FlowComponent flowComponent : components) {
            assertThat("FlowComponent next property", flowComponent.getNext(), is(nullValue()));
        }
    }
}
