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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class
})
public class JobStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String JOB_STORE_URL = "http://dataio/job-store";
    private static final int PART_NUMBER = 414243;
    private static final int JOB_ID = 3;
    private static final int CHUNK_ID = 44;
    private static final short ITEM_ID = 0;
    private static final String ITEM_DATA = "item data";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new JobStoreServiceConnector(null, JOB_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new JobStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new JobStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(JOB_STORE_URL));
    }

    // ******************************************* add job tests ********************************************

    @Test(expected = NullPointerException.class)
    public void addJob_jobInputStreamArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addJob(null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.CREATED, null);
    }

    @Test
    public void addJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void addJob_onProcessingException_throws() {
        final JobInputStream jobInputStream = getNewJobInputStream();
        when(HttpClient.doPostWithJson(CLIENT, jobInputStream, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenThrow(new ProcessingException("Connection reset"));
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.addJob(jobInputStream);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void addJob_jobIsAdded_returnsJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final JobInfoSnapshot expectedJobInfoSnapshot = new JobInfoSnapshotBuilder().build();
        final JobInfoSnapshot jobInfoSnapshot = callAddJobWithMockedHttpResponse(getNewJobInputStream(), Response.Status.CREATED, expectedJobInfoSnapshot);
        assertThat(jobInfoSnapshot, is(expectedJobInfoSnapshot));
    }

    // ******************************************* add chunk tests *******************************************

    @Test(expected = NullPointerException.class)
    public void addChunk_chunkArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(null, JOB_ID, CHUNK_ID);
    }

    @Test(expected = NullPointerException.class)
    public void addChunk_chunkTypeIsNull_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(null).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChunk_chunkTypePartitioned_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
    }

    @Test
    public void addChunk_chunkTypeProcessed_chunkIsAdded() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkWithMockedHttpResponse(chunk, Response.Status.CREATED,
                new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    @Test
    public void addChunk_chunkTypeDelivering_chunkIsAdded() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).build();
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkWithMockedHttpResponse(chunk, Response.Status.CREATED,
                new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
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

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addChunk_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.DELIVERED).build();
        callAddChunkWithMockedHttpResponse(chunk, Response.Status.INTERNAL_SERVER_ERROR, null);
    }

    // ************************************* add chunk ignore duplicates tests **************************************

    @Test
    public void addChunkIgnoreDuplicates_chunkArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(null, JOB_ID, CHUNK_ID);
            fail();
        } catch (NullPointerException e) {}
    }

    @Test
    public void addChunkIgnoreDuplicates_chunkTypeIsNull_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(null).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, JOB_ID, CHUNK_ID);
            fail();
        } catch (NullPointerException e) {}
    }

    @Test
    public void addChunkIgnoreDuplicates_chunkTypePartitioned_throws() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.addChunk(chunk, JOB_ID, CHUNK_ID);
            fail();
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void addChunkIgnoreDuplicates_acceptedRequestResponse_lookupJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkIgnoreDuplicatesWithMockedHttpResponse(chunk,
                Response.Status.ACCEPTED, new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
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
        final JobInfoSnapshot jobInfoSnapshot = callAddChunkIgnoreDuplicatesWithMockedHttpResponse(chunk,
                Response.Status.CREATED, new JobInfoSnapshotBuilder().setJobId(JOB_ID).build());

        assertThat(jobInfoSnapshot, is(notNullValue()));
        assertThat((long) jobInfoSnapshot.getJobId(), is(chunk.getJobId()));
    }

    // ******************************************* listJobs() tests *******************************************

    @Test
    public void listJobs_criteriaArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.listJobs(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobs_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        try {
            callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.INTERNAL_SERVER_ERROR, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(500));
        }
    }

    @Test
    public void listJobs_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        try {
            callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void listJobs_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> expectedSnapshots = Collections.emptyList();
        final List<JobInfoSnapshot> returnedSnapshots = callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void listJobs_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<JobInfoSnapshot> expectedSnapshots = Collections.singletonList(new JobInfoSnapshotBuilder().build());
        final List<JobInfoSnapshot> returnedSnapshots = callListJobsWithMockedHttpResponse(new JobListCriteria(), Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void countJobs_parseNumber() throws Exception {
        long count = 123;
        JobListCriteria criteria=new JobListCriteria();


        when(HttpClient.doPostWithJson(CLIENT, criteria, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES_COUNT))
                .thenReturn(new MockedResponse<>(200, count));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        long res = instance.countJobs(criteria);
        assertThat(res, is(count));
    }

    // ******************************************* listItems() tests *******************************************

    @Test
    public void listItems_criteriaArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.listItems(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listItems_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        try {
            callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.INTERNAL_SERVER_ERROR, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(500));
        }
    }

    @Test
    public void listItems_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        try {
            callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void listItems_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<ItemInfoSnapshot> expectedSnapshots = Collections.emptyList();
        final List<ItemInfoSnapshot> returnedSnapshots = callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void listItems_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final List<ItemInfoSnapshot> expectedSnapshots = Collections.singletonList(new ItemInfoSnapshotBuilder().build());
        final List<ItemInfoSnapshot> returnedSnapshots = callListItemsWithMockedHttpResponse(new ItemListCriteria(), Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void countItems_parseNumber() throws Exception {
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        long count = 123;
        ItemListCriteria criteria = new ItemListCriteria();

        when(HttpClient.doPostWithJson(CLIENT, criteria, JOB_STORE_URL, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES_COUNT))
                .thenReturn(new MockedResponse<>(200, count));

        long result = instance.countItems(criteria);
        assertThat(result, is(count));
    }

    // ******************************************* getResourceBundle() tests *******************************************

    @Test
    public void getResourceBundle_jobIdArgIsLessThanBound_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.getResourceBundle(-1);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void getResourceBundle_badRequestResponse_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JOB_IDENTIFIER, "description", null);
        try {
            callGetResourceBundleWithMockedHttpResponse(JOB_ID, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void getResourceBundle_responseWithNullValuedEntity_throws() throws JobStoreServiceConnectorException {
        try {
            callGetResourceBundleWithMockedHttpResponse(JOB_ID, Response.Status.INTERNAL_SERVER_ERROR, null);
        } catch(JobStoreServiceConnectorException e) {}
    }

    @Test
    public void getResourceBundle_bundleCreated_returnsResourceBundle() throws JobStoreServiceConnectorException {
        Flow flow = new FlowBuilder().build();
        Sink sink = new SinkBuilder().build();
        SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessDataBuilder().build();
        final ResourceBundle expectedResourceBundle = new ResourceBundle(flow, sink, supplementaryProcessData);

        final ResourceBundle resourceBundle = callGetResourceBundleWithMockedHttpResponse(JOB_ID, Response.Status.OK, expectedResourceBundle);

        assertThat("ResourceBundle not null", resourceBundle, not(nullValue()));
        assertThat(String.format("ResourceBundle: %s, expected to match: %s", resourceBundle, expectedResourceBundle), resourceBundle, is(expectedResourceBundle));
    }

    // ******************************************* getItemData() tests *******************************************

    @Test
    public void getItemData_jobIdArgIsLessThanBound_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.getItemData(-1, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test(expected = NullPointerException.class)
    public void getItemData_phaseIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.getItemData(JOB_ID, CHUNK_ID, ITEM_ID, null);
    }

    @Test
    public void getItemData_notFoundResponse_throws() throws JobStoreServiceConnectorException {
        try {
            callGetItemDataWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING,
                    Response.Status.NOT_FOUND, null);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void getItemData_partitioningPhase_partitionedDataReturned() throws JobStoreServiceConnectorException {
        final String data = callGetItemDataWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PARTITIONING,
                Response.Status.OK, ITEM_DATA);

        assertThat(data, is(notNullValue()));
        assertThat(data, is(ITEM_DATA));
    }

    @Test
    public void getItemData_processingPhase_processedDataReturned() throws JobStoreServiceConnectorException {
        final String data = callGetItemDataWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.PROCESSING,
                Response.Status.OK, ITEM_DATA);

        assertThat(data, is(notNullValue()));
        assertThat(data, is(ITEM_DATA));
    }

    @Test
    public void getItemData_deliveredPhase_deliveredDataReturned() throws JobStoreServiceConnectorException {
        final String data = callGetItemDataWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID, State.Phase.DELIVERING,
                Response.Status.OK, ITEM_DATA);

        assertThat(data, is(notNullValue()));
        assertThat(data, is(ITEM_DATA));
    }

    // ***************************************** getNextItemData() tests *****************************************

    @Test
    public void getProcessedNextResult_jobIdArgIsLessThanBound_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.getProcessedNextResult(-1, CHUNK_ID, ITEM_ID);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void getProcessedNextResult_notFoundResponse_throws() throws JobStoreServiceConnectorException {
        try {
            callProcessedNextResultWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID,
                    Response.Status.NOT_FOUND, null);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void getProcessedNextResult_itemFound_returnsProcessedNextResult() throws JobStoreServiceConnectorException {
        final String data = callProcessedNextResultWithMockedHttpResponse(JOB_ID, CHUNK_ID, ITEM_ID,
                Response.Status.OK, ITEM_DATA);

        assertThat(data, is(notNullValue()));
        assertThat(data, is(ITEM_DATA));
    }

    // ******************************************* listJobNotificationsForJob() tests *******************************************

    @Test
    public void listJobNotificationsForJob_JobIdIsZero_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.listJobNotificationsForJob(0);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        try {
            callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.INTERNAL_SERVER_ERROR, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(500));
        }
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsNullEntity_throws() throws JobStoreServiceConnectorException {
        try {
            callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, null);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsEmptyListEntity_returnsEmptyList() throws JobStoreServiceConnectorException {
        final List<JobNotification> expectedSnapshots = Collections.emptyList();
        final List<JobNotification> returnedSnapshots = callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    @Test
    public void listJobNotificationsForJob_serviceReturnsNonEmptyListEntity_returnsNonEmptyList() throws JobStoreServiceConnectorException {
        final JobNotification testJobNotification = new JobNotification(234, new Date(), new Date(), JobNotification.Type.JOB_CREATED, JobNotification.Status.COMPLETED, "status message", "destination", "content", 345);
        final List<JobNotification> expectedSnapshots = Collections.singletonList(testJobNotification);
        final List<JobNotification> returnedSnapshots = callListJobNotificationForJobIdWithMockedHttpResponse(123, Response.Status.OK, expectedSnapshots);
        assertThat(returnedSnapshots, is(expectedSnapshots));
    }

    // ******************************************* addNotification() tests *******************************************

    @Test(expected = NullPointerException.class)
    public void addNotification_requestArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.addNotification(null);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void addNotification_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        callAddNotificationWithMockedHttpResponse(getAddNotificationRequest(), Response.Status.OK, null);
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
    public void addNotification_onProcessingException_throws() {
        final AddNotificationRequest request = getAddNotificationRequest();
        when(HttpClient.doPostWithJson(CLIENT, request, JOB_STORE_URL, JobStoreServiceConstants.NOTIFICATIONS))
                .thenThrow(new ProcessingException("Connection reset"));
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.addNotification(request);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void addNotification_notificationIsAdded_returnsJobNotification() throws JobStoreServiceConnectorException {
        final JobNotification expectedJobNotification = new JobNotification();
        final JobNotification jobNotification = callAddNotificationWithMockedHttpResponse(getAddNotificationRequest(), Response.Status.OK, expectedJobNotification);
        assertThat(jobNotification, is(expectedJobNotification));
    }

    // ******************************************* set workflowNote on job tests ********************************************

    @Test(expected = NullPointerException.class)
    public void setWorkflowNoteOnJob_workflowNoteArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.setWorkflowNote(null, JOB_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void setWorkflowNoteOnJob_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(),
                JOB_ID, Response.Status.OK, null);
    }

    @Test
    public void setWorkflowNoteOnJob_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(),
                    JOB_ID, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void setWorkflowNoteOnJob_onProcessingException_throws() {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(JOB_ID));
        when(HttpClient.doPostWithJson(CLIENT, workflowNote, JOB_STORE_URL, path.build()))
                .thenThrow(new ProcessingException("Connection reset"));

        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.setWorkflowNote(workflowNote, JOB_ID);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void setWorkflowNoteOnJob_workflowNoteIsAdded_returnsJobInfoSnapshot() throws JobStoreServiceConnectorException {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final JobInfoSnapshot expectedJobInfoSnapshot = new JobInfoSnapshotBuilder().setWorkflowNote(workflowNote).build();
        final JobInfoSnapshot jobInfoSnapshot = callSetWorkflowNoteWithMockedHttpResponse(workflowNote,
                JOB_ID, Response.Status.OK, expectedJobInfoSnapshot);
        assertThat(jobInfoSnapshot, is(expectedJobInfoSnapshot));
    }

    // ******************************************* set workflowNote on item tests ********************************************

    @Test(expected = NullPointerException.class)
    public void setWorkflowNoteOnItem_workflowNoteArgIsNull_throws() throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        jobStoreServiceConnector.setWorkflowNote(null, JOB_ID, CHUNK_ID, ITEM_ID);
    }

    @Test(expected = JobStoreServiceConnectorException.class)
    public void setWorkflowNoteOnItem_responseWithNullEntity_throws() throws JobStoreServiceConnectorException {
        callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(),
                JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.OK, null);
    }

    @Test
    public void setWorkflowNoteOnItem_responseWithUnexpectedStatusCode_throws() throws JobStoreServiceConnectorException {
        final JobError jobError = new JobError(JobError.Code.INVALID_JSON, "description", null);
        try {
            callSetWorkflowNoteWithMockedHttpResponse(new WorkflowNoteBuilder().build(),
                    JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.BAD_REQUEST, jobError);
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat("Exception status code", e.getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Exception JobError entity not null", e.getJobError(), is(notNullValue()));
            assertThat("Exception JobError entity", e.getJobError(), is(jobError));
        }
    }

    @Test
    public void setWorkflowNoteOnItem_onProcessingException_throws() {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(JOB_ID))
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(CHUNK_ID))
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(ITEM_ID));
        when(HttpClient.doPostWithJson(CLIENT, workflowNote, JOB_STORE_URL, path.build()))
                .thenThrow(new ProcessingException("Connection reset"));

        final JobStoreServiceConnector jobStoreServiceConnector = newJobStoreServiceConnector();
        try {
            jobStoreServiceConnector.setWorkflowNote(workflowNote, JOB_ID, CHUNK_ID, ITEM_ID);
            fail("No exception thrown");
        } catch (JobStoreServiceConnectorException e) {
        }
    }

    @Test
    public void setWorkflowNoteOnItem_workflowNoteIsAdded_returnsItemInfoSnapshot() throws JobStoreServiceConnectorException {
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();
        final ItemInfoSnapshot expectedItemInfoSnapshot = new ItemInfoSnapshotBuilder().setWorkflowNote(workflowNote).build();
        final ItemInfoSnapshot itemInfoSnapshot = callSetWorkflowNoteWithMockedHttpResponse(
                workflowNote, JOB_ID, CHUNK_ID, ITEM_ID, Response.Status.OK, expectedItemInfoSnapshot);
        assertThat(itemInfoSnapshot, is(expectedItemInfoSnapshot));
    }

    /*
     * Private methods
     */

    // Helper method
    private JobInfoSnapshot callAddJobWithMockedHttpResponse(JobInputStream jobInputStream, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doPostWithJson(CLIENT, jobInputStream, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.addJob(jobInputStream);
    }

    private JobInfoSnapshot callAddChunkWithMockedHttpResponse(Chunk chunk, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final String basePath = getAddChunkBasePath(chunk);
        when(HttpClient.doPostWithJson(CLIENT, chunk, JOB_STORE_URL, buildAddChunkPath(chunk.getJobId(), chunk.getChunkId(), basePath)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.addChunk(chunk, chunk.getJobId(), chunk.getChunkId());
    }

   private JobInfoSnapshot callAddChunkIgnoreDuplicatesWithMockedHttpResponse(Chunk chunk, Response.Status statusCode, Object returnValue)
           throws JobStoreServiceConnectorException {
        final String basePath = getAddChunkBasePath(chunk);
        when(HttpClient.doPostWithJson(CLIENT, chunk, JOB_STORE_URL, buildAddChunkPath(chunk.getJobId(), chunk.getChunkId(), basePath)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        when(HttpClient.doPostWithJson(any(Client.class), any(JobListCriteria.class), anyString(), anyString()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), Collections.singletonList(returnValue)));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
    }

    private List<JobInfoSnapshot> callListJobsWithMockedHttpResponse(JobListCriteria criteria, Response.Status statusCode, List<JobInfoSnapshot> responseEntity)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doPostWithJson(CLIENT, criteria, JOB_STORE_URL, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), responseEntity));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.listJobs(criteria);
    }

    private List<ItemInfoSnapshot> callListItemsWithMockedHttpResponse(ItemListCriteria criteria, Response.Status statusCode, List<ItemInfoSnapshot> responseEntity)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doPostWithJson(CLIENT, criteria, JOB_STORE_URL, JobStoreServiceConstants.ITEM_COLLECTION_SEARCHES))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), responseEntity));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.listItems(criteria);
    }

    private ResourceBundle callGetResourceBundleWithMockedHttpResponse(int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_RESOURCEBUNDLE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
        when(HttpClient.doGet(CLIENT, JOB_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));

        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.getResourceBundle(jobId);
    }

    private String callGetItemDataWithMockedHttpResponse(int jobId, int chunkId, short itemId, State.Phase phase, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        final String basePath;
        switch (phase) {
            case PARTITIONING: basePath = JobStoreServiceConstants.CHUNK_ITEM_PARTITIONED;
                break;
            case PROCESSING: basePath = JobStoreServiceConstants.CHUNK_ITEM_PROCESSED;
                break;
            case DELIVERING: basePath = JobStoreServiceConstants.CHUNK_ITEM_DELIVERED;
                break;
            default: basePath = "";
        }
        when(HttpClient.doGet(CLIENT, JOB_STORE_URL, buildAddChunkItemPath(jobId, chunkId, itemId, basePath)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.getItemData(jobId, chunkId, itemId, phase);
    }

    private List<JobNotification> callListJobNotificationForJobIdWithMockedHttpResponse(int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, JOB_STORE_URL, buildJobIdPath(jobId, JobStoreServiceConstants.JOB_NOTIFICATIONS)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.listJobNotificationsForJob(jobId);
    }

    private String callProcessedNextResultWithMockedHttpResponse(int jobId, int chunkId, short itemId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, JOB_STORE_URL, buildAddChunkItemPath(jobId, chunkId, itemId, JobStoreServiceConstants.CHUNK_ITEM_PROCESSED_NEXT)))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.getProcessedNextResult(jobId, chunkId, itemId);
    }

    private JobNotification callAddNotificationWithMockedHttpResponse(AddNotificationRequest request, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {
        when(HttpClient.doPostWithJson(CLIENT, request, JOB_STORE_URL, JobStoreServiceConstants.NOTIFICATIONS))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.addNotification(request);
    }

    private JobInfoSnapshot callSetWorkflowNoteWithMockedHttpResponse(WorkflowNote workflowNote, int jobId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        when(HttpClient.doPostWithJson(CLIENT, workflowNote, JOB_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.setWorkflowNote(workflowNote, jobId);
    }

    private ItemInfoSnapshot callSetWorkflowNoteWithMockedHttpResponse(WorkflowNote workflowNote, int jobId, int chunkId, short itemId, Response.Status statusCode, Object returnValue)
            throws JobStoreServiceConnectorException {

        final PathBuilder path = new PathBuilder(JobStoreServiceConstants.ITEM_WORKFLOW_NOTE)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId))
        .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId))
        .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, Long.toString(itemId));
        when(HttpClient.doPostWithJson(CLIENT, workflowNote, JOB_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
        final JobStoreServiceConnector instance = newJobStoreServiceConnector();
        return instance.setWorkflowNote(workflowNote, jobId, chunkId, itemId);
    }

    private String getAddChunkBasePath(Chunk chunk) {
        final String basePath;
        switch (chunk.getType()) {
            case PROCESSED: basePath = JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
                break;
            case DELIVERED: basePath = JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
                break;
            default: basePath = "";
        }
        return basePath;
    }

    private String[] buildAddChunkPath(long jobId, long chunkId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId).build();
    }

    private String[] buildAddChunkItemPath(int jobId, int chunkId, short itemId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                .bind(JobStoreServiceConstants.ITEM_ID_VARIABLE, itemId)
                .build();
    }

    private String[] buildJobIdPath(int jobId, String pathString) {
        return new PathBuilder(pathString)
                .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .build();
    }

    private static JobInputStream getNewJobInputStream() {
        try {
            final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
            return new JobInputStream(jobSpecification, false, PART_NUMBER);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private static AddNotificationRequest getAddNotificationRequest() {
        try {
            final InvalidTransfileNotificationContext context = new InvalidTransfileNotificationContext("name", "content", "cause");
            return new AddNotificationRequest("mail@company.com", context, JobNotification.Type.INVALID_TRANSFILE);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private static JobStoreServiceConnector newJobStoreServiceConnector() {
        try {
            return new JobStoreServiceConnector(CLIENT, JOB_STORE_URL);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
