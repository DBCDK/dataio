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

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.server.modelmappers.WorkflowNoteModelMapper;
import dk.dbc.dataio.gui.server.util.PrettyPrint;
import dk.dbc.dataio.jobstore.test.types.ItemInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyShort;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class,
    Format.class
})
public class JobStoreProxyImplTest {
    private final Client client = mock(Client.class);
    private final dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector jobStoreServiceConnector = mock(dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector.class);
    private final long ID = 737L;

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        mockStatic(Format.class);
        String jobStoreServiceUrl = "http://dataio/job-service";
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenReturn(jobStoreServiceUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_jobStoreProxyConstructorJobStoreService_EndpointCanNotBeLookedUp_throws() throws Exception{
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new JobStoreProxyImpl();
            fail();
        }catch (NamingException e){

        }
    }


    @Test(expected = ProxyException.class)
    public void listJobs_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenThrow(new dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException("Testing"));

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
    public void listItems_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listItems(any(ItemListCriteria.class))).thenThrow(new dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException("Testing"));

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
            assertThat(itemModels.get(0).getStatus(), is(ItemModel.LifeCycle.PROCESSING));
            assertThat(itemModels.get(1).getStatus(), is(ItemModel.LifeCycle.DELIVERING));
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
            assertThat(itemModels.get(0).getStatus(), is(ItemModel.LifeCycle.PROCESSING));
            assertThat(itemModels.get(1).getStatus(), is(ItemModel.LifeCycle.DELIVERING));
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
    public void getChunkItem_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
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
            assertThat(data, is(""));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemData()");
        }
    }

    @Test(expected = ProxyException.class)
    public void getProcessedNextResult_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
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
    public void listJobNotificationsForJob_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobNotificationsForJob(any(Integer.class))).thenThrow(new dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.listJobNotificationsForJob(123);
    }


    @Test
    public void listJobNotificationsForJob_remoteServiceReturnsHttpStatusOk_returnsListOfJobModelEntities() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        List<JobNotification> testJobNotifications = new ArrayList<>();
        testJobNotifications.add(new JobNotification(11, null, null, JobNotification.Type.JOB_CREATED, JobNotification.Status.WAITING, "status1", "dest1", "content1", 1111));
        testJobNotifications.add(new JobNotification(22, null, null, JobNotification.Type.JOB_COMPLETED, JobNotification.Status.COMPLETED, "status2", "dest2", "content2", 2222));
        testJobNotifications.add(new JobNotification(33, null, null, JobNotification.Type.JOB_COMPLETED, JobNotification.Status.FAILED, "status3", "dest3", "content3", 3333));

        when(jobStoreServiceConnector.listJobNotificationsForJob(any(Integer.class))).thenReturn(testJobNotifications);
        try {
            List<JobNotification> jobNotifications = jobStoreProxy.listJobNotificationsForJob(2222);
            assertThat(jobNotifications, is(testJobNotifications));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: listJobs()");
        }
    }

    @Test(expected = ProxyException.class)
    public void setWorkflowNoteForJob_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), (any(Integer.class)))).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.setWorkflowNote(new WorkflowNoteModelBuilder().build(), 1);
    }

    @Test
    public void setWorkflowNoteForJob_remoteServiceReturnsHttpStatusOk_returnsUpdatedJobModel() throws Exception {
        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        WorkflowNoteModel workflowNoteModel = new WorkflowNoteModelBuilder().build();

        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), anyInt()))
                .thenReturn(new JobInfoSnapshotBuilder().setWorkflowNote(WorkflowNoteModelMapper.toWorkflowNote(workflowNoteModel)).build());
        try {
            JobModel updatedJobModel = jobStoreProxy.setWorkflowNote(workflowNoteModel, 1);
            assertThat(updatedJobModel, is(notNullValue()));
            assertThat(updatedJobModel.getWorkflowNoteModel(), is(workflowNoteModel));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: setWorkflowNote()");
        }
    }

    @Test(expected = ProxyException.class)
    public void setWorkflowNoteForItem_jobStoreServiceConnectorException_throwsProxyException() throws ProxyException, NamingException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.setWorkflowNote(any(WorkflowNote.class), anyInt(), anyInt(), anyShort())).thenThrow(new JobStoreServiceConnectorException("Testing"));

        final JobStoreProxyImpl jobStoreProxy = new JobStoreProxyImpl(jobStoreServiceConnector);
        jobStoreProxy.setWorkflowNote(new WorkflowNoteModelBuilder().build(), 1, 0, (short)0);
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


   /*
     * private methods
     */

    private List<JobInfoSnapshot> getListOfJobInfoSnapshots() {
        List<JobInfoSnapshot> jobInfoSnapshots = new ArrayList<>();
        jobInfoSnapshots.add(getJobInfoSnapShot(new Date(System.currentTimeMillis() + 10000)));
        jobInfoSnapshots.add(getJobInfoSnapShot(new Date(System.currentTimeMillis() + 500)));
        jobInfoSnapshots.add(getJobInfoSnapShot(new Date()));
        return jobInfoSnapshots;
    }

    private JobInfoSnapshot getJobInfoSnapShot(Date date) {
        return new JobInfoSnapshotBuilder().setJobId(Long.valueOf(ID).intValue()).setTimeOfCreation(date).build();
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
                state.getPhase(State.Phase.PARTITIONING).setFailed(1);
                state.getPhase(State.Phase.PROCESSING).setIgnored(1);
                state.getPhase(State.Phase.DELIVERING).setIgnored(1);
                break;
            case PROCESSING:
                state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).setFailed(1);
                state.getPhase(State.Phase.DELIVERING).setIgnored(1);
                break;
            case DELIVERING:
                state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).setSucceeded(1);
                state.getPhase(State.Phase.DELIVERING).setFailed(1);
        }
        return state;
    }

    private State buildPhaseCompletion(State.Phase lastPhaseCompleted) {
        State state = new State();
        switch (lastPhaseCompleted) {
            case DELIVERING:
                state.getPhase(State.Phase.DELIVERING).setSucceeded(1);
                state.getPhase(State.Phase.DELIVERING).setEndDate(new Date());
            case PROCESSING:
                state.getPhase(State.Phase.PROCESSING).setSucceeded(1);
                state.getPhase(State.Phase.PROCESSING).setEndDate(new Date());
            case PARTITIONING:
                state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
                state.getPhase(State.Phase.PARTITIONING).setEndDate(new Date());
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
