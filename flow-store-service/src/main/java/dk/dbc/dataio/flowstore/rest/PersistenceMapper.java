package dk.dbc.dataio.flowstore.rest;

import org.json.simple.JSONObject;

import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceMapper implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException e) {
        String message = JSONObject.escape(e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(String.format("{\"message\": \"%s\"}", message)).type(MediaType.APPLICATION_JSON).build();
    }

}
