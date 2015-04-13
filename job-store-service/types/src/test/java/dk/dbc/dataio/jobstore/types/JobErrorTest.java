package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JobErrorTest {
    private static final JobError.Code CODE = JobError.Code.INVALID_DATA;
    private static final String DESCRIPTION = "description";
    private static final String STACKTRACE = "stacktrace";

    @Test(expected = NullPointerException.class)
    public void constructor_codeArgIsNull_throws() {
        new JobError(null, DESCRIPTION, STACKTRACE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new JobError(CODE, null, STACKTRACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new JobError(CODE, "", STACKTRACE);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JobError jobError = new JobError(CODE, DESCRIPTION, STACKTRACE);
        assertThat(jobError, is(notNullValue()));
        assertThat(jobError.getCode(), is(CODE));
        assertThat(jobError.getDescription(), is(DESCRIPTION));
        assertThat(jobError.getStacktrace(), is(STACKTRACE));
    }

    @Test
    public void constructor_stacktraceIsNull_returnsNewInstanceWithEmptyStacktrace() {
        final JobError jobError = new JobError(CODE, DESCRIPTION, null);
        assertThat(jobError, is(notNullValue()));
        assertThat(jobError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_stacktraceIsEmpty_returnsNewInstanceWithEmptyStacktrace() {
        final JobError jobError = new JobError(CODE, DESCRIPTION, "");
        assertThat(jobError, is(notNullValue()));
        assertThat(jobError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void jsonBinding_marshallingFollowedByUnmarshalling_returnsNewInstanceWithMatchingFields() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JobError jobError = new JobError(CODE, DESCRIPTION, STACKTRACE);
        final String marshalled = jsonbContext.marshall(jobError);
        final JobError unmarshalled = jsonbContext.unmarshall(marshalled, JobError.class);
        assertThat(unmarshalled, is(notNullValue()));
        assertThat(unmarshalled.getCode(), is(jobError.getCode()));
        assertThat(unmarshalled.getDescription(), is(jobError.getDescription()));
        assertThat(unmarshalled.getStacktrace(), is(jobError.getStacktrace()));
    }
}