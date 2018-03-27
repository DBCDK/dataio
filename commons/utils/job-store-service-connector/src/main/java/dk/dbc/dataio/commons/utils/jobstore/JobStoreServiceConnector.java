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
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
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
import javax.ws.rs.core.MediaType;
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

    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param client web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public JobStoreServiceConnector(Client client, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(HttpClient.create(client), baseUrl);
    }

    JobStoreServiceConnector(HttpClient httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: addJob();");
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            final Response response;
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(JobStoreServiceConstants.JOB_COLLECTION)
                        .withJsonData(jobInputStream)
                        .execute();
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
     * Creates new acceptance test job defined by given job specification in the job-store
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException if given null-valued argument
     * @throws JobStoreServiceConnectorException on general communication failure
     * @throws JobStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public JobInfoSnapshot addAccTestJob(AccTestJobInputStream jobInputStream) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            final Response response;
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_ACCTESTS)
                        .withJsonData(jobInputStream)
                        .execute();
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
            log.debug("addAccTestJob took {} milliseconds", stopWatch.getElapsedTime());
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: addChunkIgnoreDuplicates({}, {});", jobId, chunkId);
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: addChunk({}, {});", jobId, chunkId);
        try {
            InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
            final PathBuilder path = new PathBuilder(chunkTypeToJobStorePath(chunk.getType()))
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
            final Response response = new HttpPost(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withJsonData(chunk)
                    .execute();
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
    public Notification addNotification(AddNotificationRequest request) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(request, "request");
            final Response response;
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(JobStoreServiceConstants.NOTIFICATIONS)
                        .withJsonData(request)
                        .execute();
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Notification.class);
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: listJobs();");
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = new HttpPost(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES)
                    .withJsonData(criteria)
                    .execute();
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: listItems();");
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = new HttpPost(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES)
                    .withJsonData(criteria)
                    .execute();
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: listJobs();");
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = new HttpPost(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT)
                    .withJsonData(criteria)
                    .execute();
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: countItems();");
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = new HttpPost(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT)
                    .withJsonData(criteria)
                    .execute();
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
     * Retrieves the cached flow of a specific job
     * @param jobId job id
     * @return the cached flow
     * @throws JobStoreServiceConnectorException on general failure to retrieve flow
     * @throws IllegalArgumentException on job id less than bound value
     */
    public Flow getCachedFlow(int jobId) throws JobStoreServiceConnectorException , IllegalArgumentException {
        log.trace("JobStoreServiceConnector: getCachedFlow({});", jobId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_CACHED_FLOW)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Flow.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getCachedFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    public ChunkItem getChunkItem(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreServiceConnectorException, IllegalArgumentException{
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: getChunkItem({}, {}, {}, {});", jobId, chunkId, itemId);
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(phaseToJobStorePath(phase))
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
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
    public ChunkItem getProcessedNextResult(int jobId, int chunkId, short itemId) throws JobStoreServiceConnectorException, IllegalArgumentException{
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: getProcessedNextResult({}, {}, {}, {});", jobId, chunkId, itemId);
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, ChunkItem.class);
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
    public List<Notification> listJobNotificationsForJob(int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: listJobNotificationsForJob();");
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_NOTIFICATIONS)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<Notification>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: listJobNotificationsForJob took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public List<Notification> listInvalidTransfileNotifications() throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.NOTIFICATIONS_TYPES_INVALID_TRNS)
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<Notification>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: listInvalidTransfileNotifications took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public JobInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: setWorkflowNote({});", jobId);
        try {
            InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
            final Response response;
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(path.build())
                        .withJsonData(workflowNote)
                        .execute();
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
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: setWorkflowNote({}, {}, {}, {});", jobId, chunkId, itemId);
        try {
            InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
            final Response response;
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId))
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId))
                    .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(itemId));
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(path.build())
                        .withJsonData(workflowNote)
                        .execute();
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

    /**
     * Retrieves statuses for all sinks
     *
     * @return a list containing status of sink
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on general failure to produce sink status listing
     */
    public List<SinkStatusSnapshot> getSinkStatusList() throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: getSinkStatusList()");
        try {
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(JobStoreServiceConstants.SINKS_STATUS)
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<SinkStatusSnapshot>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getSinkStatusList took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves status for a specific sinks
     *
     * @param sinkId the id of the sink
     * @return sinkStatusSnapshot
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on general failure to produce sink status
     */
    public SinkStatusSnapshot getSinkStatus(int sinkId) throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: getSinkStatus({})", sinkId);
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.SINK_STATUS)
                    .bind(JobStoreServiceConstants.SINK_ID_VARIABLE, sinkId);
            final Response response = new HttpGet(httpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, SinkStatusSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: getSinkStatus took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Creates rerun task for given job ID
     * @param jobId ID of job to be rerun
     * @param failedItemsOnly determining whether all items or only failed should be rerun
     * @throws JobStoreServiceConnectorException on failure to create rerun task
     */
    public void createJobRerun(int jobId, boolean failedItemsOnly) throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: createJobRerun({});", jobId);
        try {
            final Response response;
            try {
                response = new HttpPost(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(JobStoreServiceConstants.RERUNS)
                        .withQueryParameter("failedItemsOnly", failedItemsOnly)
                        .withData(Integer.toString(jobId), MediaType.TEXT_PLAIN)
                        .execute();
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.CREATED);
                readResponseEntity(response, String.class);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: createJobRerun took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public Client getClient() {
        return httpClient.getClient();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) throws JobStoreServiceConnectorUnexpectedStatusCodeException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            final JobStoreServiceConnectorUnexpectedStatusCodeException exception =
                    new JobStoreServiceConnectorUnexpectedStatusCodeException(String.format(
                            "job-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
            if (response.hasEntity()) {
                try {
                    exception.setJobError(readResponseEntity(response, JobError.class));
                } catch (JobStoreServiceConnectorException | ProcessingException | ClassCastException e) {
                    try {
                        log.error("job-store response was: {}", readResponseEntity(response, String.class));
                    } catch (JobStoreServiceConnectorException jssce) {
                        log.warn("Unable to extract entity from response", e);
                    }
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
