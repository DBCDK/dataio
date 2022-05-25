package dk.dbc.dataio.jobstore.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobErrorTest {
    @Test
    public void jsonBinding_marshallingFollowedByUnmarshalling_returnsNewInstanceWithMatchingFields() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JobError jobError = new JobError(JobError.Code.INVALID_DATA, "description", "stacktrace");
        final String marshalled = jsonbContext.marshall(jobError);
        final JobError unmarshalled = jsonbContext.unmarshall(marshalled, JobError.class);
        assertThat(unmarshalled, is(jobError));
    }
}
