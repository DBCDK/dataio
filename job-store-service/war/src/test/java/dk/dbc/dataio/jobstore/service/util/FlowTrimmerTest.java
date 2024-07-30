package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
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
}
