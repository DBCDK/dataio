package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
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
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.List;

import static dk.dbc.dataio.commons.utils.jobstore.Metric.ABORT_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.ADD_ACC_TEST_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.ADD_CHUNK;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.ADD_EMPTY_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.ADD_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.ADD_NOTIFICATION;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.COUNT_ITEMS;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.COUNT_JOBS;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.GET_CACHED_FLOW;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.GET_CHUNK_ITEM;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.GET_PROCESSED_NEXT_RESULT;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.GET_SINK_STATUS_LIST;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.LIST_INVALID_TRANSFILE_NOTIFICATIONS;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.LIST_ITEMS;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.LIST_JOBS;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.LIST_JOBS_CRIT;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.LIST_JOB_NOTIFICATIONS_FOR_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.RESEND_JOB;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.SET_WORKFLOW_NOTE;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.SET_WORKFLOW_NOTE2;
import static dk.dbc.dataio.commons.utils.jobstore.Metric.SINK_STATUS;

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
@SuppressWarnings("PMD.TooManyStaticImports")
public class JobStoreServiceConnector {
    private static final Logger log = LoggerFactory.getLogger(JobStoreServiceConnector.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final MetricRegistry metricRegistry;

    /**
     * Class constructor
     *
     * @param client  web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public JobStoreServiceConnector(Client client, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(HttpClient.create(client), baseUrl, null);
    }

    public JobStoreServiceConnector(Client client, String baseUrl, MetricRegistry metricRegistry) throws NullPointerException, IllegalArgumentException {
        this(HttpClient.create(client), baseUrl, metricRegistry);
    }

    JobStoreServiceConnector(HttpClient httpClient, String baseUrl, MetricRegistry metricRegistry) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
        this.metricRegistry = metricRegistry;
    }

    public JobInfoSnapshot abortJob(int jobId) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: abortJob({})", jobId);
        try {
            return post(null, Response.Status.OK, JobInfoSnapshot.class, ABORT_JOB, JobStoreServiceConstants.JOB_ABORT, Integer.toString(jobId));
        } catch (ProcessingException e) {
            throw new JobStoreServiceConnectorException("job-store communication error", e);
        }
    }

    public JobInfoSnapshot resendJob(int jobId) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: resendJob({})", jobId);
        try {
            return post(null, Response.Status.OK, JobInfoSnapshot.class, RESEND_JOB, JobStoreServiceConstants.JOB_RESEND, Integer.toString(jobId));
        } catch (ProcessingException e) {
            throw new JobStoreServiceConnectorException("job-store communication error", e);
        }
    }

    /**
     * Creates new job defined by given job specification in the job-store
     *
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException                                  if given null-valued argument
     * @throws JobStoreServiceConnectorException                     on general communication failure
     * @throws JobStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: addJob()");
        InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
        try {
            return post(jobInputStream, Response.Status.CREATED, JobInfoSnapshot.class, ADD_JOB, JobStoreServiceConstants.JOB_COLLECTION);
        } catch (ProcessingException e) {
            throw new JobStoreServiceConnectorException("job-store communication error", e);
        }
    }

    /**
     * Creates new acceptance test job defined by given job specification in the job-store
     *
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException                                  if given null-valued argument
     * @throws JobStoreServiceConnectorException                     on general communication failure
     * @throws JobStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public JobInfoSnapshot addAccTestJob(AccTestJobInputStream jobInputStream) throws NullPointerException, JobStoreServiceConnectorException {
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            return post(jobInputStream, Response.Status.CREATED, JobInfoSnapshot.class, ADD_ACC_TEST_JOB, JobStoreServiceConstants.JOB_COLLECTION_ACCTESTS);
        } catch (ProcessingException e) {
            throw new JobStoreServiceConnectorException("job-store communication error", e);
        }
    }

    /**
     * Creates new empty job defined by given job specification in the job-store
     *
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot job snapshot information
     * @throws JobStoreServiceConnectorException                     on general communication failure
     * @throws JobStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public JobInfoSnapshot addEmptyJob(JobInputStream jobInputStream) throws JobStoreServiceConnectorException {
        try {
            return post(jobInputStream, Response.Status.CREATED, JobInfoSnapshot.class, ADD_EMPTY_JOB, JobStoreServiceConstants.JOB_COLLECTION_EMPTY);
        } catch (ProcessingException e) {
            throw new JobStoreServiceConnectorException("job-store communication error", e);
        }
    }

    /**
     * Adds chunk and updates existing job by updating existing items, chunk and job entities in the underlying data store.
     * If attempting to re-add a previously added chunk, the method locates and returns the stored job information without updating.
     *
     * @param chunk   chunk
     * @param jobId   job id
     * @param chunkId chunk id
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException              if given null-valued chunk argument
     * @throws JobStoreServiceConnectorException on general failure to update job
     * @throws IllegalArgumentException          on invalid chunk type
     */
    public JobInfoSnapshot addChunkIgnoreDuplicates(Chunk chunk, int jobId, long chunkId) throws NullPointerException, IllegalArgumentException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: addChunkIgnoreDuplicates({}, {});", jobId, chunkId);
        JobInfoSnapshot jobInfoSnapshot;
        try {
            jobInfoSnapshot = addChunk(chunk, jobId, chunkId);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            if (e.getStatusCode() == Response.Status.ACCEPTED.getStatusCode()) {
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
     *
     * @param chunk   chunk
     * @param jobId   job id
     * @param chunkId chunk id
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     * @throws NullPointerException              if given null-valued chunk argument
     * @throws JobStoreServiceConnectorException on general failure to update job
     * @throws IllegalArgumentException          on invalid chunk type
     */
    public JobInfoSnapshot addChunk(Chunk chunk, int jobId, long chunkId) throws NullPointerException, IllegalArgumentException, JobStoreServiceConnectorException {
        log.info("JobStoreServiceConnector: addChunk({}/{});", jobId, chunkId);
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        final PathBuilder path = new PathBuilder(chunkTypeToJobStorePath(chunk.getType()))
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
        return post(chunk, Response.Status.CREATED, JobInfoSnapshot.class, ADD_CHUNK, path.build());
    }

    /**
     * Adds notification request to the underlying store
     *
     * @param request notification request
     * @return job notification representation
     * @throws NullPointerException              if given null-valued notification request
     * @throws JobStoreServiceConnectorException on general failure to add notification
     */
    public Notification addNotification(AddNotificationRequest request) throws NullPointerException, JobStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(request, "request");
        return post(request, Response.Status.OK, Notification.class, ADD_NOTIFICATION, JobStoreServiceConstants.NOTIFICATIONS);
    }

    /**
     * Retrieves job listing determined by given search criteria from the job-store
     *
     * @param criteria list criteria
     * @return list of selected job info snapshots
     * @throws NullPointerException              when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listJobs();");
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return post(criteria, Response.Status.OK, new GenericType<>() {}, LIST_JOBS_CRIT, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES);
    }

    /**
     * Retrieves job listing determined by given search query from the job-store
     *
     * @param query dataIOQL query string
     * @return list of selected job info snapshots
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<JobInfoSnapshot> listJobs(String query) throws JobStoreServiceConnectorException {
        StopWatch stopWatch = new StopWatch();
        Metric.StatusTag status = Metric.StatusTag.FAILED;
        InvariantUtil.checkNotNullNotEmptyOrThrow(query, "query");
        try (Response response = new HttpPost(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_QUERIES)
                .withData(query, MediaType.TEXT_PLAIN)
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            status = Metric.StatusTag.SUCCESS;
            return readResponseEntity(response, new GenericType<>() {});
        } finally {
            if(metricRegistry != null) LIST_JOBS.simpleTimer(metricRegistry, status.tag).update(Duration.ofMillis(stopWatch.getElapsedTime()));
        }
    }

    /**
     * Retrieves item listing determined by given search criteria from the job-store
     *
     * @param criteria list criteria
     * @return list of selected item info snapshots
     * @throws NullPointerException              when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce items listing
     */
    public List<ItemInfoSnapshot> listItems(ItemListCriteria criteria) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listItems();");
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return post(criteria, Response.Status.OK, new GenericType<>() {}, LIST_ITEMS, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES);
    }

    /**
     * Retrieves job listing determined by given search criteria from the job-store
     *
     * @param criteria list criteria
     * @return list of selected job info snapshots                             s
     * @throws NullPointerException              when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public long countJobs(JobListCriteria criteria) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: countJobs();");
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return post(criteria, Response.Status.OK, Long.class, COUNT_JOBS, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT);
    }

    /**
     * Retrieves item count determined by given search criteria from the job-store
     *
     * @param criteria list criteria
     * @return number of items located through criteria
     * @throws NullPointerException              when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public long countItems(ItemListCriteria criteria) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: countItems();");
        InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
        return post(criteria, Response.Status.OK, Long.class, COUNT_ITEMS, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT);
    }

    /**
     * Retrieves the cached flow of a specific job
     *
     * @param jobId job id
     * @return the cached flow
     * @throws JobStoreServiceConnectorException on general failure to retrieve flow
     * @throws IllegalArgumentException          on job id less than bound value
     */
    public Flow getCachedFlow(int jobId) throws JobStoreServiceConnectorException, IllegalArgumentException {
        log.trace("JobStoreServiceConnector: getCachedFlow({});", jobId);
        InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_CACHED_FLOW)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        return get(Flow.class, Response.Status.OK, GET_CACHED_FLOW, path.build());
    }


    public ChunkItem getChunkItem(int jobId, int chunkId, short itemId, State.Phase phase) throws JobStoreServiceConnectorException, IllegalArgumentException {
        log.trace("JobStoreServiceConnector: getChunkItem({}, {}, {});", jobId, chunkId, itemId);
        InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
        final PathBuilder path = new PathBuilder(phaseToJobStorePath(phase))
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
        return get(ChunkItem.class, Response.Status.OK, GET_CHUNK_ITEM, path.build());
    }

    /**
     * Retrieves processed next result: Representing the the data stored within the next chunk item as String
     *
     * @param jobId   job id
     * @param chunkId chunk id
     * @param itemId  item id
     * @return processed next result
     * @throws JobStoreServiceConnectorException on general failure to retrieve processed next result
     * @throws IllegalArgumentException          on job id less than bound value
     */
    public ChunkItem getProcessedNextResult(int jobId, int chunkId, short itemId) throws JobStoreServiceConnectorException, IllegalArgumentException {
        log.trace("JobStoreServiceConnector: getProcessedNextResult({}, {}, {});", jobId, chunkId, itemId);
        InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
        return get(ChunkItem.class, Response.Status.OK, GET_PROCESSED_NEXT_RESULT, path.build());
    }

    /**
     * Retrieves job notifications identified by jobId from the job-store
     *
     * @param jobId The jobId
     * @return list of selected job notifications
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<Notification> listJobNotificationsForJob(int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: listJobNotificationsForJob();");
        InvariantUtil.checkIntLowerBoundOrThrow(jobId, "jobId", 0);
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_NOTIFICATIONS)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        return get(new GenericType<>() {}, Response.Status.OK, LIST_JOB_NOTIFICATIONS_FOR_JOB, path.build());
    }

    public List<Notification> listInvalidTransfileNotifications() throws JobStoreServiceConnectorException {
        return get(new GenericType<>() {}, Response.Status.OK, LIST_INVALID_TRANSFILE_NOTIFICATIONS, JobStoreServiceConstants.NOTIFICATIONS_TYPES_INVALID_TRNS);
    }

    public JobInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: setWorkflowNote({});", jobId);
        InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        return post(workflowNote, Response.Status.OK, JobInfoSnapshot.class, SET_WORKFLOW_NOTE, path.build());
    }

    public ItemInfoSnapshot setWorkflowNote(WorkflowNote workflowNote, int jobId, int chunkId, short itemId) throws NullPointerException, JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: setWorkflowNote({}, {}, {});", jobId, chunkId, itemId);
        InvariantUtil.checkNotNullOrThrow(workflowNote, "workflowNote");
        PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId))
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId))
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(itemId));
        return post(workflowNote, Response.Status.OK, ItemInfoSnapshot.class, SET_WORKFLOW_NOTE2, path.build());
    }

    /**
     * Retrieves statuses for all sinks
     *
     * @return a list containing status of sink
     * @throws ProcessingException               on general communication error
     * @throws JobStoreServiceConnectorException on general failure to produce sink status listing
     */
    public List<SinkStatusSnapshot> getSinkStatusList() throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: getSinkStatusList()");
        return get(new GenericType<>() {}, Response.Status.OK, GET_SINK_STATUS_LIST, JobStoreServiceConstants.SINKS_STATUS);
    }

    /**
     * Retrieves status for a specific sinks
     *
     * @param sinkId the id of the sink
     * @return sinkStatusSnapshot
     * @throws ProcessingException               on general communication error
     * @throws JobStoreServiceConnectorException on general failure to produce sink status
     */
    public SinkStatusSnapshot getSinkStatus(int sinkId) throws JobStoreServiceConnectorException {
        log.trace("JobStoreServiceConnector: getSinkStatus({})", sinkId);
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.SINK_STATUS)
                .bind(JobStoreServiceConstants.SINK_ID_VARIABLE, sinkId);
        return get(SinkStatusSnapshot.class, Response.Status.OK, SINK_STATUS, path.build());
    }

    /**
     * Creates rerun task for given job ID
     *
     * @param jobId           ID of job to be rerun
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

    /**
     * Initiates a purge operation.
     * That is: All transient jobs will be deleted, and persisten jobs older than appx 5 years will be "compacted".
     *
     * @throws JobStoreServiceConnectorException on failure to activate purge jobs.
     */
    public void purge() throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        log.trace("JobStoreServiceConnector: purge");
        try {
            final Response response;
            try {
                response = new HttpDelete(httpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(JobStoreServiceConstants.ACTIVATE_JOB_PURGE)
                        .execute();
            } catch (ProcessingException e) {
                throw new JobStoreServiceConnectorException("job-store communication error", e);
            }
            try {
                verifyResponseStatus(response, Response.Status.OK);
            } finally {
                response.close();
            }
        } finally {
            log.debug("JobStoreServiceConnector: purge took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public Client getClient() {
        return httpClient.getClient();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private <R, T> R post(T data, Response.Status expected, GenericType<R> genericType, Metric metric, String... path) throws JobStoreServiceConnectorException {
        StopWatch stopWatch = new StopWatch();
        Metric.StatusTag status = Metric.StatusTag.FAILED;
        try(Response response = new HttpPost(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(data)
                .execute()) {
            verifyResponseStatus(response, expected);
            status = Metric.StatusTag.SUCCESS;
            return readResponseEntity(response, genericType);
        } finally {
            if(metricRegistry != null) metric.simpleTimer(metricRegistry, status.tag).update(Duration.ofMillis(stopWatch.getElapsedTime()));
        }
    }

    private <R, T> R post(T data, Response.Status expected, Class<R> clazz, Metric metric, String... path) throws JobStoreServiceConnectorException {
        StopWatch stopWatch = new StopWatch();
        Metric.StatusTag status = Metric.StatusTag.FAILED;
        try(Response response = new HttpPost(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(data)
                .execute()) {
            verifyResponseStatus(response, expected);
            status = Metric.StatusTag.SUCCESS;
            return readResponseEntity(response, clazz);
        } finally {
            if(metricRegistry != null) metric.simpleTimer(metricRegistry, status.tag).update(Duration.ofMillis(stopWatch.getElapsedTime()));
        }
    }

    private <R> R get(Class<R> clazz, Response.Status expected, Metric metric, String... path) throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        Metric.StatusTag status = Metric.StatusTag.FAILED;
        try (Response response = new HttpGet(httpClient).withBaseUrl(baseUrl).withPathElements(path).execute()) {
            verifyResponseStatus(response, expected);
            status = Metric.StatusTag.SUCCESS;
            return readResponseEntity(response, clazz);
        } finally {
            if(metricRegistry != null) metric.simpleTimer(metricRegistry, status.tag).update(Duration.ofMillis(stopWatch.getElapsedTime()));
        }
    }

    private <R> R get(GenericType<R> genericType, Response.Status expected, Metric metric, String... path) throws JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        Metric.StatusTag status = Metric.StatusTag.FAILED;
        try (Response response = new HttpGet(httpClient).withBaseUrl(baseUrl).withPathElements(path).execute()) {
            verifyResponseStatus(response, expected);
            status = Metric.StatusTag.SUCCESS;
            return readResponseEntity(response, genericType);
        } finally {
            if(metricRegistry != null) metric.simpleTimer(metricRegistry, status.tag).update(Duration.ofMillis(stopWatch.getElapsedTime()));
        }
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
            case PROCESSED:
                return JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
            case DELIVERED:
                return JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
            case PARTITIONED:
                throw new IllegalArgumentException("PARTITIONED is not a valid type");
            default:
                throw new IllegalArgumentException("Chunk.Type could not be identified");
        }
    }

    private String phaseToJobStorePath(State.Phase phase) throws IllegalArgumentException {
        switch (phase) {
            case PARTITIONING:
                return JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED;
            case PROCESSING:
                return JobStoreServiceConstants.CHUNK_ITEM_PROCESSED;
            case DELIVERING:
                return JobStoreServiceConstants.CHUNK_ITEM_DELIVERED;
            default:
                throw new IllegalArgumentException("State.Phase could not be identified");
        }
    }
}
