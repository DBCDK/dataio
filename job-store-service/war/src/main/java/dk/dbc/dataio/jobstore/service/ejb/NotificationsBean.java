/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

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
     * @param jsonRequest JSON representation of an {@link AddNotificationRequest} instance
     * @return a HTTP 200 OK response with {@link Notification} entity if notification data is valid,
     *         a HTTP 400 BAD_REQUEST response on invalid json content
     * @throws JobStoreException on internal failure to marshall notification context
     */
    @POST
    @Path(JobStoreServiceConstants.NOTIFICATIONS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response addNotification(String jsonRequest) throws JobStoreException {
        final AddNotificationRequest addNotificationRequest;
        try{
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
