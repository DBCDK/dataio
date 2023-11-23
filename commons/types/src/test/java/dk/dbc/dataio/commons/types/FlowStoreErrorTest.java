package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlowStoreErrorTest {
    private static final FlowStoreError.Code CODE = FlowStoreError.Code.NONEXISTING_SUBMITTER;
    private static final String DESCRIPTION = "description";
    private static final String STACKTRACE = "stacktrace";

    @Test
    public void constructor_codeArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowStoreError(null, DESCRIPTION, STACKTRACE));
    }

    @Test
    public void constructor_descriptionArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FlowStoreError(CODE, null, STACKTRACE));
    }

    @Test
    public void constructor_descriptionArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FlowStoreError(CODE, "", STACKTRACE));
    }

    @Test
    public void constructor_stacktraceIsNull_returnsNewInstanceWithEmptyStacktrace() {
        FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, null);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_stacktraceIsEmpty_returnsNewInstanceWithEmptyStacktrace() {
        FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, "");
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, STACKTRACE);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getCode(), is(CODE));
        assertThat(FlowStoreError.getDescription(), is(DESCRIPTION));
        assertThat(FlowStoreError.getStacktrace(), is(STACKTRACE));
    }

    @Test
    public void jsonBinding_marshallingFollowedByUnmarshalling_returnsNewInstanceWithMatchingFields() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, STACKTRACE);
        String marshalled = jsonbContext.marshall(FlowStoreError);
        FlowStoreError unmarshalled = jsonbContext.unmarshall(marshalled, FlowStoreError.class);
        assertThat(unmarshalled, is(notNullValue()));
        assertThat(unmarshalled.getCode(), is(FlowStoreError.getCode()));
        assertThat(unmarshalled.getDescription(), is(FlowStoreError.getDescription()));
        assertThat(unmarshalled.getStacktrace(), is(FlowStoreError.getStacktrace()));
    }

}
