package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#NOTIFICATIONS} entry point
 */
@Path("/")
public class NotificationsBean {
    private final JSONBContext jsonbContext = new JSONBContext();

    @EJB
    JobNotificationRepository jobNotificationRepository;

    /**
     * Adds a Notification for later processing
     *
     * @param jsonRequest JSON representation of an {@link AddNotificationRequest} instance
     * @return a HTTP 200 OK response with {@link Notification} entity if notification data is valid,
     * a HTTP 400 BAD_REQUEST response on invalid json content
     * @throws JobStoreException on internal failure to marshall notification context
     */
    @POST
    @Path(JobStoreServiceConstants.NOTIFICATIONS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response addNotification(String jsonRequest) throws JobStoreException {
        final AddNotificationRequest addNotificationRequest;
        try {
            addNotificationRequest = jsonbContext.unmarshall(jsonRequest, AddNotificationRequest.class);
        } catch (JSONBException e) {
            return Response.status(BAD_REQUEST)
                    .entity(marshall(new JobError(JobError.Code.INVALID_JSON,
                            e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
        final NotificationEntity jobNotification = jobNotificationRepository.addNotification(
                addNotificationRequest.getNotificationType(),
                addNotificationRequest.getDestinationEmail(),
                addNotificationRequest.getContext());
        return Response.ok().entity(marshall(jobNotification)).build();
    }

    /**
     * Lists notifications of type INVALID_TRANSFILE
     *
     * @return a HTTP 200 OK response with list of
     * {@link dk.dbc.dataio.jobstore.service.entity.NotificationEntity} entity
     * @throws JobStoreException on internal failure to marshall notifications
     */
    @GET
    @Path(JobStoreServiceConstants.NOTIFICATIONS_TYPES_INVALID_TRNS)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getInvalidTransfileNotifications() throws JobStoreException {
        return Response.ok()
                .entity(marshall(jobNotificationRepository.getNotificationsByType(
                        Notification.Type.INVALID_TRANSFILE)))
                .build();
    }

    private String marshall(Object object) throws JobStoreException {
        try {
            return jsonbContext.marshall(object);
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to marshall object", e);
        }
    }
}
