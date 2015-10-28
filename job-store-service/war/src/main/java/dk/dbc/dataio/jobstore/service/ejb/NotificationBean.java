package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Path("/")
public class NotificationBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationBean.class);

    @EJB
    private JobNotificationRepository jobNotificationRepository;

    /**
     * This is a dummy service for TEST purposes.
     * @return always OK
     */
    @GET
    @Path(JobStoreServiceConstants.NOTIFICATIONS_TEST)
    @Produces(MediaType.APPLICATION_JSON)
    @Stopwatch
    public Response testThis() {
        return Response.ok().entity("Hi from NotificationBean").build();
    }

    @POST
    @Path(JobStoreServiceConstants.NOTIFICATIONS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response addNotification(AddNotificationRequest addNotificationRequest) {
        return Response.ok().build();
    }
}
