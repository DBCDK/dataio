/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

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
     * @throws URISyntaxException on invalid redirect URL
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED)
    @Stopwatch
    public Response exportItemsPartitioned(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.PARTITIONING);
    }

    /**
     * Exports all chunk items failed during partitioning for given job
     * @param jobId ID of the job from which to export items
     * @param format type of exported data, eg. ChunkItem.Type.BYTES,
     *               (for certain types on-the-fly conversion is possible)
     * @return a HTTP 200 OK response with item data as stream,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED_FAILED)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedDuringPartitioning(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) throws JobStoreException {
        return exportFailedItemsFromPhase(jobId, State.Phase.PARTITIONING, format);
    }

    /**
     * Exports all successfully processed chunk items for given job to file in file-store
     * @param jobId ID of the job from which to export items
     * @return a HTTP 303 See Other response redirecting to the generated export file in file-store,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     * @throws URISyntaxException on invalid redirect URL
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED)
    @Stopwatch
    public Response exportItemsProcessed(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.PROCESSING);
    }

    /**
     * Exports all chunk items failed during processing for given job
     * @param jobId ID of the job from which to export items
     * @param format type of exported data, eg. ChunkItem.Type.BYTES,
     *               (for certain types on-the-fly conversion is possible)
     * @return a HTTP 200 OK response with item data as stream,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED_FAILED)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedDuringProcessing(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) throws JobStoreException {
        return exportFailedItemsFromPhase(jobId, State.Phase.PROCESSING, format);
    }

    /**
     * Exports all successfully delivered chunk items for given job to file in file-store
     * @param jobId ID of the job from which to export items
     * @return a HTTP 303 See Other response redirecting to the generated export file in file-store,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     * @throws URISyntaxException on invalid redirect URL
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED)
    @Stopwatch
    public Response exportItemsDelivered(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId)
            throws URISyntaxException, JobStoreException {
        return exportItemsFromPhase(jobId, State.Phase.DELIVERING);
    }

    /**
     * Exports all chunk items failed during delivery for given job
     * @param jobId ID of the job from which to export items
     * @param format type of exported data, eg. ChunkItem.Type.BYTES,
     *               (for certain types on-the-fly conversion is possible)
     * @return a HTTP 200 OK response with item data as stream,
     *         a HTTP 204 No Content response if the specified job does not exist,
     *         a HTTP 500 Internal Server Error on failure to generate an export
     * @throws JobStoreException on failure to to generate an export
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED_FAILED)
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedDuringDelivery(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) throws JobStoreException {
        return exportFailedItemsFromPhase(jobId, State.Phase.DELIVERING, format);
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

    private Response exportFailedItemsFromPhase(int jobId, State.Phase phase, ChunkItem.Type format)
            throws JobStoreException {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = jobStoreRepository.exportFailedItems(
                    jobId, phase, format, StandardCharsets.UTF_8);
            final StreamingOutput streamingOutput = os -> os.write(byteArrayOutputStream.toByteArray());
            return Response.ok(streamingOutput).build();
        } catch (JobStoreException e) {
            if (!jobStoreRepository.jobExists(jobId)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            throw e;
        }
    }
}
