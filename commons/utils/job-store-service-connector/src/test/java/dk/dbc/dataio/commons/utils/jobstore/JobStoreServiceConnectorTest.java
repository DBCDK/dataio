package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
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
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobStoreServiceConnectorTest {
    private static final String JOB_STORE_URL = "http://dataio/job-store";
    private static final int PART_NUMBER = 414243;
    private static final int JOB_ID = 3;
    private static final int CHUNK_ID = 44;
    private static final short ITEM_ID = 0;
    private static final String ITEM_DATA = "item data";
    private static final ChunkItem CHUNK_ITEM = new ChunkItemBuilder().setData(ITEM_DATA).build();

    private final HttpClient httpClient = mock(HttpClient.class);

    private final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, JOB_STORE_URL, null);


    // ******************************************* add job tests ********************************************

    @Test
    public void addJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.CREATED, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void addJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addJob_onProcessingException_throws() {
        final JobInputStream jobInputStream = getNewJobInputStream();
        when(httpClient.execute(any(HttpPost.class)))
                .thenThrow(new ProcessingException("Connection reset"));

        try {
            jobStoreServiceConnector.addJob(jobInputStream);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void addJob_jobIsAdded_returnsJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final JobInfoSnapshot expectedJobInfoSnapshot = new JobInfoSnapshot();
        final JobInfoSnapshot jobInfoSnapshot = callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.CREATED, expectedJobInfoSnapshot);
        assertThat(jobInfoSnapshot, is(expectedJobInfoSnapshot));
    }

    private JobInfoSnapshot callAddJobWithMockedHttpResponse(JobInputStream jobInputStream, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION)
                .withJsonData(jobInputStream);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.addJob(jobInputStream);
    }

    // **************************************** add accTestJob tests *****************************************

    @Test
    public void addJAccTestJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callAddAccTestJobWithMockedHttpResponse(getNewAccTestJobInputStream(), Response.Status.CREATED, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void addAccTestJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callAddAccTestJobWithMockedHttpResponse(getNewAccTestJobInputStream(), Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addAccTestJob_onProcessingException_throws() {
        final AccTestJobInputStream jobInputStream = getNewAccTestJobInputStream();
        when(httpClient.execute(any(HttpPost.class)))
                .thenThrow(new ProcessingException("Connection reset"));

        try {
            jobStoreServiceConnector.addAccTestJob(jobInputStream);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void addAccTestJob_jobIsAdded_returnsJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final JobInfoSnapshot expectedJobInfoSnapshot = new JobInfoSnapshot();
        final JobInfoSnapshot jobInfoSnapshot = callAddAccTestJobWithMockedHttpResponse(getNewAccTestJobInputStream(), Response.Status.CREATED, expectedJobInfoSnapshot);
        assertThat(jobInfoSnapshot, is(expectedJobInfoSnapshot));
    }

    private JobInfoSnapshot callAddAccTestJobWithMockedHttpResponse(AccTestJobInputStream jobInputStream, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_ACCTESTS)
                .withJsonData(jobInputStream);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.addAccTestJob(jobInputStream);
    }

    // ******************************************* add empty job tests ********************************************

    @Test
    public void addEmptyJob_onUnexpectedStatusCode() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callAddEmptyJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Status code", e.getStatusCode(),
                    is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("JobError entity", e.getJobError(),
                    is(jobError));
        }
    }

    @Test
    public void addEmptyJob_onProcessingException() {
        final JobInputStream jobInputStream = getNewJobInputStream();
        when(httpClient.execute(any(HttpPost.class)))
                .thenThrow(new ProcessingException("Connection reset"));
        try {
            jobStoreServiceConnector.addEmptyJob(jobInputStream);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void addEmptyJob() throws JobStoreServiceConnectorException {
        final JobInfoSnapshot expectedJobInfoSnapshot = new JobInfoSnapshot();
        final JobInfoSnapshot jobInfoSnapshot = callAddEmptyJobWithMockedHttpResponse(
                getNewJobInputStream(), Response.Status.CREATED, expectedJobInfoSnapshot);
        assertThat(jobInfoSnapshot, is(expectedJobInfoSnapshot));
    }

    private JobInfoSnapshot callAddEmptyJobWithMockedHttpResponse(JobInputStream jobInputStream,
                                                                  Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_EMPTY)
                .withJsonData(jobInputStream);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.addEmptyJob(jobInputStream);
    }

    // ******************************************* add chunk tests *******************************************

    @Test
    public void addChunk_chunkTypePartitioned_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        assertThat(() -> jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void addChunk_chunkTypeProcessed_chunkIsAdded() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot expected = new JobInfoSnapshot().withJobId(JOB_ID);
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkWithMockedHttpResponse(
                chunk, Response.Status.CREATED, expected);

        assertThat(jobInfoSnapshot, is(expected));
    }

    @Test
    public void addChunk_chunkTypeDelivering_chunkIsAdded() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).build();
        final JobInfoSnapshot expected = new JobInfoSnapshot().withJobId(JOB_ID);
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkWithMockedHttpResponse(
                chunk, Response.Status.CREATED, expected);

        assertThat(jobInfoSnapshot, is(expected));
    }

    @Test
    public void addChunk_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobError jobError = new JobError(JobError.Code.INVALID_CHUNK_IDENTIFIER, "description", null);
        try {
            callAddChunkWithMockedHttpResponse(chunk, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addChunk_acceptedRequestResponse_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        try {
            callAddChunkWithMockedHttpResponse(chunk, Response.Status.ACCEPTED, null);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.ACCEPTED.getStatusCode()));
        }
    }

    @Test
    public void addChunk_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).build();
        assertThat(() -> callAddChunkWithMockedHttpResponse(chunk, Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    private JobInfoSnapshot callAddChunkWithMockedHttpResponse(Chunk chunk, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final String[] path = buildAddChunkPath(chunk.getJobId(), chunk.getChunkId(), getAddChunkBasePath(chunk));

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path)
                .withJsonData(chunk);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.addChunk(chunk, chunk.getJobId(), chunk.getChunkId());
    }

    // ************************************* add chunk ignore duplicates tests **************************************

    @Test
    public void addChunkIgnoreDuplicates_chunkTypePartitioned_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        assertThat(() -> jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, JOB_ID, CHUNK_ID), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void addChunkIgnoreDuplicates_acceptedRequestResponse_lookupJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot expected = new JobInfoSnapshot().withJobId(JOB_ID);
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkIgnoreDuplicatesWithMockedHttpResponse(
                chunk, Response.Status.ACCEPTED, expected);

        assertThat(jobInfoSnapshot, is(expected));
    }

    @Test
    public void addChunkIgnoreDuplicates_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobError jobError = new JobError(JobError.Code.INVALID_CHUNK_IDENTIFIER, "description", null);
        try {
            callAddChunkIgnoreDuplicatesWithMockedHttpResponse(chunk, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addChunkIgnoreDuplicates_noneDuplicateChunk_chunkIsAdded() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot expected = new JobInfoSnapshot().withJobId(JOB_ID);
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkIgnoreDuplicatesWithMockedHttpResponse(
                chunk, Response.Status.CREATED, expected);

        assertThat(jobInfoSnapshot, is(expected));
    }

    private JobInfoSnapshot callAddChunkIgnoreDuplicatesWithMockedHttpResponse(Chunk chunk, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        when(httpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), Collections.singletonList(returnValue)));

        return jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
    }

    // ******************************************* listJobs() tests *******************************************

    @Test
    public void listJobs_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void listJobs_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void listJobs_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> snapshots = callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, Collections.emptyList());
        assertThat(snapshots, is(Collections.emptyList()));
    }

    @Test
    public void listJobs_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> expected = Collections.singletonList(new JobInfoSnapshot());
        final List<JobInfoSnapshot> snapshots = callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, expected);
        assertThat(snapshots, is(expected));
    }

    private List<JobInfoSnapshot> callListJobsWithMockedHttpResponse(JobListCriteria criteria, Response.Status statusCode, List<JobInfoSnapshot> responseEntity)
            throws JobStoreServiceConnectorException {

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES)
                .withJsonData(criteria);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), responseEntity));

        return jobStoreServiceConnector.listJobs(criteria);
    }

    // ******************************************* listItems() tests *******************************************

    @Test
    public void listItems_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void listItems_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void listItems_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<ItemInfoSnapshot> snapshots = callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, Collections.emptyList());
        assertThat(snapshots, is(Collections.emptyList()));
    }

    @Test
    public void listItems_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<ItemInfoSnapshot> expected = Collections.singletonList(new ItemInfoSnapshotBuilder().build());
        final List<ItemInfoSnapshot> snapshots = callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, expected);
        assertThat(snapshots, is(expected));
    }

    private List<ItemInfoSnapshot> callListItemsWithMockedHttpResponse(ItemListCriteria criteria, Response.Status statusCode, List<ItemInfoSnapshot> responseEntity)
            throws JobStoreServiceConnectorException {

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES)
                .withJsonData(criteria);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), responseEntity));

        return jobStoreServiceConnector.listItems(criteria);
    }

    // ******************************************* count tests *******************************************

    @Test
    public void countJobs_parseNumber() throws Exception {
        final long expected = 123;
        final JobListCriteria criteria = new JobListCriteria();

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT)
                .withJsonData(criteria);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(200, expected));

        final long count = jobStoreServiceConnector.countJobs(criteria);
        assertThat(count, is(expected));
    }

    @Test
    public void countItems_parseNumber() throws Exception {
        final long expected = 123;
        final ItemListCriteria criteria = new ItemListCriteria();

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT)
                .withJsonData(criteria);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(200, expected));

        final long count = jobStoreServiceConnector.countItems(criteria);
        assertThat(count, is(expected));
    }

    // ******************************************* getCachedFlow tests *******************************************

    @Test
    public void getCachedFlow_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callGetCachedFlowWithMockedHttpResponse(JOB_ID, Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void getCachedFlow_jobEntityFound_returns() throws JobStoreServiceConnectorException {
        final Flow expected = new FlowBuilder().build();
        final Flow flow = callGetCachedFlowWithMockedHttpResponse(JOB_ID, Response.Status.OK, expected);
        assertThat("Flow", flow, is(expected));
    }

    private Flow callGetCachedFlowWithMockedHttpResponse(int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_CACHED_FLOW)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path.build());

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.getCachedFlow(jobId);
    }

    // ******************************************* getChunkItem() tests *******************************************

    @Test
    public void getChunkItem_phaseIsNull_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> jobStoreServiceConnector.getChunkItem(JOB_ID, CHUNK_ID, ITEM_ID, null),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void getChunkItem_notFoundResponse_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callGetChunkItemWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING, Response.Status.NOT_FOUND, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getChunkItem_partitioningPhase_partitionedDataReturned() throws JobStoreServiceConnectorException {
        final ChunkItem chunkItem = callGetChunkItemWithMockedHttpResponse(
                JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING, Response.Status.OK, CHUNK_ITEM);

        assertThat(chunkItem, is(CHUNK_ITEM));
    }

    @Test
    public void getChunkItem_processingPhase_processedDataReturned() throws JobStoreServiceConnectorException {
        final ChunkItem chunkItem = callGetChunkItemWithMockedHttpResponse(
                JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING, Response.Status.OK, CHUNK_ITEM);

        assertThat(chunkItem, is(CHUNK_ITEM));
    }

    @Test
    public void getChunkItem_deliveredPhase_deliveredDataReturned() throws JobStoreServiceConnectorException {
        final ChunkItem chunkItem = callGetChunkItemWithMockedHttpResponse(
                JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.DELIVERING, Response.Status.OK, CHUNK_ITEM);

        assertThat(chunkItem, is(CHUNK_ITEM));
    }

    private ChunkItem callGetChunkItemWithMockedHttpResponse(int jobId, int chunkId, short itemId, State.Phase phase, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final String basePath;
        switch (phase) {
            case PARTITIONING:
                basePath = JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED;
                break;
            case PROCESSING:
                basePath = JobStoreServiceConstants.CHUNK_ITEM_PROCESSED;
                break;
            case DELIVERING:
                basePath = JobStoreServiceConstants.CHUNK_ITEM_DELIVERED;
                break;
            default:
                basePath = "";
        }

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(buildGetChunkItemPath(jobId, chunkId, itemId, basePath));

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.getChunkItem(jobId, chunkId, itemId, phase);
    }

    // ***************************************** getNextItemData() tests *****************************************

    @Test
    public void getProcessedNextResult_notFoundResponse_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callProcessedNextResultWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.NOT_FOUND, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getProcessedNextResult_itemFound_returnsProcessedNextResult() throws JobStoreServiceConnectorException {
        final ChunkItem chunkItem = callProcessedNextResultWithMockedHttpResponse(
                JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.OK, CHUNK_ITEM);

        assertThat(chunkItem, is(CHUNK_ITEM));
    }

    private ChunkItem callProcessedNextResultWithMockedHttpResponse(int jobId, int chunkId, short itemId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(buildGetChunkItemPath(jobId, chunkId, itemId, JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT));

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.getProcessedNextResult(jobId, chunkId, itemId);
    }

    // ******************************************* listJobNotificationsForJob() tests *******************************************

    @Test
    public void listJobNotificationsForJob_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<Notification> snapshots = callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, Collections.emptyList());
        assertThat(snapshots, is(Collections.emptyList()));
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final Notification jobNotification = new Notification()
                .withId(234)
                .withTimeOfCreation(new Date())
                .withTimeOfLastModification(new Date())
                .withType(Notification.Type.JOB_CREATED)
                .withStatus(Notification.Status.COMPLETED)
                .withStatusMessage("status message")
                .withDestination("destination")
                .withContent("content")
                .withJobId(345);
        final List<Notification> expected = Collections.singletonList(jobNotification);
        final List<Notification> snapshots = callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, expected);
        assertThat(snapshots, is(expected));
    }

    private List<Notification> callListJobNotificationForJobIdWithMockedHttpResponse(int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_NOTIFICATIONS)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path.build());

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.listJobNotificationsForJob(jobId);
    }

    // ********************************************** getSinkStatusList tests ***********************************************

    @Test
    public void getSinkStatusList_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callGetSinkStatusListWithMockedHttpResponse(Response.Status.INTERNAL_SERVER_ERROR, null),
                isThrowing(JobStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getSinkStatusList_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callGetSinkStatusListWithMockedHttpResponse(Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void getSinkStatusList_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<SinkStatusSnapshot> expected = Collections.singletonList(new SinkStatusSnapshot().withName("name").withSinkType(SinkContent.SinkType.ES).withNumberOfChunks(1).withNumberOfJobs(1));
        final List<SinkStatusSnapshot> snapshots = callGetSinkStatusListWithMockedHttpResponse(Response.Status.OK, expected);
        assertThat(snapshots, is(expected));
    }

    private List<SinkStatusSnapshot> callGetSinkStatusListWithMockedHttpResponse(Response.Status statusCode, List<SinkStatusSnapshot> responseEntity)
            throws JobStoreServiceConnectorException {

        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.SINKS_STATUS);

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), responseEntity));

        return jobStoreServiceConnector.getSinkStatusList();
    }

    // ********************************************* getSinkStatus tests ****************************************************

    @Test
    public void getSinkStatus_returns() throws JobStoreServiceConnectorException {
        final SinkStatusSnapshot expected = new SinkStatusSnapshot().withName("name").withSinkType(SinkContent.SinkType.ES);
        final SinkStatusSnapshot snapshot = callGetSinkStatusWithMockedHttpResponse(1, Response.Status.OK, expected);
        assertThat(snapshot, is(expected));
    }

    private SinkStatusSnapshot callGetSinkStatusWithMockedHttpResponse(int sinkId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.SINK_STATUS)
                .bind(JobStoreServiceConstants.SINK_ID_VARIABLE, sinkId);
        final HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path.build());

        when(httpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.getSinkStatus(sinkId);
    }

    // ******************************************* addNotification() tests *******************************************

    @Test
    public void addNotification_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callAddNotificationWithMockedHttpResponse(getAddNotificationRequest(), Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void addNotification_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callAddNotificationWithMockedHttpResponse(getAddNotificationRequest(), Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addNotification_notificationIsAdded_returnsJobNotification() throws JobStoreServiceConnectorException {
        final Notification expected = new Notification();
        final Notification jobNotification = callAddNotificationWithMockedHttpResponse(getAddNotificationRequest(), Response.Status.OK, expected);
        assertThat(jobNotification, is(expected));
    }

    private Notification callAddNotificationWithMockedHttpResponse(AddNotificationRequest request, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(JobStoreServiceConstants.NOTIFICATIONS)
                .withJsonData(request);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.addNotification(request);
    }

    // ******************************************* set workflowNote on job tests ********************************************

    @Test
    public void setWorkflowNoteOnJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(), JOB_ID, Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void setWorkflowNoteOnJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(), JOB_ID, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void setWorkflowNoteOnJob_workflowNoteIsAdded_returnsJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final JobInfoSnapshot expected = new JobInfoSnapshot().withWorkflowNote(workflowNote);
        final JobInfoSnapshot snapshot = callSetWorkflowNoteWithMockedHttpResponse(workflowNote, JOB_ID, Response.Status.OK, expected);
        assertThat(snapshot, is(expected));
    }

    private JobInfoSnapshot callSetWorkflowNoteWithMockedHttpResponse(WorkflowNote workflowNote, int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path.build())
                .withJsonData(workflowNote);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.setWorkflowNote(workflowNote, jobId);
    }

    // ******************************************* set workflowNote on item tests ********************************************

    @Test
    public void setWorkflowNoteOnItem_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        assertThat(() -> callSetWorkflowNoteWithMockedHttpResponse(
                        new WorkflowNoteBuilder().build(), JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.OK, null),
                isThrowing(JobStoreServiceConnectorException.class));
    }

    @Test
    public void setWorkflowNoteOnItem_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callSetWorkflowNoteWithMockedHttpResponse(
                    new WorkflowNoteBuilder().build(), JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void setWorkflowNoteOnItem_workflowNoteIsAdded_returnsItemInfoSnapshot() throws JobStoreServiceConnectorException {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final ItemInfoSnapshot expected = new ItemInfoSnapshotBuilder().setWorkflowNote(workflowNote).build();
        final ItemInfoSnapshot snapshot = callSetWorkflowNoteWithMockedHttpResponse(
                workflowNote, JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.OK, expected);
        assertThat(snapshot, is(expected));
    }

    private ItemInfoSnapshot callSetWorkflowNoteWithMockedHttpResponse(WorkflowNote workflowNote, int jobId, int chunkId, short itemId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId))
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId))
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(itemId));

        final HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(JOB_STORE_URL)
                .withPathElements(path.build())
                .withJsonData(workflowNote);

        when(httpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        return jobStoreServiceConnector.setWorkflowNote(workflowNote, jobId, chunkId, itemId);
    }

    private static JobInputStream getNewJobInputStream() {
        try {
            final JobSpecification jobSpecification = new JobSpecification();
            return new JobInputStream(jobSpecification, false, PART_NUMBER);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private static AccTestJobInputStream getNewAccTestJobInputStream() {
        try {
            final JobSpecification jobSpecification = new JobSpecification();
            final Flow flow = new FlowBuilder().build();
            return new AccTestJobInputStream(jobSpecification, flow, RecordSplitterConstants.RecordSplitter.XML);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private String getAddChunkBasePath(Chunk chunk) {
        final String basePath;
        switch (chunk.getType()) {
            case PROCESSED:
                basePath = JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
                break;
            case DELIVERED:
                basePath = JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
                break;
            default:
                basePath = "";
        }
        return basePath;
    }

    private String[] buildAddChunkPath(long jobId, long chunkId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId).build();
    }

    private String[] buildGetChunkItemPath(int jobId, int chunkId, short itemId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId)
                .build();
    }

    private static AddNotificationRequest getAddNotificationRequest() {
        try {
            final InvalidTransfileNotificationContext context = new InvalidTransfileNotificationContext("name", "content", "cause");
            return new AddNotificationRequest("mail@company.com", context, Notification.Type.INVALID_TRANSFILE);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
