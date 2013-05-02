package dk.dbc.dataio.flowstore.rest;

import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException e) {
        //String message = JSONObject.escape(e.getMessage());
        //return Response.status(Response.Status.BAD_REQUEST).entity(String.format("{\"message\": \"%s\"}", message)).type(MediaType.APPLICATION_JSON).build();
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }

}
