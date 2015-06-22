package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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
import java.util.List;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Stateless
@Path("/")
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    JSONBContext jsonbContext = new JSONBContext();

    @EJB
    PgJobStore jobStore;

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
            jobInputStream = jsonbContext.unmarshall(jobInputStreamData, JobInputStream.class);
            jobInfoSnapshot = jobStore.addAndScheduleJob(jobInputStream);
            return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.getJobId())))
                    .entity(jsonbContext.marshall(jobInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        } catch(InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(e.getJobError()))
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
     *         a HTTP 202 ACCEPTED response when attempting to add already existing chunk
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on illegal number of items (not matching that of the internal chunk entity),
     *         a HTTP 400 BAD_REQUEST response on referenced items not found,
     *         a HTTP 400 BAD_REQUEST response on failure to update item entities,
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
     *         a HTTP 202 ACCEPTED response when attempting to add already existing chunk
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on illegal number of items (not matching that of the internal chunk entity),
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found,
     *         a HTTP 400 BAD_REQUEST response on failure to update item entities,
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to update job
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_CHUNK_DELIVERED)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addChunkDelivered(@Context UriInfo uriInfo, String externalChunkData,
                                      @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) long jobId,
                                      @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) long chunkId)
            throws JSONBException, JobStoreException {
        return addChunk(uriInfo, jobId, chunkId, ExternalChunk.Type.DELIVERED, externalChunkData);
    }

    /**
     * Retrieves job listing from the underlying data store determined by given search criteria
     * @param jobListCriteriaData JSON representation of JobListCriteria
     * @return a HTTP 200 OK response with list of JobInfoSnapshots for selected jobs,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     * @throws JSONBException on marshalling failure
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listJobs(String jobListCriteriaData) throws JSONBException {
        try {
            final JobListCriteria jobListCriteria =
                    jsonbContext.unmarshall(jobListCriteriaData, JobListCriteria.class);
            final List<JobInfoSnapshot> jobInfoSnapshots = jobStore.listJobs(jobListCriteria);
            return Response.ok()
                    .entity(jsonbContext.marshall(jobInfoSnapshots))
                    .build();

        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Retrieves item listing from the underlying data store determined by given search criteria
     * @param itemListCriteriaData JSON representation of ItemListCriteria
     * @return a HTTP 200 OK response with list of ItemInfoSnapshots for selected items,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     * @throws JSONBException on marshalling failure
     */
    @POST
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listItems(String itemListCriteriaData) throws JSONBException {
        try {
            final ItemListCriteria itemListCriteria =
                    jsonbContext.unmarshall(itemListCriteriaData, ItemListCriteria.class);
            final List<ItemInfoSnapshot> itemInfoSnapshots = jobStore.listItems(itemListCriteria);
            return Response.ok()
                    .entity(jsonbContext.marshall(itemInfoSnapshots))
                    .build();

        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Retrieves bundle of resources relevant for a job. (sink, flow, supplementaryProcessData)
     * @param jobId of job to bundle resources for
     *
     * @return a HTTP 200 OK response with resource bundle as JSON,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve job
     *
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(JobStoreServiceConstants.JOB_RESOURCEBUNDLE)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getResourceBundle(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId) throws JSONBException, JobStoreException {
        try {
            ResourceBundle resourceBundle = jobStore.getResourceBundle(jobId);
            return Response.ok()
                    .entity(jsonbContext.marshall(resourceBundle))
                    .build();
        } catch (InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(e.getJobError()))
                    .build();
        }
    }

    /**
     * Retrieves partitioned item data, base64 decoded as String
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with base64 decoded data as String,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED)
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getPartitionedResult(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
                                         @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
                                         @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {
        return getItemData(jobId, chunkId, itemId, State.Phase.PARTITIONING);
    }

    /**
     * Retrieves processed item data, base64 decoded as String
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with base64 decoded data as String,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED)
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getProcessingResult(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
                                        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
                                        @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {
        return getItemData(jobId, chunkId, itemId, State.Phase.PROCESSING);
    }

    /**
     * Retrieves delivered item data, base64 decoded as String
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with base64 decoded data as String,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_DELIVERED)
    @Produces({ MediaType.TEXT_PLAIN })
    public Response getDeliveringResult(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
                                        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
                                        @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {
        return getItemData(jobId, chunkId, itemId, State.Phase.DELIVERING);
    }


    /**
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the item id
     * @param phase the phase of the item (PARTITIONING, PROCESSING, DELIVERING)
     * @return a HTTP 200 OK response with base64 decoded data as String,
     *         a HTTP 404 NOT_FOUND response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     */
    Response getItemData(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreException, JSONBException {
        try {
            ItemData itemData = jobStore.getItemData(jobId, chunkId, itemId, phase);
            return  Response.ok()
                    .entity(Base64Util.base64decode(itemData.getData()))
                    .build();
        } catch (InvalidInputException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
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
    Response addChunk(UriInfo uriInfo, long jobId, long chunkId, ExternalChunk.Type type, String externalChunkData)
            throws JobStoreException, JSONBException {

        final ExternalChunk chunk;
        try {
            chunk = jsonbContext.unmarshall(externalChunkData, ExternalChunk.class);
        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }

        try {
            JobError jobError = getChunkInputDataError(jobId, chunkId, chunk, type);
            if(jobError == null) {
                JobInfoSnapshot jobInfoSnapshot = jobStore.addChunk(chunk);
                return Response.created(getUri(uriInfo, Long.toString(chunk.getChunkId())))
                        .entity(jsonbContext.marshall(jobInfoSnapshot))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(jsonbContext.marshall(jobError))
                        .build();
            }

        } catch(InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(e.getJobError()))
                    .build();
        } catch(DuplicateChunkException e) {
            return Response.status(Response.Status.ACCEPTED)
                    .entity(jsonbContext.marshall(e.getJobError()))
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
                    JobError.Code.INVALID_JOB_IDENTIFIER,
                    String.format("jobId: %s did not match ExternalChunk.jobId: %s", jobId, chunk.getJobId()), null);
        } else if(chunkId != chunk.getChunkId()) {
            jobError = new JobError(
                    JobError.Code.INVALID_CHUNK_IDENTIFIER,
                    String.format("chunkId: %s did not match ExternalChunk.chunkId: %s", chunkId, chunk.getChunkId()), null);
        } else if(type != chunk.getType()) {
            jobError = new JobError(
                        JobError.Code.INVALID_CHUNK_TYPE,
                        String.format("type: %s did not match ExternalChunk.type: %s", type, chunk.getType()), null);
        }
        return jobError;
    }

}
