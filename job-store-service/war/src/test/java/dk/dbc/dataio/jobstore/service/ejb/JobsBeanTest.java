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

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsProducer;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobsBeanTest {
    private final static String LOCATION = "location";
    private final static int PART_NUMBER = 2535678;
    private final static int JOB_ID = 42;
    private final static int CHUNK_ID = 10;
    private final static short ITEM_ID = 0;
    private UriInfo mockedUriInfo;
    private JobsBean jobsBean;
    private JSONBContext jsonbContext;

    private ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private JMSContext jmsContext = mock(JMSContext.class);
    private MockedJmsProducer jmsProducer = new MockedJmsProducer();

    @Before
    public void setup() throws URISyntaxException {
        initializeJobsBean();
        jsonbContext = new JSONBContext();

        mockedUriInfo = mock(UriInfo.class);
        final UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);

        final URI uri = new URI(LOCATION);
        when(mockedUriBuilder.build()).thenReturn(uri);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @After
    public void clearMocks() {
        jmsProducer.clearMessages();
    }


    // ************************************* ADD JOB TESTS **************************************************************

    @Test(expected = JobStoreException.class)
    public void addJob_addAndScheduleJobFailure_throwsJobStoreException() throws Exception {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);
        final String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStore.addAndScheduleJob(any(JobInputStream.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
    }

    @Test
    public void addJob_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final Response response = jobsBean.addJob(mockedUriInfo, "invalid JSON");
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addJob_returnsResponseWithHttpStatusCreated_returnsJobInfoSnapshot() throws Exception {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setJobId(JOB_ID).build();
        final JobInputStream jobInputStream = new JobInputStream(jobInfoSnapshot.getSpecification(), false, PART_NUMBER);
        final String jobInputStreamJson = asJson(jobInputStream);

        when(jobsBean.jobStore.addAndScheduleJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addJob(mockedUriInfo, jobInputStreamJson);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        final JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat(returnedJobInfoSnapshot.hasFatalError(), is(false));
        assertThat(returnedJobInfoSnapshot.getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat(returnedJobInfoSnapshot.getSpecification(), is(jobInfoSnapshot.getSpecification()));
        assertThat(returnedJobInfoSnapshot.getState(), is(jobInfoSnapshot.getState()));
        assertThat(returnedJobInfoSnapshot.getFlowStoreReferences(), is(jobInfoSnapshot.getFlowStoreReferences()));
    }


    // ************************************* ADD CHUNK TESTS **************************************************************

    @Test
    public void addChunk_jobIsUpdated_jobInfoSnapShotReturned() throws Exception {
        final JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshotBuilder().setJobId(JOB_ID).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        when(jobsBean.jobStore.addChunk(any(ExternalChunk.class))).thenReturn(jobInfoSnapshot);

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.PROCESSED, chunk);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        final JobInfoSnapshot returnedJobInfoSnapshot = jsonbContext.unmarshall((String) response.getEntity(), JobInfoSnapshot.class);
        assertThat(returnedJobInfoSnapshot, is(notNullValue()));
        assertThat((long) returnedJobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @Test
    public void addChunkProcessed_messageIsSentToSink() throws Exception {

        when(jmsContext.createTextMessage(any(String.class)))
        .thenAnswer(new Answer<MockedJmsTextMessage>() {
            @Override
            public MockedJmsTextMessage answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final MockedJmsTextMessage message = new MockedJmsTextMessage();
                message.setText((String) args[0]);
                return message;
            }
        })
        .thenAnswer(new Answer<MockedJmsTextMessage>() {
            @Override
            public MockedJmsTextMessage answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final MockedJmsTextMessage message = new MockedJmsTextMessage();
                message.setText((String) args[0]);
                return message;
            }
        });

        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        jobsBean.sinkMessageProducer = sinkMessageProducerBean;

        final ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final Sink sink = new SinkBuilder().build();
        final Flow flow = new FlowBuilder().build();
        when(jobsBean.jobStoreRepository.getSinkByJobId(anyLong())).thenReturn(sink);

        // Subject Under Test
        final Response response = jobsBean.addChunkProcessed(mockedUriInfo, jsonChunk, chunk.getJobId(), chunk.getChunkId());
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.getLocation().toString(), is(LOCATION));
        assertThat(response.hasEntity(), is(true));

        assertThat("Number of JMS messages", jmsProducer.messages.size(), is(1));
        assertChunk(chunk, assertProcessorMessageForSink(jmsProducer.messages.pop(), sink.getContent().getResource()));
    }

    private ExternalChunk assertProcessorMessageForSink(MockedJmsTextMessage message, String resource) throws JMSException, JSONBException {
        assertThat("sink JMS msg", message, is(notNullValue()));
        assertThat("sink JMS msg source", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("sink JMS msg payload", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat("sink JMS msg resource", message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(resource));
        return jsonbContext.unmarshall(message.getText(), ExternalChunk.class);
    }
    private void assertChunk(ExternalChunk in, ExternalChunk out) {
        assertThat("chunk type", out.getType(), is(ExternalChunk.Type.PROCESSED));
        assertThat("chunk jobId", out.getJobId(), is(in.getJobId()));
        assertThat("chunk chunkId", out.getChunkId(), is(in.getChunkId()));
        assertThat("chunk size", out.size(), is(in.size()));
        final Iterator<ChunkItem> inIterator = in.iterator();
        for (ChunkItem item : out) {
            assertThat("chunk item data", StringUtil.asString(item.getData()), is(StringUtil.asString(inIterator.next().getData())));
        }
    }

    @Test
    public void addChunk_invalidJobId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId() + 1, chunk.getChunkId(), ExternalChunk.Type.PROCESSED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_invalidChunkId_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId() + 1, ExternalChunk.Type.PROCESSED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_invalidChunkType_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId() + 1, ExternalChunk.Type.DELIVERED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
    }

    @Test
    public void addChunk_marshallingFailure_returnsResponseWithHttpStatusBadRequest() throws Exception {

        final Response response = jobsBean.addChunkDelivered(mockedUriInfo, "invalid json", JOB_ID, CHUNK_ID);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void addChunk_invalidInput_returnsResponseWithHttpStatusBadRequest() throws Exception {
        final JobError jobError = new JobError(JobError.Code.ILLEGAL_CHUNK, "illegal number of items", "stack trace");
        final InvalidInputException invalidInputException = new InvalidInputException("error message", jobError);
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();

        when(jobsBean.jobStore.addChunk(any(ExternalChunk.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.PROCESSED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat(jobErrorReturned, is(notNullValue()));
        assertThat(jobErrorReturned.getCode(), is(jobError.getCode()));
    }

    @Test(expected = JobStoreException.class)
    public void addChunk_onFailureToUpdateJob_throwsJobStoreException() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        when(jobsBean.jobStore.addChunk(any(ExternalChunk.class))).thenThrow(new JobStoreException("Error"));

        jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.DELIVERED, chunk);
    }

    @Test
    public void addChunk_duplicateChunk_throwsJobStoreException() throws Exception {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).setJobId(JOB_ID).setChunkId(CHUNK_ID).build();
        when(jobsBean.jobStore.addChunk(any(ExternalChunk.class))).thenThrow(new DuplicateChunkException("Error", null));

        final Response response = jobsBean.addChunk(mockedUriInfo, chunk.getJobId(), chunk.getChunkId(), ExternalChunk.Type.DELIVERED, chunk);
        assertThat(response.hasEntity(), is(true));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    // ************************************* listJobs() tests **********************************************************

    @Test
    public void listJobs_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreRepository.listJobs(any(JobListCriteria.class))).thenReturn(Collections.<JobInfoSnapshot>emptyList());

        final Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listJobs_unableToUnmarshallJobListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        final Response response = jobsBean.listJobs("Invalid JSON");
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void listJobs_jobStoreReturnsList_returnsStatusOkResponseWithJobInfoSnapshotList() throws JSONBException {
        final List<JobInfoSnapshot> expectedJobInfoSnapshots = new ArrayList<>();
        expectedJobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
        when(jobsBean.jobStoreRepository.listJobs(any(JobListCriteria.class))).thenReturn(expectedJobInfoSnapshots);

        final Response response = jobsBean.listJobs(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType jobInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, JobInfoSnapshot.class);

        List<JobInfoSnapshot> jobInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), jobInfoSnapshotListType);
        assertThat("JobInfoSnapshots", jobInfoSnapshots, is(notNullValue()));
        assertThat("JobInfoSnapshots size", jobInfoSnapshots.size(), is(expectedJobInfoSnapshots.size()));
        assertThat("JobInfoSnapshots element", jobInfoSnapshots.get(0).getJobId(), is(expectedJobInfoSnapshots.get(0).getJobId()));
    }

    // ************************************* listItems() tests **********************************************************

    @Test
    public void listItems_jobStoreReturnsEmptyList_returnsStatusOkResponseWithEmptyList() throws JSONBException {
        when(jobsBean.jobStoreRepository.listItems(any(ItemListCriteria.class))).thenReturn(Collections.<ItemInfoSnapshot>emptyList());

        final Response response = jobsBean.listItems(asJson(new ItemListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listItems_unableToUnmarshallItemListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        final Response response = jobsBean.listJobs("Invalid JSON");
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }

    @Test
    public void listItems_jobStoreReturnsList_returnsStatusOkResponseWithItemInfoSnapshotList() throws JSONBException {
        final List<ItemInfoSnapshot> expectedItemInfoSnapshots = new ArrayList<>();
        expectedItemInfoSnapshots.add(new ItemInfoSnapshotBuilder().build());
        when(jobsBean.jobStoreRepository.listItems(any(ItemListCriteria.class))).thenReturn(expectedItemInfoSnapshots);

        final Response response = jobsBean.listItems(asJson(new JobListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final CollectionType itemInfoSnapshotListType =
                jsonbContext.getTypeFactory().constructCollectionType(List.class, ItemInfoSnapshot.class);

        List<ItemInfoSnapshot> itemInfoSnapshots = jsonbContext.unmarshall((String) response.getEntity(), itemInfoSnapshotListType);
        assertThat("ItemInfoSnapshots", itemInfoSnapshots, is(notNullValue()));
        assertThat("ItemInfoSnapshots size", itemInfoSnapshots.size(), is(expectedItemInfoSnapshots.size()));
        assertThat("ItemInfoSnapshots element", itemInfoSnapshots.get(0).getItemId(), is(expectedItemInfoSnapshots.get(0).getItemId()));
    }

    // ************************************* countItems() tests **********************************************************

    @Test
    public void countItems_jobStoreReturnsItemCount_returnsStatusOkResponseWithCountAsEntity() throws JSONBException {
        when(jobsBean.jobStoreRepository.countItems(any(ItemListCriteria.class))).thenReturn(110L);

        final Response response = jobsBean.countItems(asJson(new ItemListCriteria()));
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        long count = jsonbContext.unmarshall((String)response.getEntity(), Long.class);
        assertThat("Count", count, is(110L));
    }

    @Test
    public void countItems_unableToUnmarshallItemListCriteria_returnsStatusBadRequestWithJobError() throws JSONBException {
        final Response response = jobsBean.countJobs("Invalid JSON");
        assertThat("Response", response, is(notNullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }

    // ************************************* getResourceBundle() tests ***********************************************************

    @Test
    public void getResourceBundle_resourcesLocated_returnsStatusOkResponseWithResourceBundle() throws JSONBException, JobStoreException {
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessDataBuilder().build();
        ResourceBundle resourceBundle = new ResourceBundle(flow, sink, supplementaryProcessData);

        when(jobsBean.jobStoreRepository.getResourceBundle(anyInt())).thenReturn(resourceBundle);

        final Response response = jobsBean.getResourceBundle(JOB_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        final ResourceBundle resourceBundleReturned = jsonbContext.unmarshall((String) response.getEntity(), ResourceBundle.class);
        assertThat("ResourceBundle not null", resourceBundleReturned, not(nullValue()));
    }


    @Test
    public void getResourceBundle_jobEntityNotFound_returnsStatusBadRequestResponseWithJobError() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreRepository.getResourceBundle(anyInt())).thenThrow(invalidInputException);

        final Response response = jobsBean.getResourceBundle(JOB_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        final JobError jobErrorReturned = jsonbContext.unmarshall((String) response.getEntity(), JobError.class);
        assertThat("JobError not null", jobErrorReturned, is(notNullValue()));
    }

    // ************************************* getItemData() tests ***********************************************************

    @Test
    public void getItemData_itemEntityLocated_returnsStatusOkResponseWithDataAsString() throws JSONBException, JobStoreException {
        ItemData itemData = new ItemData("data", Charset.defaultCharset());

        when(jobsBean.jobStoreRepository.getItemData(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenReturn(itemData);

        final Response response = jobsBean.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        assertThat("base64decodedDataString not null", response.getEntity().toString(), not(nullValue()));
    }


    @Test
    public void getItemData_itemEntityNotFound_returnsStatusNotFoundResponse() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreRepository.getItemData(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenThrow(invalidInputException);

        final Response response = jobsBean.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(false));
    }

    // ************************************* getProcessedNextResult() tests ***********************************************************

    @Test
    public void getProcessedNextResult_itemEntityLocated_returnsStatusOkResponseWithDataAsString() throws JSONBException, JobStoreException {
        ChunkItem chunkItem = new ChunkItem(1, StringUtil.asBytes("Next data"), ChunkItem.Status.SUCCESS);

        when(jobsBean.jobStoreRepository.getNextProcessingOutcome(anyInt(), anyInt(), anyShort())).thenReturn(chunkItem);

        final Response response = jobsBean.getProcessedNextResult(JOB_ID, CHUNK_ID, ITEM_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(true));
        assertThat("DataString not null", response.getEntity().toString(), not(nullValue()));
    }


    @Test
    public void getProcessedNextResult_itemEntityNotFound_returnsStatusNotFoundResponse() throws Exception {
        JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "job not found", null);
        InvalidInputException invalidInputException = new InvalidInputException("msg", jobError);

        when(jobsBean.jobStoreRepository.getNextProcessingOutcome(anyInt(), anyInt(), anyShort())).thenThrow(invalidInputException);

        final Response response = jobsBean.getProcessedNextResult(JOB_ID, CHUNK_ID, ITEM_ID);
        assertThat("Response not null", response, not(nullValue()));
        assertThat("Response status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("Response entity", response.hasEntity(), is(false));
    }

    /*
     Private methods
    */


    private void initializeJobsBean() {
        jobsBean = new JobsBean();
        jobsBean.jobStore = mock(PgJobStore.class);
        jobsBean.jobStoreRepository = mock(PgJobStoreRepository.class);
    }

    private String asJson(Object object) throws JSONBException {
        return jsonbContext.marshall(object);
    }

}
