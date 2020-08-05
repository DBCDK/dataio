/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource for job exports
 * exposed by the /jobs/{jobId}/exports endpoint
 */
@Stateless
@Path("/")
public class JobsExportsBean {
    @EJB PgJobStoreRepository jobStoreRepository;
    @Inject FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    /**
     * Exports all successfully partitioned chunk items for given job to file in file-store
     * @param jobId ID of the job from which to export items
     * @return a HTTP 303 See Other response redirecting to the generated export file in file-store,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED)
    @Stopwatch
    public Response exportItemsPartitioned(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.PARTITIONING);
    }

    /**
     * Exports all successfully processed chunk items for given job to file in file-store
     * @param jobId ID of the job from which to export items
     * @return a HTTP 303 See Other response redirecting to the generated export file in file-store,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED)
    @Stopwatch
    public Response exportItemsProcessed(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.PROCESSING);
    }

    /**
     * Exports all successfully delivered chunk items for given job to file in file-store
     * @param jobId ID of the job from which to export items
     * @return a HTTP 303 See Other response redirecting to the generated export file in file-store,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED)
    @Stopwatch
    public Response exportItemsDelivered(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.DELIVERING);
    }

    private Response exportItemsFromPhase(int jobId, State.Phase phase) throws URISyntaxException, JobStoreException {
        try {
            return Response.seeOther(new URI(jobStoreRepository.exportItemsToFileStore(
                    jobId, phase, fileStoreServiceConnectorBean.getConnector()))).build();
        } catch (JobStoreException | URISyntaxException e) {
            if (!jobStoreRepository.jobExists(jobId)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            throw e;
        }
    }
}
