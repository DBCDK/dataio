package dk.dbc.dataio.jobstore.service.ejb;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.hazelcast.test.HazelcastTestSupport;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsProducer;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import jakarta.jms.JMSContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobsBeanTest extends HazelcastTestSupport {
    private final static String LOCATION = "location";
    private final static int PART_NUMBER = 2535678;
    private final static int JOB_ID = 42;
    private final static int CHUNK_ID = 10;
    private final static short ITEM_ID = 0;
    private UriInfo mockedUriInfo;
    private JobsBean jobsBean;
    private JSONBContext jsonbContext;

    private final JobSchedulerBean jobSchedulerBean = mock(JobSchedulerBean.class);
    private final JMSContext jmsContext = mock(JMSContext.class);
    private final MockedJmsProducer jmsProducer = new MockedJmsProducer();

    @org.junit.Before
    public void setup() throws URISyntaxException, IOException {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("hz-data.xml")) {
            Hazelcast.testInstance(createHazelcastInstance(Hazelcast.makeConfig(is)));
            JobsBean.testingUpdateStaticTestHazelcast();
        }

        initializeJobsBean();
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);

        URI uri = new URI(LOCATION);
        when(mockedUriBuilder.build()).thenReturn(uri);

    }

    @org.junit.After
    public void clearMocks() {
        jmsProducer.clearMessages();
        Hazelcast.shutdownNode();
    }


    // ************************************* ADD JOB TESTS **************************************************************

    @org.junit.Test
    public void addJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception {
        JobSpecification jobSpecification = new JobSpecification();
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStore.addAndScheduleJob(any(JobInputStream.class))).thenThrow(new JobStoreException("Error"));

        assertThrows(JobStoreException.class, () -> jobsBean.addJob(mockedUriInfo, jobInputStreamJson));
    }

    @org.junit.Test
    public void addJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Response response = jobsBean.addJob(mockedUriInfo, "invalid JSON");

        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobInfoSnapshot() throws Exception {
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withSpecification(new JobSpecification()).withJobId(JOB_ID);
        JobInputStream jobInputStream = new JobInputStream(jobInfoSnapshot.getSpecification(), false, PART_NUMBER);
        String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStore.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

        Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat(returnedJobInfoSnapshot.hasFatalError(), is(false));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat(returnedJobInfoSnapshot.getSpecification(), is(jobInfoSnapshot.getSpecification()));
        assertThat(returnedJobInfoSnapshot.getState(), is(jobInfoSnapshot.getState()));
        assertThat(returnedJobInfoSnapshot.getFlowStoreReferences(), is(jobInfoSnapshot.getFlowStoreReferences()));
    }

    // ********************************** ADD ACCTEST JOB TESTS ********************************************************

    @org.junit.Test
    public void addAccTestJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception {
        AccTestJobInputStream jobInputStream = new AccTestJobInputStream(
                new JobSpecification(),
                new FlowBuilder().build(),
                RecordSplitter.XML);

        when(jobsBean.jobStore.addAndScheduleAccTestJob(any(AccTestJobInputStream.class))).thenThrow(new JobStoreException("Error"));
        assertThat(() -> jobsBean.addAccTestJob(mockedUriInfo, asJson(jobInputStream)), isThrowing(JobStoreException.class));
    }

    @org.junit.Test
    public void addAccTestJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Response response = jobsBean.addJob(mockedUriInfo, "invalid JSON");

        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void addAccTestJob_returnsResponseWithHttpStatusCreated_returnsJobInfoSnapshot() throws Exception {
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withSpecification(new JobSpecification()).withJobId(JOB_ID);
        Flow flow = new FlowBuilder().build();
        AccTestJobInputStream jobInputStream = new AccTestJobInputStream(jobInfoSnapshot.getSpecification(), flow, RecordSplitter.DANMARC2_LINE_FORMAT);
        String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStore.addAndScheduleAccTestJob(any(AccTestJobInputStream.class))).thenReturn(jobInfoSnapshot);

        Response response = jobsBean.addAccTestJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat(returnedJobInfoSnapshot.hasFatalError(), is(false));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat(returnedJobInfoSnapshot.getSpecification(), is(jobInfoSnapshot.getSpecification()));
        assertThat(returnedJobInfoSnapshot.getState(), is(jobInfoSnapshot.getState()));
        assertThat(returnedJobInfoSnapshot.getFlowStoreReferences(), is(jobInfoSnapshot.getFlowStoreReferences()));
    }

    // ************************************* ADD EMPTY JOB TESTS *******************************************************

    @org.junit.Test
    public void addEmptyJob_onInvalidJson() throws JSONBException, JobStoreException {
        assertBadRequestResponse(jobsBean.addEmptyJob(mockedUriInfo, "invalid JSON"),
                JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void addEmptyJob_onInvalidInputException() throws JSONBException, JobStoreException {
        JobSpecification jobSpecification = new JobSpecification()
                .withType(JobSpecification.Type.PERIODIC)
                .withDataFile(FileStoreUrn.EMPTY_JOB_FILE.toString());
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        when(jobsBean.jobStore.addAndScheduleEmptyJob(any(JobInputStream.class)))
                .thenThrow(new InvalidInputException("Error", new JobError(JobError.Code.INVALID_INPUT)));

        assertBadRequestResponse(jobsBean.addEmptyJob(mockedUriInfo, asJson(jobInputStream)),
                JobError.Code.INVALID_INPUT);
    }

    @org.junit.Test
    public void addEmptyJob_onIllegalJobType() throws JSONBException, JobStoreException {
        JobSpecification jobSpecification = new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.EMPTY_JOB_FILE.toString());
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        assertBadRequestResponse(jobsBean.addEmptyJob(mockedUriInfo, asJson(jobInputStream)),
                JobError.Code.INVALID_JOB_SPECIFICATION);
    }

    @org.junit.Test
    public void addEmptyJob_onIllegalDatafile() throws JSONBException, JobStoreException {
        JobSpecification jobSpecification = new JobSpecification()
                .withType(JobSpecification.Type.PERIODIC)
                .withDataFile("illegal");
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        assertBadRequestResponse(jobsBean.addEmptyJob(mockedUriInfo, asJson(jobInputStream)),
                JobError.Code.INVALID_JOB_SPECIFICATION);
    }

    @org.junit.Test
    public void addEmptyJob_onSuccess() throws JobStoreException, JSONBException {
        JobSpecification jobSpecification = new JobSpecification()
                .withType(JobSpecification.Type.PERIODIC)
                .withDataFile(FileStoreUrn.EMPTY_JOB_FILE.toString());
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot()
                .withSpecification(jobSpecification)
                .withJobId(JOB_ID);

        when(jobsBean.jobStore.addAndScheduleEmptyJob(any(JobInputStream.class)))
                .thenReturn(jobInfoSnapshot);

        Response response = jobsBean.addEmptyJob(mockedUriInfo, asJson(jobInputStream));
        assertThat("response status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat("response location", response.getLocation().toString(), is(LOCATION));
        assertThat("response has entity", response.hasEntity(), is(true));

        JobInfoSnapshot responseJobInfoSnapshot =
                jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat("response snapshot", responseJobInfoSnapshot, is(notNullValue()));
        assertThat("snapshot has fatal error", responseJobInfoSnapshot.hasFatalError(), is(false));
        assertThat("snapshot job ID", responseJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
    }

    // ************************************* ADD CHUNK TESTS **************************************************************

    @org.junit.Test
    public void addChunk_jobIsUpdated_jobInfoSnapShotReturned() throws Exception {
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withJobId(JOB_ID);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        when(jobsBean.jobStore.addChunk(any(Chunk.class))).thenReturn(jobInfoSnapshot);

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED, chunk);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @org.junit.Test
    public void addChunkProcessed_messageIsSentToSink() throws Exception {

        when(jmsContext.createTextMessage(any(String.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    MockedJmsTextMessage message = new MockedJmsTextMessage();
                    message.setText((String) args[0]);
                    return message;
                })
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    MockedJmsTextMessage message = new MockedJmsTextMessage();
                    message.setText((String) args[0]);
                    return message;
                });
        jobsBean.jobSchedulerBean = jobSchedulerBean;

        ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.singletonList(item)).build();
        String jsonChunk = new JSONBContext().marshall(chunk);

        SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);
        Sink sink = new SinkBuilder().build();
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        JobEntity jobEntity = new JobEntity();
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setFlowStoreReferences(flowStoreReferences);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                new FlowStoreReference(42, 1, "test-binder"));

        when(jobsBean.jobStoreRepository.getJobEntityById(anyInt())).thenReturn(jobEntity);
        when(sinkCacheEntity.getSink()).thenReturn(sink);

        // Subject Under Test
        Response response = jobsBean.addChunkProcessed(mockedUriInfo, jsonChunk, chunk.getJobId(), chunk.getChunkId());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        verify(jobSchedulerBean, atLeastOnce()).chunkProcessingDone(any(Chunk.class));
    }

    @org.junit.Test
    public void addChunk_invalidJobId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId() + 1, chunk.getChunkId(), Chunk.Type.PROCESSED, chunk);
        assertBadRequestResponse(response, JobError.Code.INVALID_JOB_IDENTIFIER);
    }

    @org.junit.Test
    public void addChunk_invalidChunkId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId() + 1, Chunk.Type.PROCESSED, chunk);
        assertBadRequestResponse(response, JobError.Code.INVALID_CHUNK_IDENTIFIER);
    }

    @org.junit.Test
    public void addChunk_invalidChunkType_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId() + 1, Chunk.Type.DELIVERED, chunk);
        assertBadRequestResponse(response, JobError.Code.INVALID_CHUNK_IDENTIFIER);
    }

    @org.junit.Test
    public void addChunk_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Response response = jobsBean.addChunkDelivered(mockedUriInfo, "invalid json", JOB_ID, CHUNK_ID);

        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void addChunk_invalidInput_returnsResponseWithHttpStatusBadRequest() throws Exception {
        JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, "illegal number of items", "stack trace");
        InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        when(jobsBean.jobStore.addChunk(any(Chunk.class))).thenThrow(invalidInputException);

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED, chunk);
        assertBadRequestResponse(response, JobError.Code.ILLEGAL_CHUNK);
    }

    @org.junit.Test
    public void addChunk_onFailureToUpdateJob_throwsJobStoreException() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        when(jobsBean.jobStore.addChunk(any(Chunk.class))).thenThrow(new JobStoreException("Error"));

        assertThrows(JobStoreException.class, () -> jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED, chunk));
    }

    @org.junit.Test
    public void addChunk_duplicateChunk_throwsJobStoreException() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        when(jobsBean.jobStore.addChunk(any(Chunk.class))).thenThrow(new DuplicateChunkException("Error", null));

        Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    // ************************************* listJobs() tests **********************************************************

    @org.junit.Test
    public void listJobs_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreRepository.listJobs(any(JobListCriteria.class))).thenReturn(Collections.emptyList());

        Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertOkResponse(response);

        CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @org.junit.Test
    public void listJobs_unableToUnmarshallJobListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        Response response = jobsBean.listJobs("Invalid JSON");
        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void listJobs_jobStoreReturnsList_returnsStatusOkResponseWithJobInfoSnapshotList() throws JSONBException {
        List<JobInfoSnapshot> expectedJobInfoSnapshots = new ArrayList<>();
        expectedJobInfoSnapshots.add(new JobInfoSnapshot());
        when(jobsBean.jobStoreRepository.listJobs(any(JobListCriteria.class))).thenReturn(expectedJobInfoSnapshots);

        Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertOkResponse(response);

        CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots size", jobInfoSnapshots.size(), is(expectedJobInfoSnapshots.size()));
        assertThat("JobInfoSnapshots element", jobInfoSnapshots.get(0).getJobId(), is(expectedJobInfoSnapshots.get(0).getJobId()));
    }

    // ************************************* listItems() tests **********************************************************

    @org.junit.Test
    public void listItems_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreRepository.listItems(any(ItemListCriteria.class))).thenReturn(Collections.emptyList());

        Response response = jobsBean.listItems(asJson(new ItemListCriteria()));
        assertOkResponse(response);

        CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @org.junit.Test
    public void listItems_unableToUnmarshallItemListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        Response response = jobsBean.listJobs("Invalid JSON");
        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void listItems_jobStoreReturnsList_returnsStatusOkResponseWithItemInfoSnapshotList() throws JSONBException {
        List<ItemInfoSnapshot> expectedItemInfoSnapshots = new ArrayList<>();
        expectedItemInfoSnapshots.add(new ItemInfoSnapshotBuilder().build());
        when(jobsBean.jobStoreRepository.listItems(any(ItemListCriteria.class))).thenReturn(expectedItemInfoSnapshots);

        Response response = jobsBean.listItems(asJson(new JobListCriteria()));
        assertOkResponse(response);

        CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots size", itemInfoSnapshots.size(), is(expectedItemInfoSnapshots.size()));
        assertThat("ItemInfoSnapshots element", itemInfoSnapshots.get(0).getItemId(), is(expectedItemInfoSnapshots.get(0).getItemId()));
    }

    // ************************************* countItems() tests **********************************************************

    @org.junit.Test
    public void countItems_jobStoreReturnsItemCount_returnsStatusOkResponseWithCountAsEntity() throws JSONBException {
        when(jobsBean.jobStoreRepository.countItems(any(ItemListCriteria.class))).thenReturn(110L);

        Response response = jobsBean.countItems(asJson(new ItemListCriteria()));
        assertOkResponse(response);
        long count = jsonbContext.unmarshall((String) response.getEntity(), Long.class);
        assertThat("Count", count, is(110L));
    }

    @org.junit.Test
    public void countItems_unableToUnmarshallItemListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        Response response = jobsBean.countJobs("Invalid JSON");
        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    // *************************************************** getCachedFlow() tests *******************************************************

    @org.junit.Test
    public void getCachedFlow_jobEntityFound_returnsStatusOkResponseWithFlow() throws JSONBException, JobStoreException {
        when(jobsBean.jobStoreRepository.getCachedFlow(JOB_ID)).thenReturn(new FlowBuilder().build());

        // Subject under test
        Response response = jobsBean.getCachedFlow(JOB_ID);

        // Verification
        assertOkResponse(response);
        Flow chachedFlow = jsonbContext.unmarshall((String) response.getEntity(), Flow.class);
        assertThat("Flow not null", chachedFlow, not(nullValue()));
    }


    @org.junit.Test
    public void getCachedFlow_jobEntityNotFound_returnsStatusBadRequestResponseWithJobError() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);
        when(jobsBean.jobStoreRepository.getCachedFlow(JOB_ID)).thenThrow(invalidInputException);

        // Subject under test
        Response response = jobsBean.getCachedFlow(JOB_ID);

        // Verification
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError not null", jobErrorReturned, is(notNullValue()));
    }

    // ************************************* getChunkItemForPhase() tests ***********************************************************

    @org.junit.Test
    public void getChunkItemForPhase_itemEntityLocated_returnsStatusOkResponseWithDataAsString() throws JSONBException, JobStoreException {
        ChunkItem chunkItem = new ChunkItemBuilder().setData("Item data").build();

        when(jobsBean.jobStoreRepository.getChunkItemForPhase(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenReturn(chunkItem);

        assertOkResponse(jobsBean.getChunkItemForPhase(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING));
    }


    @org.junit.Test
    public void getChunkItemForPhase_itemEntityNotFound_returnsStatusNotFoundResponse() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreRepository.getChunkItemForPhase(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenThrow(invalidInputException);

        assertNotFoundResponse(jobsBean.getChunkItemForPhase(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING));
    }

    // ************************************* getProcessedNextResult() tests ***********************************************************

    @org.junit.Test
    public void getProcessedNextResult_itemEntityLocated_returnsStatusOkResponseWithDataAsString() throws JSONBException, JobStoreException {
        ChunkItem chunkItem = new ChunkItemBuilder().setData("Next data").build();

        when(jobsBean.jobStoreRepository.getNextProcessingOutcome(anyInt(), anyInt(), anyShort())).thenReturn(chunkItem);

        assertOkResponse(jobsBean.getProcessedNextResult(JOB_ID, CHUNK_ID, ITEM_ID));
    }


    @org.junit.Test
    public void getProcessedNextResult_itemEntityNotFound_returnsStatusNotFoundResponse() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreRepository.getNextProcessingOutcome(anyInt(), anyInt(), anyShort())).thenThrow(invalidInputException);

        assertNotFoundResponse(jobsBean.getProcessedNextResult(JOB_ID, CHUNK_ID, ITEM_ID));
    }

    @org.junit.Test
    public void getNotificationsForJob_repositoryReturnsList_returnsStatusOkResponseWithJsonEntity() throws JSONBException {
        when(jobsBean.jobNotificationRepository.getNotificationsForJob(JOB_ID)).thenReturn(
                Collections.singletonList(new NotificationEntity()));

        Response response = jobsBean.getNotificationsForJob(JOB_ID);
        assertOkResponse(response);
        List<Notification> notifications = jsonbContext.unmarshall(response.getEntity().toString(),
                jsonbContext.getTypeFactory().constructCollectionType(List.class, Notification.class));
        assertThat("Number of notifications returned", notifications.size(), is(1));
    }

    // ************************************* setWorkflowNote() on job tests **********************************************************

    @org.junit.Test
    public void setWorkflowNote_setWorkflowNoteOnJobFailure_throwsJobStoreException() throws Exception {
        when(jobsBean.jobStore.setWorkflowNote(any(WorkflowNote.class), anyInt())).thenThrow(new JobStoreException("Error"));
        assertThrows(JobStoreException.class, () -> jobsBean.setWorkflowNote(asJson(new WorkflowNoteBuilder().build()), JOB_ID));
    }

    @org.junit.Test
    public void setWorkflowNoteOnJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Response response = jobsBean.setWorkflowNote("invalid JSON", JOB_ID);
        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void setWorkflowNote_returnsResponseWithHttpStatusOk_returnsJobInfoSnapshot() throws Exception {
        WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot().withJobId(JOB_ID).withWorkflowNote(workflowNote);

        when(jobsBean.jobStore.setWorkflowNote(any(WorkflowNote.class), eq(JOB_ID))).thenReturn(jobInfoSnapshot);

        Response response = jobsBean.setWorkflowNote(asJson(workflowNote), JOB_ID);
        assertOkResponse(response);

        JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat("JobInfoSnapshot not null", returnedJobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.jobId", returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("JobInfoSnapshot.workflowNote", returnedJobInfoSnapshot.getWorkflowNote(), is(jobInfoSnapshot.getWorkflowNote()));
    }

    // ************************************* setWorkflowNote() on item tests **********************************************************

    @org.junit.Test
    public void setWorkflowNote_setWorkflowNoteOnItemFailure_throwsJobStoreException() throws Exception {
        when(jobsBean.jobStore.setWorkflowNote(any(WorkflowNote.class), anyInt(), anyInt(), anyShort())).thenThrow(new JobStoreException("Error"));
        assertThrows(JobStoreException.class, () -> jobsBean.setWorkflowNote(asJson(new WorkflowNoteBuilder().build()), JOB_ID, CHUNK_ID, ITEM_ID));
    }

    @org.junit.Test
    public void setWorkflowNoteOnItem_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        Response response = jobsBean.setWorkflowNote("invalid JSON", JOB_ID, CHUNK_ID, ITEM_ID);
        assertBadRequestResponse(response, JobError.Code.INVALID_JSON);
    }

    @org.junit.Test
    public void setWorkflowNote_returnsResponseWithHttpStatusOk_returnsItemInfoSnapshot() throws Exception {
        WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        ItemInfoSnapshot itemInfoSnapshot = new ItemInfoSnapshotBuilder().setJobId(JOB_ID).setWorkflowNote(workflowNote).build();

        when(jobsBean.jobStore.setWorkflowNote(any(WorkflowNote.class), eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID))).thenReturn(itemInfoSnapshot);

        Response response = jobsBean.setWorkflowNote(asJson(workflowNote), JOB_ID, CHUNK_ID, ITEM_ID);
        assertOkResponse(response);

        ItemInfoSnapshot returnedItemInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), ItemInfoSnapshot.class);
        assertThat("ItemInfoSnapshot not null", returnedItemInfoSnapshot, is(notNullValue()));
        assertThat("ItemInfoSnapshot.jobId", returnedItemInfoSnapshot.getJobId(), is(itemInfoSnapshot.getJobId()));
        assertThat("ItemInfoSnapshot.chunkId", returnedItemInfoSnapshot.getChunkId(), is(itemInfoSnapshot.getChunkId()));
        assertThat("ItemInfoSnapshot.itemId", returnedItemInfoSnapshot.getItemId(), is(itemInfoSnapshot.getItemId()));
        assertThat("ItemInfoSnapshot.workflowNote", returnedItemInfoSnapshot.getWorkflowNote(), is(itemInfoSnapshot.getWorkflowNote()));
    }

    private void initializeJobsBean() {
        jobsBean = new JobsBean();
        jobsBean.jobStore = mock(PgJobStore.class);
        jobsBean.jobStoreRepository = mock(PgJobStoreRepository.class);
        jobsBean.jobNotificationRepository = mock(JobNotificationRepository.class);
    }

    private String asJson(Object object) throws JSONBException {
        return jsonbContext.marshall(object);
    }

    private void assertOkResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
    }

    private void assertNotFoundResponse(Response response) {
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(false));
    }

    private void assertBadRequestResponse(Response response, JobError.Code code) throws JSONBException {
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(code));
    }

    public static void notAborted(int jobId, Consumer<MockedStatic<JobsBean>> block) {
        try(MockedStatic<JobsBean> jobsBeanMock = mockStatic(JobsBean.class)) {
            jobsBeanMock.when(() -> JobsBean.isAborted(jobId)).thenReturn(false);
            block.accept(jobsBeanMock);
        }
    }
}
