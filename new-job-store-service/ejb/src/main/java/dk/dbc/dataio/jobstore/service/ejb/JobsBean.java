package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Stateless
@Path("/")
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);
    private static final String DUMMY_JOB_ID = "42";

    @EJB
    JSONBBean jsonbBean;

    @EJB
    JobStoreBean jobStoreBean;

    @GET
    public Response iOnlyExistForSanityTest() throws JobStoreException {
        LOGGER.debug("some debug information");
        jobStoreBean.testAddJob();
        return Response.ok().build();
    }

    /**
     * Adds new job based on POSTed job input stream, and persists it in the underlying data store
     *
     * @param uriInfo application and request URI information
     * @param jobInputStreamData job input stream data as json
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found,
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to add job
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addJob(@Context UriInfo uriInfo, String jobInputStreamData) throws JSONBException, JobStoreException {
        LOGGER.trace("JobInputStream: {}", jobInputStreamData);
        final JobInputStream jobInputStream;
        JobInfoSnapshot jobInfoSnapshot;

        try {
            jobInputStream = jsonbBean.getContext().unmarshall(jobInputStreamData, JobInputStream.class);
            jobInfoSnapshot = jobStoreBean.addAndScheduleJob(jobInputStream);
            return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.getJobId())))
                    .entity(jsonbBean.getContext().marshall(jobInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbBean.getContext().marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        } catch(InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbBean.getContext().marshall(e.getJobError()))
                    .build();
        }
    }

    /**
     * Adds chunk with type: PROCESSED (updates existing job by adding external chunk)
     *
     * @param uriInfo application and request URI information
     * @param externalChunkData chunk data as json
     * @param jobId job id
     * @param chunkId chunk id
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on illegal number of items (not matching that of the internal chunk entity),
     *         a HTTP 400 BAD_REQUEST response on referenced items not found
     *         a HTTP 400 BAD_REQUEST response on failure to update item entities
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to update job
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_CHUNK_PROCESSED)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addChunkProcessed(@Context UriInfo uriInfo, String externalChunkData,
                             @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) long jobId,
                             @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) long chunkId)
            throws JSONBException, JobStoreException {
        return addChunk(uriInfo, jobId, chunkId, ExternalChunk.Type.PROCESSED, externalChunkData);
    }


    /**
     * Adds chunk with type: DELIVERED (updates existing job by adding external chunk)
     *
     * @param uriInfo application and request URI information
     * @param externalChunkData chunk data as json
     * @param jobId job id
     * @param chunkId chunk id
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on illegal number of items (not matching that of the internal chunk entity),
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found
     *         a HTTP 400 BAD_REQUEST response on failure to update item entities
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to update job
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_CHUNK_PROCESSED)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addChunkDelivered(@Context UriInfo uriInfo, String externalChunkData,
                                      @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) long jobId,
                                      @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) long chunkId)
            throws JSONBException, JobStoreException {
        return addChunk(uriInfo, jobId, chunkId, ExternalChunk.Type.DELIVERED, externalChunkData);
    }


    /**
     * @param uriInfo application and request URI information
     * @param jobId job id
     * @param chunkId chunk id
     * @param type external chunk type (PARTITIONED, PROCESSED, DELIVERED)
     * @param externalChunkData chunk data as json
     *
     * @return HTTP 201 CREATED response on success, HTTP 400 BAD_REQUEST response on failure to update job
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on referenced entities not found
     */
    private Response addChunk(UriInfo uriInfo, long jobId, long chunkId, ExternalChunk.Type type, String externalChunkData)
            throws JobStoreException, JSONBException {

        final ExternalChunk chunk;
        try {
            chunk = jsonbBean.getContext().unmarshall(externalChunkData, ExternalChunk.class);
        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbBean.getContext().marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }

        try {
            JobError jobError = getChunkInputDataError(jobId, chunkId, chunk, type);
            if(jobError == null) {
                JobInfoSnapshot jobInfoSnapshot = jobStoreBean.addChunk(chunk);
                return Response.created(getUri(uriInfo, Long.toString(chunk.getChunkId())))
                        .entity(jsonbBean.getContext().marshall(jobInfoSnapshot))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(jsonbBean.getContext().marshall(jobError))
                        .build();
            }

        } catch(InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbBean.getContext().marshall(e.getJobError()))
                    .build();
        }
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }

    /**
     *
     * @param jobId job id
     * @param chunkId chunk id
     * @param chunk external chunk
     * @param type external chunk type
     * @return jobError containing the relevant error message, null if input data is valid
     */
    private JobError getChunkInputDataError(long jobId, long chunkId, ExternalChunk chunk, ExternalChunk.Type type) {
        JobError jobError = null;
        if(jobId != chunk.getJobId()) {
            jobError = new JobError(
                    JobError.Code.INVALID_JOB_ID,
                    String.format("jobId: %s did not match ExternalChunk.jobId: %s", jobId, chunk.getJobId()), null);
        } else if(chunkId != chunk.getChunkId()) {
            jobError = new JobError(
                    JobError.Code.INVALID_CHUNK_ID,
                    String.format("chunkId: %s did not match ExternalChunk.chunkId: %s", chunkId, chunk.getChunkId()), null);
        } else if(type != chunk.getType()) {
            jobError = new JobError(
                        JobError.Code.INVALID_CHUNK_TYPE,
                        String.format("type: %s did not match ExternalChunk.type: %s", type, chunk.getType()), null);
        }
        return jobError;
    }

}
