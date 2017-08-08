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

package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@value dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants#ITEM_LOG_ENTRY_COLLECTION}' entry point
 */
@Stateless
@Path("/")
public class LogEntriesBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogEntriesBean.class);

    @EJB
    LogStoreBean logStoreBean;

    /**
     * Retrieves log for given item in given chunk in given job
     * @param jobId ID of job
     * @param chunkId ID of chunk in job
     * @param itemId ID of item in chunk
     * @return a HTTP 200 OK response with log entries as text
     *         a HTTP 404 NOT_FOUND response in case no log entries could be found
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @Path(LogStoreServiceConstants.ITEM_LOG_ENTRY_COLLECTION)
    @Produces(MediaType.TEXT_PLAIN)
    @Stopwatch
    public Response getItemLog(
            @PathParam(LogStoreServiceConstants.JOB_ID_VARIABLE) final String jobId,
            @PathParam(LogStoreServiceConstants.CHUNK_ID_VARIABLE) final Long chunkId,
            @PathParam(LogStoreServiceConstants.ITEM_ID_VARIABLE) final Long itemId) {
        LOGGER.trace("getItemLog() method called with path {}/{}/{}", jobId, chunkId, itemId);

        final String logEntity = logStoreBean.getItemLog(jobId, chunkId, itemId);
        if (logEntity.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(logEntity).build();
    }

    @DELETE
    @Path(LogStoreServiceConstants.JOB_LOG_ENTRY_COLLECTION)
    public Response deleteJobLog(@PathParam(LogStoreServiceConstants.JOB_ID_VARIABLE) String jobId) {
        LOGGER.trace("deleteJobLog() method called with jobId {}", jobId);
        final int deletedRows = logStoreBean.deleteJobLog(jobId);
        LOGGER.trace("deleted {} logItemEntries for job {}", deletedRows, jobId);
        return Response.noContent().build();
    }
}
