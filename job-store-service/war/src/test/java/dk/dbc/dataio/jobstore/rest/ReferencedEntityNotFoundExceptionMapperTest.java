package dk.dbc.dataio.jobstore.rest;

import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
  * ReferencedEntityNotFoundExceptionMapper unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class ReferencedEntityNotFoundExceptionMapperTest {
    @Test
    public void toResponse_argIsReferencedEntityNotFoundException_returnsStatusPreconditionFailedResponse() {
        final ReferencedEntityNotFoundExceptionMapper instance = new ReferencedEntityNotFoundExceptionMapper();
        final Response response = instance.toResponse(new ReferencedEntityNotFoundException("die"));
        assertThat(response.getStatus(), is(Response.Status.PRECONDITION_FAILED.getStatusCode()));
    }
}
