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

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

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
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Path("/")
public class NotificationsBean {
    private final JSONBContext jsonbContext = new JSONBContext();

    @EJB
    JobNotificationRepository jobNotificationRepository;

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

    /**
     * Adds a Notification for later processing
     * @param jsonRequest JSON representation of an {@link AddNotificationRequest} instance
     * @return a HTTP 200 OK response with {@link JobNotification} entity if notification data is valid,
     *         a HTTP 400 BAD_REQUEST response on invalid json content
     * @throws JSONBException on marshalling failure
     */
    @POST
    @Path(JobStoreServiceConstants.NOTIFICATIONS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response addNotification(String jsonRequest) throws JSONBException {
        final AddNotificationRequest addNotificationRequest;
        try{
            addNotificationRequest = jsonbContext.unmarshall(jsonRequest, AddNotificationRequest.class);
        } catch (JSONBException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
        final String notificationContext = jsonbContext.marshall(
                addNotificationRequest.getIncompleteTransfileNotificationContext());
        final JobNotification jobNotification = jobNotificationRepository.addNotification(
                addNotificationRequest.getNotificationType(), addNotificationRequest.getDestinationEmail(), notificationContext)
                .toJobNotification();
        return Response.ok().entity(jsonbContext.marshall(jobNotification)).build();
    }
}
