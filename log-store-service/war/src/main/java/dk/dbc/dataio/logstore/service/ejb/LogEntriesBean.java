package dk.dbc.dataio.logstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     *
     * @param jobId   ID of job
     * @param chunkId ID of chunk in job
     * @param itemId  ID of item in chunk
     * @return a HTTP 200 OK response with log entries as text
     * a HTTP 404 NOT_FOUND response in case no log entries could be found
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
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
