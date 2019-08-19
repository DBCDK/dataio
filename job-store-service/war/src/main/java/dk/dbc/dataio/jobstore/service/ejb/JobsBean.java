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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Stateless
@Path("/")
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    JSONBContext jsonbContext = new JSONBContext();

    /* This is not private so it is accessible from automatic test */
    @EJB
    PgJobStore jobStore;

    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    JobNotificationRepository jobNotificationRepository;

    @EJB
    JobSchedulerBean jobSchedulerBean;

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
    @Stopwatch
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
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        } catch(InvalidInputException e) {
            return Response.status(BAD_REQUEST).entity(jsonbContext.marshall(e.getJobError())).build();
        }
    }

    /**
     * Adds new acceptance test job based on POSTed job input stream, and persists it in the underlying data store
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
    @Path(JobStoreServiceConstants.JOB_COLLECTION_ACCTESTS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response addAccTestJob(@Context UriInfo uriInfo, String jobInputStreamData) throws JSONBException, JobStoreException {
        final AccTestJobInputStream jobInputStream;
        JobInfoSnapshot jobInfoSnapshot;

        try {
            jobInputStream = jsonbContext.unmarshall(jobInputStreamData, AccTestJobInputStream.class);
            jobInfoSnapshot = jobStore.addAndScheduleAccTestJob(jobInputStream);
            return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.getJobId())))
                    .entity(jsonbContext.marshall(jobInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        } catch(InvalidInputException e) {
            return Response.status(BAD_REQUEST).entity(jsonbContext.marshall(e.getJobError())).build();
        }
    }

    /**
     * Sets the workflow note from POSTed workflow note string on the given job, and persists it in the underlying data store
     *
     * @param workflowNoteString the workflow note to set on the job entity
     * @param jobId of the job entity
     * @return a HTTP 200 OK response with jobInfoSnapshot as JSON,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found,
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to set workflow note
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response setWorkflowNote(String workflowNoteString, @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId) throws JSONBException, JobStoreException {
        LOGGER.trace("jobId: {}, workflowNote: {}", jobId, workflowNoteString);
        final JobInfoSnapshot jobInfoSnapshot;
        try {
            final WorkflowNote workflowNote = jsonbContext.unmarshall(workflowNoteString, WorkflowNote.class);
            jobInfoSnapshot = jobStore.setWorkflowNote(workflowNote, jobId);
            return Response.ok()
                    .entity(jsonbContext.marshall(jobInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Sets the workflow note from POSTed workflow note string on the given item, and persists it in the underlying data store
     *
     * @param workflowNoteString the workflow note to set on the item entity
     * @param jobId of the referenced job
     * @param chunkId of the referenced job
     * @param itemId of the item
     * @return a HTTP 200 OK response with jobInfoSnapshot as JSON,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found,
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to set workflow note
     */
    @POST
    @Path(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response setWorkflowNote(String workflowNoteString,
                                    @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
                                    @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
                                    @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {
        LOGGER.trace("jobId: {}, chunkId: {}, itemId: {}, workflowNote: {}", jobId, chunkId, itemId, workflowNoteString);
        final ItemInfoSnapshot itemInfoSnapshot;
        try {
            final WorkflowNote workflowNote = jsonbContext.unmarshall(workflowNoteString, WorkflowNote.class);
            itemInfoSnapshot = jobStore.setWorkflowNote(workflowNote, jobId, chunkId, itemId);
            return Response.ok()
                    .entity(jsonbContext.marshall(itemInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Adds chunk with type: PROCESSED (updates existing job by adding chunk)
     *
     * @param uriInfo application and request URI information
     * @param chunkData chunk data as json
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
    @Stopwatch
    public Response addChunkProcessed(
        @Context UriInfo uriInfo,
        String chunkData,
        @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) long jobId,
        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) long chunkId) throws JSONBException, JobStoreException {

        final Chunk processedChunk;
        try {
            processedChunk = jsonbContext.unmarshall(chunkData, Chunk.class);
        } catch (JSONBException e) {
            return buildBadRequestResponse(e);
        }

        jobSchedulerBean.chunkProcessingDone( processedChunk );

        return addChunk(uriInfo, jobId, chunkId, Chunk.Type.PROCESSED, processedChunk);
    }

    /**
     * Adds chunk with type: DELIVERED (updates existing job by adding chunk)
     *
     * @param uriInfo application and request URI information
     * @param chunkData chunk data as json
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
    @Stopwatch
    public Response addChunkDelivered(

        @Context UriInfo uriInfo,
        String chunkData,
        @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) long jobId,
        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) long chunkId) throws JSONBException, JobStoreException {

        final Chunk deliveredChunk;
        try {
            deliveredChunk = jsonbContext.unmarshall(chunkData, Chunk.class);
        } catch (JSONBException e) {
            return buildBadRequestResponse(e);
        }

        jobSchedulerBean.chunkDeliveringDone( deliveredChunk );

        // Todo check hvordan job afsluttes.
        return addChunk(uriInfo, jobId, chunkId, Chunk.Type.DELIVERED, deliveredChunk);
    }

    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION_QUERIES)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listJobsByPost(String query) throws JSONBException {
        return listJobsByIOQL(query);
    }

    @GET
    @Path(JobStoreServiceConstants.JOB_COLLECTION_QUERIES)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listJobsByGet(@QueryParam("q") String query) throws JSONBException {
        return listJobsByIOQL(query);
    }

    private Response listJobsByIOQL(String query) throws JSONBException {
        try {
            final List<JobInfoSnapshot> jobInfoSnapshots = jobStoreRepository.listJobs(query);
            return Response.ok().entity(jsonbContext.marshall(jobInfoSnapshots)).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_INPUT,
                                    e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION_COUNT)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response countJobsByPost(String query) throws JSONBException {
        return countJobsByIOQL(query);
    }

    @GET
    @Path(JobStoreServiceConstants.JOB_COLLECTION_COUNT)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response countJobsByGet(@QueryParam("q") String query) throws JSONBException {
        return countJobsByIOQL(query);
    }

    private Response countJobsByIOQL(String query) throws JSONBException {
        try {
            final long count = jobStoreRepository.countJobs(query);
            return Response.ok().entity(jsonbContext.marshall(count)).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_INPUT,
                                    e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    @POST
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_QUERIES)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listItemsByPost(String query) throws JSONBException {
        return listItemsByIOQL(query);
    }

    @GET
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_QUERIES)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response listItemsByGet(@QueryParam("q") String query) throws JSONBException {
        return listItemsByIOQL(query);
    }

    private Response listItemsByIOQL(String query) throws JSONBException {
        try {
            final List<ItemInfoSnapshot> itemInfoSnapshots = jobStoreRepository.listItems(query);
            return Response.ok().entity(jsonbContext.marshall(itemInfoSnapshots)).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_INPUT,
                                    e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    @POST
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_COUNT)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response countItemsByPost(String query) throws JSONBException {
        return countItemsByIOQL(query);
    }

    @GET
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_COUNT)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response countItemsByGet(@QueryParam("q") String query) throws JSONBException {
        return countItemsByIOQL(query);
    }

    private Response countItemsByIOQL(String query) throws JSONBException {
        try {
            final long count = jobStoreRepository.countItems(query);
            return Response.ok().entity(jsonbContext.marshall(count)).build();
        } catch (NullPointerException | IllegalArgumentException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_INPUT,
                                    e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
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
    //@Stopwatch
    public Response listJobs(String jobListCriteriaData) throws JSONBException {
        try {
            final JobListCriteria jobListCriteria = jsonbContext.unmarshall(jobListCriteriaData, JobListCriteria.class);
            final List<JobInfoSnapshot> jobInfoSnapshots = jobStoreRepository.listJobs(jobListCriteria);
            return Response.ok().entity(jsonbContext.marshall(jobInfoSnapshots)).build();
        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

   /**
     * Retrieves job listing from the underlying data store determined by given search criteria
     * @param jobListCriteriaData JSON representation of JobListCriteria
     * @return a HTTP 200 OK response with list of JobInfoSnapshots for selected jobs,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     * @throws JSONBException on marshalling failure
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response countJobs(String jobListCriteriaData) throws JSONBException {
        try {
            final JobListCriteria jobListCriteria = jsonbContext.unmarshall(jobListCriteriaData, JobListCriteria.class);
            final long count = jobStoreRepository.countJobs(jobListCriteria);
            LOGGER.debug("count Response {}",count);
            return Response.ok().entity(jsonbContext.marshall(count)).build();
        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(BAD_REQUEST)
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
    @Stopwatch
    public Response listItems(String itemListCriteriaData) throws JSONBException {
        try {
            final ItemListCriteria itemListCriteria = jsonbContext.unmarshall(itemListCriteriaData, ItemListCriteria.class);
            final List<ItemInfoSnapshot> itemInfoSnapshots = jobStoreRepository.listItems(itemListCriteria);
            return Response.ok().entity(jsonbContext.marshall(itemInfoSnapshots)).build();

        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Retrieves item count from the underlying data store determined by given search criteria
     * @param itemListCriteriaData JSON representation of ItemListCriteria
     * @return a HTTP 200 OK response with count of selected items,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     * @throws JSONBException on marshalling failure
     */
    @POST
    @Path(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response countItems(String itemListCriteriaData) throws JSONBException {
        try {
            final ItemListCriteria itemListCriteria = jsonbContext.unmarshall(itemListCriteriaData, ItemListCriteria.class);
            final long count = jobStoreRepository.countItems(itemListCriteria);
            return Response.ok().entity(jsonbContext.marshall(count)).build();
        } catch (JSONBException e) {
            LOGGER.warn("Bad request: {}", e.getMessage());
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Retrieves the flow cashed for the specified job.
     * @param jobId of job to retrieve cashed flow from
     *
     * @return a HTTP 200 OK response with flow as JSON,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve job
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to retrieve job
     */
    @GET
    @Path(JobStoreServiceConstants.JOB_CACHED_FLOW)
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response getCachedFlow(@PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId) throws JSONBException, JobStoreException {
        try {
            Flow flow = jobStoreRepository.getCachedFlow(jobId);
            return Response.ok().entity(jsonbContext.marshall(flow)).build();
        } catch (InvalidInputException e) {
            return Response.status(BAD_REQUEST).entity(jsonbContext.marshall(e.getJobError())).build();
        }
    }

    /**
     * Retrieves partitioned chunk item
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with partitioned chunk item as entity,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to retrieve job
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getPartitionedResult(
        @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
        @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {

        return getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.PARTITIONING);
    }

    /**
     * Retrieves processed chunk item
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with processed chunk item as entity,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to retrieve job
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getProcessingResult(
        @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
        @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {

        return getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.PROCESSING);
    }

    /**
     * Retrieves delivered chunk item
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with delivered chunk item as entity,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to retrieve job
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_DELIVERED)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getDeliveringResult(
        @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
        @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
        @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {

        return getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.DELIVERING);
    }

    /**
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the item id
     * @param phase the phase of the item (PARTITIONING, PROCESSING, DELIVERING)
     * @return a HTTP 200 OK response with chunk item as String,
     *         a HTTP 404 NOT_FOUND response on failure to retrieve item
     *
     * @throws JobStoreException on failure to retrieve job
     * @throws JSONBException on marshalling failure
     */
    Response getChunkItemForPhase(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreException, JSONBException {
        try {
            ChunkItem chunkItem = jobStoreRepository.getChunkItemForPhase(jobId, chunkId, itemId, phase);
            return Response.ok().entity(jsonbContext.marshall(chunkItem)).build();
        } catch (InvalidInputException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    /**
     * Retrieves processed next chunk item
     * @param jobId the job id
     * @param chunkId the chunk id
     * @param itemId the itemId
     *
     * @return a HTTP 200 OK response with processed next chunk item as entity,
     *         a HTTP 400 BAD_REQUEST response on failure to retrieve item
     *
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on failure to retrieve item
     */
    @GET
    @Path(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response getProcessedNextResult(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @PathParam(JobStoreServiceConstants.CHUNK_ID_VARIABLE) int chunkId,
            @PathParam(JobStoreServiceConstants.ITEM_ID_VARIABLE) short itemId) throws JSONBException, JobStoreException {

        try {
            ChunkItem chunkItem = jobStoreRepository.getNextProcessingOutcome(jobId, chunkId, itemId);
            return Response.ok().entity(jsonbContext.marshall(chunkItem)).build();
        } catch (InvalidInputException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    /**
     * Retrieves list of notifications associated with job
     * @param jobId id of job for which to retrieve notifications
     * @return a HTTP 200 OK response with list of notifications as JSON String
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(JobStoreServiceConstants.JOB_NOTIFICATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Stopwatch
    public Response getNotificationsForJob(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId) throws JSONBException {
        final List<NotificationEntity> notifications = jobNotificationRepository.getNotificationsForJob(jobId);
        return Response.ok().entity(jsonbContext.marshall(notifications)).build();
    }

    /**
     * This method exports all chunk items, which have failed in PARTITIONING, from the job specified
     * @param jobId of the job
     * @param format of the data contained within the items
     * @return a HTTP 200 OK response with item data as stream
     *         a HTTP 404 NOT_FOUND response on failure to export items
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED_FAILED)
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedInPartitioning(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) {

        try {
            return Response.ok(exportFailedItems(jobId, State.Phase.PARTITIONING, format)).build();
        } catch (JobStoreException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    /**
     * This method exports all chunk items, which have failed in PROCESSING, from the job specified
     * @param jobId of the job
     * @param format of the data contained within the items
     * @return a HTTP 200 OK response with item data as stream
     *         a HTTP 404 NOT_FOUND response on failure to export items
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED_FAILED)
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedInProcessing(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) {

        try {
            return Response.ok(exportFailedItems(jobId, State.Phase.PROCESSING, format)).build();
        } catch (JobStoreException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    /**
     * This method exports all chunk items, which have failed in DELIVERING, from the job specified
     * @param jobId of the job
     * @param format of the data contained within the items
     * @return a HTTP 200 OK response with item data as stream
     *         a HTTP 404 NOT_FOUND response on failure to export items
     */
    @GET
    @Path(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED_FAILED)
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    @Stopwatch
    public Response exportItemsFailedInDelivering(
            @PathParam(JobStoreServiceConstants.JOB_ID_VARIABLE) int jobId,
            @QueryParam(JobStoreServiceConstants.QUERY_PARAM_FORMAT) ChunkItem.Type format) {

        try {
            return Response.ok(exportFailedItems(jobId, State.Phase.DELIVERING, format)).build();
        } catch (JobStoreException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    /**
     * This method exports from a specified job all chunk items, which have failed in a specified phase
     * @param jobId of job containing the items
     * @param phase in which the requested items have failed
     * @param format of item data
     * @return stream containing the exported items
     * @throws JobStoreException on general failure to write output stream
     */
    private StreamingOutput exportFailedItems(int jobId, State.Phase phase, ChunkItem.Type format) throws JobStoreException {
        final ByteArrayOutputStream byteArrayOutputStream = jobStoreRepository.exportFailedItems(
                jobId, phase, format, StandardCharsets.UTF_8);
        return os -> os.write(byteArrayOutputStream.toByteArray());
    }

    /**
     * @param uriInfo application and request URI information
     * @param jobId job id
     * @param chunkId chunk id
     * @param type chunk type (PARTITIONED, PROCESSED, DELIVERED)
     * @param chunk chunk data
     *
     * @return HTTP 201 CREATED response on success, HTTP 400 BAD_REQUEST response on failure to update job
     * @throws JSONBException on marshalling failure
     * @throws JobStoreException on referenced entities not found
     */
    Response addChunk(
        UriInfo uriInfo,
        long jobId,
        long chunkId,
        Chunk.Type type,
        Chunk chunk) throws JobStoreException, JSONBException {

        try {
            JobError jobError = getChunkInputDataError(jobId, chunkId, chunk, type);
            if(jobError == null) {
                JobInfoSnapshot jobInfoSnapshot = jobStore.addChunk(chunk);
                return Response.created(getUri(uriInfo, Long.toString(chunk.getChunkId())))
                        .entity(jsonbContext.marshall(jobInfoSnapshot))
                        .build();
            } else {
                return Response.status(BAD_REQUEST).entity(jsonbContext.marshall(jobError)).build();
            }

        } catch(InvalidInputException e) {
            return Response.status(BAD_REQUEST).entity(jsonbContext.marshall(e.getJobError())).build();
        } catch(DuplicateChunkException e) {
            return Response.status(ACCEPTED).entity(jsonbContext.marshall(e.getJobError())).build();
        }
    }

    /**
     *
     * @param jobId job id
     * @param chunkId chunk id
     * @param chunk chunk
     * @param type chunk type
     * @return jobError containing the relevant error message, null if input data is valid
     */
    private JobError getChunkInputDataError(long jobId, long chunkId, Chunk chunk, Chunk.Type type) {
        JobError jobError = null;
        if(jobId != chunk.getJobId()) {
            jobError = new JobError(
                    JobError.Code.INVALID_JOB_IDENTIFIER,
                    String.format("jobId: %s did not match Chunk.jobId: %s", jobId, chunk.getJobId()),
                    JobError.NO_STACKTRACE);
        } else if(chunkId != chunk.getChunkId()) {
            jobError = new JobError(
                    JobError.Code.INVALID_CHUNK_IDENTIFIER,
                    String.format("chunkId: %s did not match Chunk.chunkId: %s", chunkId, chunk.getChunkId()),
                    JobError.NO_STACKTRACE);
        } else if(type != chunk.getType()) {
            jobError = new JobError(
                    JobError.Code.INVALID_CHUNK_TYPE,
                    String.format("type: %s did not match Chunk.type: %s", type, chunk.getType()),
                    JobError.NO_STACKTRACE);
        }
        return jobError;
    }

    private Response buildBadRequestResponse(JSONBException e) throws JSONBException {
        return Response.status(BAD_REQUEST).entity(
                jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                .build();
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }
}