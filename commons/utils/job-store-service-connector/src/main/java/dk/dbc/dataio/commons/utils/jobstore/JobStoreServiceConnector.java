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

package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * JobStoreServiceConnector - dataIO job-store REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the job-store service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class JobStoreServiceConnector {
    private static final Logger log = LoggerFactory.getLogger(JobStoreServiceConnector.class);

    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public JobStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Creates new job defined by given job specification in the job-store
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException if given null-valued argument
     * @throws JobStoreServiceConnectorException on general communication failure
     * @throws JobStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: addJob();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            final Response response;
            try {
                response = HttpClient.doPostWithJson(httpClient, jobInputStream, baseUrl, JobStoreServiceConstants.JOB_COLLECTION);
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, JobInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: addJob took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds chunk and updates existing job by updating existing items, chunk and job entities in the underlying data store.
     * If attempting to re-add a previously added chunk, the method locates and returns the stored job information without updating.
     * @param chunk chunk
     * @param jobId job id
     * @param chunkId chunk id
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException if given null-valued chunk argument
     * @throws JobStoreServiceConnectorException on general failure to update job
     * @throws IllegalArgumentException on invalid chunk type
     */
    public JobInfoSnapshot addChunkIgnoreDuplicates(Chunk chunk, long jobId, long chunkId) throws NullPointerException, IllegalArgumentException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: addChunkIgnoreDuplicates({}, {});", jobId, chunkId);
        final StopWatch stopWatch = new StopWatch();
        JobInfoSnapshot jobInfoSnapshot;
        try {
            jobInfoSnapshot = addChunk(chunk, jobId, chunkId);
        } catch(JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if(e.getStatusCode() == Response.Status.ACCEPTED.getStatusCode()) {
                log.info("Ignoring duplicate chunk.id = {}. Retrieving existing jobInfoSnapShot for job.id = {}", chunkId, jobId);
                final JobListCriteria jobListCriteria = new JobListCriteria()
                        .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
                jobInfoSnapshot = listJobs(jobListCriteria).get(0);
            } else {
                throw e;
            }
        } finally {
            log.debug("JobStoreServiceConnector: addChunkIgnoreDuplicates took {} milliseconds", stopWatch.getElapsedTime());
        }
        return jobInfoSnapshot;
    }

    /**
     * Adds chunk and updates existing job by updating existing items, chunk and job entities in the underlying data store.
     * @param chunk chunk
     * @param jobId job id
     * @param chunkId chunk id
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException if given null-valued chunk argument
     * @throws JobStoreServiceConnectorException on general failure to update job
     * @throws IllegalArgumentException on invalid chunk type
     */
    public JobInfoSnapshot addChunk(Chunk chunk, long jobId, long chunkId) throws NullPointerException, IllegalArgumentException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: addChunk({}, {});", jobId, chunkId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
            final PathBuilder path = new PathBuilder(chunkTypeToJobStorePath(chunk.getType()))
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
            final Response response = HttpClient.doPostWithJson(httpClient, chunk, baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, JobInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: addChunk took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds notification request to the underlying store
     * @param request notification request
     * @return job notification representation
     * @throws NullPointerException if given null-valued notification request
     * @throws JobStoreServiceConnectorException on general failure to add notification
     */
    public JobNotification addNotification(AddNotificationRequest request) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(request, "request");
            final Response response;
            try {
                response = HttpClient.doPostWithJson(httpClient, request,
                        baseUrl, JobStoreServiceConstants.NOTIFICATIONS);
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, JobNotification.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves job listing determined by given search criteria from the job-store
     * @param criteria list criteria
     * @return list of selected job info snapshots
     * @throws NullPointerException when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listJobs();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = HttpClient.doPostWithJson(httpClient, criteria, baseUrl, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES);
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<JobInfoSnapshot>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: listJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves item listing determined by given search criteria from the job-store
     * @param criteria list criteria
     * @return list of selected item info snapshots
     * @throws NullPointerException when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce items listing
     */
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listItems();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = HttpClient.doPostWithJson(httpClient, criteria, baseUrl, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES);
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<ItemInfoSnapshot>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: listItems took {} milliseconds", stopWatch.getElapsedTime());
        }
    }
    /**
     * Retrieves job listing determined by given search criteria from the job-store
     * @param criteria list criteria
     * @return list of selected job info snapshots                             s
     * @throws NullPointerException when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public long countJobs(JobListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listJobs();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = HttpClient.doPostWithJson(httpClient, criteria, baseUrl, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT);
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<Long>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: countJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves item count determined by given search criteria from the job-store
     * @param criteria list criteria
     * @return number of items located through criteria
     * @throws NullPointerException when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public long countItems(ItemListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: countItems();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = HttpClient.doPostWithJson(httpClient, criteria, baseUrl, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT);
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<Long>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: countItems took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves a bundle of resources connected to a specific job (identified by the job id given as input)
     * @param jobId job id
     * @return resourceBundle containing sink, flow, supplementaryProcessData
     * @throws JobStoreServiceConnectorException on general failure to retrieve bundle
     * @throws IllegalArgumentException on job id less than bound value
     */
    public ResourceBundle getResourceBundle(int jobId) throws JobStoreServiceConnectorException , IllegalArgumentException{
        log.trace("JobStoreServiceConnector: getResourceBundle({});", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_RESOURCEBUNDLE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, ResourceBundle.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getResourceBundle took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public ChunkItem getChunkItem(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreServiceConnectorException, IllegalArgumentException{
        log.trace("JobStoreServiceConnector: getChunkItem({}, {}, {}, {});", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(phaseToJobStorePath(phase))
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, ChunkItem.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getChunkItem took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves processed next result: Representing the the data stored within the next chunk item as String
     * @param jobId job id
     * @param chunkId chunk id
     * @param itemId item id
     *
     * @return processed next result
     * @throws JobStoreServiceConnectorException on general failure to retrieve processed next result
     * @throws IllegalArgumentException on job id less than bound value
     */
    public String getProcessedNextResult(int jobId, int chunkId, short itemId) throws JobStoreServiceConnectorException, IllegalArgumentException{
        log.trace("JobStoreServiceConnector: getProcessedNextResult({}, {}, {}, {});", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, String.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getProcessedNextResult took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves job notifications identified by jobId from the job-store
     * @param jobId The jobId
     * @return list of selected job notifications
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<JobNotification> listJobNotificationsForJob(int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listJobNotificationsForJob();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_NOTIFICATIONS)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<JobNotification>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: listJobNotificationsForJob took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    public JobInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: setWorkflowNote({});", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
            final Response response;
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
            try {
                response = HttpClient.doPostWithJson(httpClient, workflowNote, baseUrl, path.build());
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, JobInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: setWorkflowNote took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public ItemInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId, int chunkId, short itemId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: setWorkflowNote({}, {}, {}, {});", jobId, chunkId, itemId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
            final Response response;
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId))
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId))
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(itemId));
            try {
                response = HttpClient.doPostWithJson(httpClient, workflowNote, baseUrl, path.build());
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, ItemInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: setWorkflowNote took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /*
     * Private methods
     */

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) throws JobStoreServiceConnectorUnexpectedStatusCodeException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            final JobStoreServiceConnectorUnexpectedStatusCodeException exception =
                    new JobStoreServiceConnectorUnexpectedStatusCodeException(String.format(
                            "job-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
            if (actualStatus == Response.Status.BAD_REQUEST) {
                try {
                    exception.setJobError(readResponseEntity(response, JobError.class));
                } catch (JobStoreServiceConnectorException e) {
                    log.warn("Unable to extract job-store error from response", e);
                }
            }
            throw exception;
        }
    }

    private <T> T readResponseEntity(Response response, GenericType<T> genericType) throws JobStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(genericType);
        if (entity == null) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with null-valued %s entity", genericType.getRawType().getName()));
        }
        return entity;
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws JobStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }

    private String chunkTypeToJobStorePath(Chunk.Type chunkType) throws IllegalArgumentException {
        switch (chunkType) {
            case PROCESSED:   return JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
            case DELIVERED:   return JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
            case PARTITIONED: throw new IllegalArgumentException("PARTITIONED is not a valid type");
            default:          throw new IllegalArgumentException("Chunk.Type could not be identified");
        }
    }

    private String phaseToJobStorePath(State.Phase phase) throws IllegalArgumentException {
        switch (phase) {
            case PARTITIONING:return JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED;
            case PROCESSING:  return JobStoreServiceConstants.CHUNK_ITEM_PROCESSED;
            case DELIVERING:  return JobStoreServiceConstants.CHUNK_ITEM_DELIVERED;
            default:          throw new IllegalArgumentException("State.Phase could not be identified");
        }
    }
}
