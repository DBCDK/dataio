package dk.dbc.dataio.jobstore.rest;

import dk.dbc.dataio.commons.utils.json.JsonException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * JsonExceptionMapper unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class JsonExceptionMapperTest {
    @Test
    public void toResponse_argIsJsonException_returnsStatusBadRequestResponse() {
        final JsonExceptionMapper instance = new JsonExceptionMapper();
        final Response response = instance.toResponse(new JsonException("die"));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }
}
