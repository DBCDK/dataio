package dk.dbc.dataio.commons.utils.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Interface for service status resources.
 * <p>
 * Services can simply have a bean implement this interface for default 200 OK /status and /howru responses,
 * or they can override the getStatus() method when a more elaborate approach is required.
 * </p>
 */
public interface ServiceStatus {
    String OK_ENTITY = new HowRU().toJson();

    @GET
    @Path("status")
    @Produces({MediaType.APPLICATION_JSON})
    default Response getStatus() throws Throwable {
        return Response.ok().entity(OK_ENTITY).build();
    }

    @GET
    @Path("howru")
    @Produces({MediaType.APPLICATION_JSON})
    default Response howru() {
        try {
            return this.getStatus();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new HowRU().withException(e).toJson())
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new HowRU())
                    .build();
        }
    }
}
