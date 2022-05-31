package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.PrettyPrint;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.gui.server.modelmappers.WorkflowNoteModelMapper;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobStoreProxyImplTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final long ID = 737L;

    private final JobModel testJobModel = new JobModel()
            .withPackaging("packaging")
            .withFormat("format")
            .withCharset("utf8")
            .withDestination("dest")
            .withSubmitterNumber("12345")
            .withMailForNotificationAboutVerification("mail")
            .withMailForNotificationAboutProcessing("mail")
            .withResultMailInitials("mail")
            .withDataFile("42")
            .withType(JobSpecification.Type.TEST)
            .withAncestry(new JobSpecification.Ancestry()
                    .withTransfile("transfile")
                    .withDatafile("datafile")
                    .withBatchId("id")
                    .withDetails("details".getBytes())
                    .withPreviousJobId(4320));

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void constructor() {
        environmentVariables.set("JOBSTORE_URL", "http://dataio/job-store");
        new JobStoreProxyImpl();
    }

    @Test(expected = NullPointerException.class)
    public void noArgs_jobStoreProxyConstructorJobStoreService_EndpointCanNotBeLookedUp_throws() {
        new JobStoreProxyImpl();
    }

    @Test(expected = ProxyException.class)
    public void listJobs_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.listJobs(new JobListCriteria());
    }

    @Test
    public void listJobs_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<JobInfoSnapshot> jobInfoSnapshots = getListOfJobInfoSnapshots();

        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(jobInfoSnapshots);
        try {
            List<JobModel> jobModels = jobStoreProxy.listJobs(new JobListCriteria());
            assertThat(jobModels, not(nullValue()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listJobs()");
        }
    }

    @Test(expected = ProxyException.class)
    public void listItems_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listItems(any(ItemListCriteria.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.listItems(ItemListCriteria.Field.JOB_ID, new ItemListCriteria());
    }

    @Test
    public void listFailedItemsForJob_remoteServiceReturnsHttpStatusOk_returnsListOfItemModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<ItemInfoSnapshot> itemInfoSnapshots = getFailedListOfItemInfoSnapshots();

        when(jobStoreServiceConnector.listItems(any(ItemListCriteria.class))).thenReturn(itemInfoSnapshots);
        try {
            List<ItemModel> itemModels = jobStoreProxy.listItems(ItemListCriteria.Field.STATE_FAILED, new ItemListCriteria());
            assertThat(itemModels, not(nullValue()));
            assertThat(itemModels.get(0).getStatus(), is(ItemModel.LifeCycle.PROCESSING_FAILED));
            assertThat(itemModels.get(1).getStatus(), is(ItemModel.LifeCycle.DELIVERING_FAILED));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listItems()");
        }
    }

    @Test
    public void listIgnoredItemsForJob_remoteServiceReturnsHttpStatusOk_returnsListOfItemModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<ItemInfoSnapshot> itemInfoSnapshots = getIgnoredListOfItemInfoSnapshots();


        when(jobStoreServiceConnector.listItems(any(ItemListCriteria.class))).thenReturn(itemInfoSnapshots);
        try {
            List<ItemModel> itemModels = jobStoreProxy.listItems(ItemListCriteria.Field.STATE_IGNORED, new ItemListCriteria());
            assertThat(itemModels, not(nullValue()));
            assertThat(itemModels.get(0).getStatus(), is(ItemModel.LifeCycle.PROCESSING_IGNORED));
            assertThat(itemModels.get(1).getStatus(), is(ItemModel.LifeCycle.DELIVERING_IGNORED));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listItems()");
        }
    }

    @Test
    public void listAllItemsForJob_remoteServiceReturnsHttpStatusOk_returnsListOfItemModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<ItemInfoSnapshot> itemInfoSnapshots = getListOfItemInfoSnapshots();


        when(jobStoreServiceConnector.listItems(any(ItemListCriteria.class))).thenReturn(itemInfoSnapshots);
        try {
            List<ItemModel> itemModels = jobStoreProxy.listItems(ItemListCriteria.Field.JOB_ID, new ItemListCriteria());
            assertThat(itemModels, not(nullValue()));
            assertThat(itemModels.get(0).getStatus(), is(ItemModel.LifeCycle.PROCESSING));
            assertThat(itemModels.get(1).getStatus(), is(ItemModel.LifeCycle.DELIVERING));
            assertThat(itemModels.get(2).getStatus(), is(ItemModel.LifeCycle.DONE));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listItems()");
        }
    }

    @Test(expected = ProxyException.class)
    public void getChunkItem_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getChunkItem(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.getItemData(new ItemModel(), ItemModel.LifeCycle.PROCESSING);
    }

    @Test
    public void getChunkItem_remoteServiceReturnsHttpStatusOk_returnsDataString() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        final ChunkItem chunkItem = new ChunkItemBuilder().setData(getXmlData()).build();
        when(jobStoreServiceConnector.getChunkItem(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenReturn(chunkItem);
        try {
            String data = jobStoreProxy.getItemData(new ItemModel(), ItemModel.LifeCycle.PARTITIONING);
            assertThat("data not null", data, not(nullValue()));
            assertThat(data, is(PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding())));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemData()");
        }
    }

    @Test
    public void getChunkItem_remoteServiceReturnsHttpStatusOkWithEntityWithoutType_returnsEmptyString() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        final ChunkItem chunkItem = new ChunkItemBuilder().setData(getXmlData()).setType(Collections.emptyList()).build();
        when(jobStoreServiceConnector.getChunkItem(anyInt(), anyInt(), anyShort(), any(State.Phase.class))).thenReturn(chunkItem);
        try {
            String data = jobStoreProxy.getItemData(new ItemModel(), ItemModel.LifeCycle.PARTITIONING);
            assertThat(data, is(PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding())));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemData()");
        }
    }

    @Test(expected = ProxyException.class)
    public void getProcessedNextResult_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getProcessedNextResult(anyInt(), anyInt(), anyShort())).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.getProcessedNextResult(1, 0, (short) 0);
    }

    @Test
    public void getProcessedNextResult_remoteServiceReturnsHttpStatusOk_returnsDataString() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        final ChunkItem chunkItem = new ChunkItemBuilder().setData(getXmlData()).build();
        when(jobStoreServiceConnector.getProcessedNextResult(anyInt(), anyInt(), anyShort())).thenReturn(chunkItem);
        try {
            String data = jobStoreProxy.getProcessedNextResult(1, 0, (short) 0);
            assertThat("data not null", data, not(nullValue()));
            assertThat(data, is(PrettyPrint.asXml(chunkItem.getData(), chunkItem.getEncoding())));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getProcessedNextResult()");
        }
    }

    @Test(expected = ProxyException.class)
    public void listJobNotificationsForJob_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobNotificationsForJob(any(Integer.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.listJobNotificationsForJob(123);
    }


    @Test
    public void listJobNotificationsForJob_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<Notification> testJobNotifications = new ArrayList<>();
        testJobNotifications.add(new Notification()
                .withId(11)
                .withType(Notification.Type.JOB_CREATED)
                .withStatus(Notification.Status.WAITING)
                .withStatusMessage("status1")
                .withDestination("dest1")
                .withContent("content1")
                .withJobId(1111));
        testJobNotifications.add(new Notification()
                .withId(22)
                .withType(Notification.Type.JOB_COMPLETED)
                .withStatus(Notification.Status.COMPLETED)
                .withStatusMessage("status2")
                .withDestination("dest2")
                .withContent("content2")
                .withJobId(2222));
        testJobNotifications.add(new Notification()
                .withId(33)
                .withType(Notification.Type.JOB_COMPLETED)
                .withStatus(Notification.Status.FAILED)
                .withStatusMessage("status3")
                .withDestination("dest3")
                .withContent("content3")
                .withJobId(3333));

        when(jobStoreServiceConnector.listJobNotificationsForJob(any(Integer.class))).thenReturn(testJobNotifications);
        try {
            List<Notification> jobNotifications = jobStoreProxy.listJobNotificationsForJob(2222);
            assertThat(jobNotifications, is(testJobNotifications));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listJobs()");
        }
    }

    @Test(expected = ProxyException.class)
    public void reRunJob_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.reSubmitJob(new JobModel());
    }

    @Test
    public void reRunJob_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        final int jobId = 1;
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenReturn(getJobInfoSnapshot(new Date()).withJobId(jobId));

        try {
            JobModel jobModel = jobStoreProxy.reSubmitJob(testJobModel.withJobId(String.valueOf(jobId)));
            assertThat(jobModel.getJobId(), is(testJobModel.getJobId()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: reRunJobs()");
        }
    }

    @Test(expected = ProxyException.class)
    public void reRunJobs_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).thenThrow(new JobStoreServiceConnectorException("Testing"));
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);

        jobStoreProxy.reSubmitJobs(Arrays.asList(testJobModel.withJobId("1"), testJobModel.withJobId("2")));
    }

    @Test
    public void reRunJobs_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class))).
                thenReturn(getJobInfoSnapshot(new Date()).withJobId(1)).
                thenReturn(getJobInfoSnapshot(new Date()).withJobId(2));

        try {
            List<JobModel> jobModels = jobStoreProxy.reSubmitJobs(Arrays.asList(testJobModel.withJobId("1"), testJobModel.withJobId("2")));
            assertThat(jobModels.get(0).getJobId(), is("1"));
            assertThat(jobModels.get(1).getJobId(), is("2"));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: reRunJobs()");
        }
    }

    @Test(expected = ProxyException.class)
    public void listInvalidTransfileNotifications_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listInvalidTransfileNotifications()).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.listInvalidTransfileNotifications();
    }


    @Test
    public void listInvalidTransfileNotifications_remoteServiceReturnsHttpStatusOk_returnsListOfTransfileNotifications() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<Notification> testTransfileNotifications = new ArrayList<>();
        testTransfileNotifications.add(new Notification().withId(111).withJobId(1111).withContent("Content 1"));
        testTransfileNotifications.add(new Notification().withId(222).withJobId(2222).withContent("Content 2"));
        testTransfileNotifications.add(new Notification().withId(333).withJobId(3333).withContent("Content 3"));

        when(jobStoreServiceConnector.listInvalidTransfileNotifications()).thenReturn(testTransfileNotifications);
        try {
            List<Notification> notifications = jobStoreProxy.listInvalidTransfileNotifications();
            assertThat(notifications, is(testTransfileNotifications));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listInvalidTransfileNotifications()");
        }
    }

    @Test(expected = ProxyException.class)
    public void setWorkflowNoteForJob_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), (any(Integer.class)))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.setWorkflowNote(new WorkflowNoteModelBuilder().build(), 1);
    }

    @Test
    public void setWorkflowNoteForJob_remoteServiceReturnsHttpStatusOk_returnsUpdatedJobModel() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        WorkflowNoteModel workflowNoteModel = new WorkflowNoteModelBuilder().build();

        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), anyInt()))
                .thenReturn(getJobInfoSnapshot(new Date()).withWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel)));
        try {
            JobModel updatedJobModel = jobStoreProxy.setWorkflowNote(workflowNoteModel, 1);
            assertThat(updatedJobModel, is(notNullValue()));
            assertThat(updatedJobModel.getWorkflowNoteModel(), is(workflowNoteModel));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: setWorkflowNote()");
        }
    }

    @Test(expected = ProxyException.class)
    public void setWorkflowNoteForItem_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), anyInt(), anyInt(), anyShort())).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.setWorkflowNote(new WorkflowNoteModelBuilder().build(), 1, 0, (short) 0);
    }

    @Test
    public void setWorkflowNoteForItem_remoteServiceReturnsHttpStatusOk_returnsUpdatedItemModel() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        WorkflowNoteModel workflowNoteModel = new WorkflowNoteModelBuilder().build();

        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), anyInt(), anyInt(), anyShort()))
                .thenReturn(new ItemInfoSnapshotBuilder().setWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel)).build());
        try {
            ItemModel updatedItemModel = jobStoreProxy.setWorkflowNote(workflowNoteModel, 1, 0, (short) 0);
            assertThat(updatedItemModel, is(notNullValue()));
            assertThat(updatedItemModel.getWorkflowNoteModel(), is(workflowNoteModel));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: setWorkflowNote()");
        }
    }

    @Test(expected = ProxyException.class)
    public void getSinkStatusModels_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.getSinkStatusList()).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.getSinkStatusModels();
    }

    @Test
    public void getSinkStatusModels_remoteServiceReturnsHttpStatusOk_returnsListOfSinkStatus() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        final SinkStatusSnapshot sinkStatusSnapshot = new SinkStatusSnapshot().withSinkId(1).withSinkType(SinkContent.SinkType.DUMMY).withName("testSink");
        final SinkStatusTable.SinkStatusModel expectedSinkStatusModel = new SinkStatusTable.SinkStatusModel().withSinkId(1).withSinkType("DUMMY").withName("testSink");
        when(jobStoreServiceConnector.getSinkStatusList()).thenReturn(Collections.singletonList(sinkStatusSnapshot));
        try {
            final List<SinkStatusTable.SinkStatusModel> sinkStatusModels = jobStoreProxy.getSinkStatusModels();
            assertThat("sinkStatusModels not null", sinkStatusModels, not(nullValue()));
            assertThat(sinkStatusModels.get(0), is(expectedSinkStatusModel));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemData()");
        }
    }

    @Test
    public void getSinkStatusModels_remoteServiceReturnsHttpStatusOk_returnsEmptyList() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        when(jobStoreServiceConnector.getSinkStatusList()).thenReturn(Collections.emptyList());
        try {
            List<SinkStatusTable.SinkStatusModel> sinkStatusSnapshots = jobStoreProxy.getSinkStatusModels();
            assertThat(sinkStatusSnapshots, is(notNullValue()));
            assertThat(sinkStatusSnapshots.isEmpty(), is(true));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemData()");
        }
    }

    @Test(expected = ProxyException.class)
    public void createJobRerun_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, JobStoreServiceConnectorException {
        doThrow(dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException.class).when(jobStoreServiceConnector).createJobRerun(anyInt(), anyBoolean());

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.createJobRerun(123, false);
    }

    @Test
    public void createJobRerun_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);

        try {
            jobStoreProxy.createJobRerun(321, false);
        } catch (ProxyException e) {
            fail("Unexpected error when calling: reRunJobs()");
        }
    }


    /*
     * private methods
     */

    private List<JobInfoSnapshot> getListOfJobInfoSnapshots() {
        List<JobInfoSnapshot> jobInfoSnapshots = new ArrayList<>();
        jobInfoSnapshots.add(getJobInfoSnapshot(new Date(System.currentTimeMillis() + 10000)));
        jobInfoSnapshots.add(getJobInfoSnapshot(new Date(System.currentTimeMillis() + 500)));
        jobInfoSnapshots.add(getJobInfoSnapshot(new Date()));
        return jobInfoSnapshots;
    }

    private JobInfoSnapshot getJobInfoSnapshot(Date date) {
        return new JobInfoSnapshot()
                .withJobId(Long.valueOf(ID).intValue())
                .withEoj(true)
                .withFatalError(false)
                .withPartNumber(0)
                .withNumberOfChunks(2)
                .withNumberOfItems(11)
                .withTimeOfCreation(date)
                .withTimeOfLastModification(new Date())
                .withTimeOfCompletion(new Date())
                .withSpecification(getJobSpecification())
                .withState(new State())
                .withFlowStoreReferences(new FlowStoreReferencesBuilder().build());
    }

    private JobSpecification getJobSpecification() {
        return new JobSpecification()
                .withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withDestination("destination")
                .withSubmitterId(222)
                .withMailForNotificationAboutVerification("")
                .withMailForNotificationAboutProcessing("")
                .withResultmailInitials("")
                .withDataFile("datafile")
                .withType(JobSpecification.Type.TEST);
    }

    private List<ItemInfoSnapshot> getListOfItemInfoSnapshots() {
        List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(3);
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildFailedAndIgnoredPhase(State.Phase.PROCESSING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 0).build());
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildPhaseCompletion(State.Phase.PROCESSING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 1).build());
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildPhaseCompletion(State.Phase.DELIVERING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 2).build());
        return itemInfoSnapshots;
    }

    private List<ItemInfoSnapshot> getFailedListOfItemInfoSnapshots() {
        List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(2);
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildFailedAndIgnoredPhase(State.Phase.PROCESSING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 0).build());
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildFailedAndIgnoredPhase(State.Phase.DELIVERING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 1).build());
        return itemInfoSnapshots;
    }

    private List<ItemInfoSnapshot> getIgnoredListOfItemInfoSnapshots() {
        List<ItemInfoSnapshot> itemInfoSnapshots = new ArrayList<>(2);
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildFailedAndIgnoredPhase(State.Phase.PARTITIONING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 0).build());
        itemInfoSnapshots.add(new ItemInfoSnapshotBuilder().setState(buildFailedAndIgnoredPhase(State.Phase.PROCESSING)).setJobId(Long.valueOf(ID).intValue()).setItemId((short) 1).build());
        return itemInfoSnapshots;
    }

    private State buildFailedAndIgnoredPhase(State.Phase phaseToFail) {
        State state = new State();
        switch (phaseToFail) {
            case PARTITIONING:
                state.getPhase(State.Phase.PARTITIONING).withFailed(1);
                state.getPhase(State.Phase.PROCESSING).withIgnored(1);
                state.getPhase(State.Phase.DELIVERING).withIgnored(1);
                break;
            case PROCESSING:
                state.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).withFailed(1);
                state.getPhase(State.Phase.DELIVERING).withIgnored(1);
                break;
            case DELIVERING:
                state.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).withSucceeded(1);
                state.getPhase(State.Phase.DELIVERING).withFailed(1);
        }
        return state;
    }

    private State buildPhaseCompletion(State.Phase lastPhaseCompleted) {
        State state = new State();
        switch (lastPhaseCompleted) {
            case DELIVERING:
                state.getPhase(State.Phase.DELIVERING).withSucceeded(1);
                state.getPhase(State.Phase.DELIVERING).withEndDate(new Date());
            case PROCESSING:
                state.getPhase(State.Phase.PROCESSING).withSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).withEndDate(new Date());
            case PARTITIONING:
                state.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
                state.getPhase(State.Phase.PARTITIONING).withEndDate(new Date());
        }
        return state;
    }

    private static String getXmlData() {
        return "<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data-supplementary><creationDate>20150601</creationDate>" +
                "<enrichmentTrail>191919,870970</enrichmentTrail>" +
                "</data-supplementary><data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader><datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">51761138</subfield>" +
                "<subfield code=\"b\">870970</subfield>" +
                "<subfield code=\"c\">20150601233812</subfield>" +
                "<subfield code=\"d\">20150528</subfield>" +
                "<subfield code=\"f\">a</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"004\">" +
                "<subfield code=\"r\">n</subfield>" +
                "<subfield code=\"a\">e</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"008\"><subfield code=\"t\">s</subfield>" +
                "<subfield code=\"v\">7</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"009\">" +
                "<subfield code=\"a\">s</subfield>" +
                "<subfield code=\"g\">xc</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"032\">" +
                "<subfield code=\"x\">ACM201522</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"110\">" +
                "<subfield code=\"a\">Sun Kil Moon</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<subfield code=\"a\">Universal themes</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"300\">" +
                "<subfield code=\"n\">1 cd</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"538\">" +
                "<subfield code=\"f\">Rough Trade</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"652\">" +
                "<subfield code=\"m\">NY TITEL</subfield>" +
                "</datafield><datafield ind1=\"0\" ind2=\"0\" tag=\"996\">" +
                "<subfield code=\"a\">DBC</subfield></datafield>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"d08\">" +
                "<subfield code=\"o\">cfp</subfield></datafield>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"d70\">" +
                "<subfield code=\"c\">20150601</subfield></datafield>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"s10\"><subfield code=\"a\">DBC</subfield></datafield>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"z98\"><subfield code=\"a\">Minus korrekturprint</subfield></datafield>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"z99\"><subfield code=\"a\">cfp</subfield></datafield>" +
                "</record></collection></data></data-container></dataio-harvester-datafile>";
    }
}
