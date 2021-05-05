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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * This is a simple white-box test of the EsCleanupBean.cleanup() method.
 */
public class EsCleanupBeanTest {
    private EsInFlightBean esInFlightAdmin;
    private EsConnectorBean esConnector;
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private AddiRecordPreprocessor addiRecordPreprocessor;
    private EsInFlight esInFlight41_1;
    private EsInFlight esInFlight42_1;
    private EsInFlight esInFlight42_2;
    private EsInFlight esInFlight43_1;
    private TaskStatus taskStatus_122;
    private TaskStatus taskStatus_123;
    private TaskStatus taskStatus_124;
    private TaskStatus taskStatus_125;
    private List<EsInFlight> emptyEsInFlightList = Collections.emptyList();
    private final static int SINK_ID = 333;
    private EntityManager entityManager = mock(EntityManager.class);
    private final SinkContent sinkContent = new SinkContentBuilder().setSinkConfig(new EsSinkConfig().withUserId(42).withDatabaseName("dbname")).build();
    private final Sink sink = new SinkBuilder().setId(SINK_ID).setContent(sinkContent).build();
    private final String simpleAddiString = "1\na\n1\nb\n";

    @Before
    public void setup() throws JSONBException {

        final Chunk incompleteDeliveredChunk = new ChunkBuilder(Chunk.Type.DELIVERED).setItems(
                Collections.singletonList(new ChunkItemBuilder().setStatus(ChunkItem.Status.SUCCESS).build())).build();
        String incompleteDeliveredChunkJson = new JSONBContext().marshall(incompleteDeliveredChunk);

        esInFlight41_1 = new EsInFlight();
        esInFlight41_1.setJobId(41L);
        esInFlight41_1.setChunkId(1L);
        esInFlight41_1.setTargetReference(122);
        esInFlight41_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight42_1 = new EsInFlight();
        esInFlight42_1.setJobId(42L);
        esInFlight42_1.setChunkId(1L);
        esInFlight42_1.setTargetReference(123);
        esInFlight42_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight42_2 = new EsInFlight();
        esInFlight42_2.setJobId(42L);
        esInFlight42_2.setChunkId(2L);
        esInFlight42_2.setTargetReference(124);
        esInFlight42_2.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight43_1 = new EsInFlight();
        esInFlight43_1.setJobId(43L);
        esInFlight43_1.setChunkId(1L);
        esInFlight43_1.setTargetReference(125);
        esInFlight43_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);
        esInFlight43_1.setRedelivered(EsCleanupBean.MAX_REDELIVERING_ATTEMPTS);

        taskStatus_122 = new TaskStatus(TaskPackageEntity.TaskStatus.ABORTED, 122);
        taskStatus_123 = new TaskStatus(TaskPackageEntity.TaskStatus.COMPLETE, 123);
        taskStatus_124 = new TaskStatus(TaskPackageEntity.TaskStatus.ACTIVE, 124);
        taskStatus_125 = new TaskStatus(TaskPackageEntity.TaskStatus.PENDING, 125);
    }

    @Before
    public void setupMocks() {
        esInFlightAdmin = mock(EsInFlightBean.class);
        esInFlightAdmin.entityManager = entityManager;
        esConnector = mock(EsConnectorBean.class);
        jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);

        addiRecordPreprocessor = mock(AddiRecordPreprocessor.class);
    }

    @Test
    public void cleanup_emptyEsInFlight_nothingHappens() {
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(emptyEsInFlightList);

        getEsCleanupBean().cleanup();
    }

    @Test
    public void cleanup_noFinishedInFlight_nothingHappens() throws SinkException {
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(Collections.singletonList(esInFlight42_2));
        final HashMap<Integer, TaskStatus> taskStatusMap = new HashMap<>();
        taskStatusMap.put(taskStatus_124.getTargetReference(), taskStatus_124);
        when(esConnector.getCompletionStatusForESTaskpackages(anyList())).thenReturn(taskStatusMap);

        getEsCleanupBean().cleanup();
    }

    @Test
    public void cleanup_AbortedCompletedActivePendingInFlight_AbortedCompletedIsCleanedUp() throws SinkException, JobStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));
        final HashMap<Integer, TaskStatus> taskStatusMap = new HashMap<>();
        taskStatusMap.put(taskStatus_122.getTargetReference(), taskStatus_122);
        taskStatusMap.put(taskStatus_123.getTargetReference(), taskStatus_123);
        taskStatusMap.put(taskStatus_124.getTargetReference(), taskStatus_124);
        taskStatusMap.put(taskStatus_125.getTargetReference(), taskStatus_125);
        when(esConnector.getCompletionStatusForESTaskpackages(anyList())).thenReturn(taskStatusMap);
        when(esConnector.getChunkForTaskPackage(anyInt(), any(Chunk.class)))
                .thenReturn(new ChunkBuilder(Chunk.Type.DELIVERED).build());

        getEsCleanupBean().cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight42_1));
        verify(esConnector).deleteESTaskpackages(anyList());
        verify(jobStoreServiceConnector, times(2)).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void cleanup_InFlightNoTargetReferenceFound_chunkIsRedelivered() throws SinkException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException, JSONBException {
        when(esInFlightAdmin.buildEsInFlight(any(Chunk.class), anyInt(),anyString(), anyInt(), anyLong())).thenReturn(esInFlight41_1);
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(Collections.singletonList(esInFlight41_1));
        when(esConnector.getCompletionStatusForESTaskpackages(anyList()))
                .thenReturn(Collections.emptyMap());

        when(jobStoreServiceConnector.getChunkItem(anyInt(), anyInt(), anyShort(), any(State.Phase.class)))
                .thenReturn(new ChunkItem().withId(0).withStatus(ChunkItem.Status.SUCCESS).withData(simpleAddiString.getBytes()));

        when(flowStoreServiceConnector.getSink(SINK_ID)).thenReturn(sink);

        EsCleanupBean esCleanupBean = getEsCleanupBean();
        when(esCleanupBean.addiRecordPreprocessor.execute(any(AddiRecord.class), anyString())).thenReturn(new AddiRecord("meta".getBytes(), "content".getBytes()));
        esCleanupBean.cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight41_1));
        verify(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        verify(esConnector, times(0)).deleteESTaskpackages(anyList());
        verify(jobStoreServiceConnector, times(0)).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void cleanup_InFlightNoTargetReferenceFound_lostChunkIsCleanedUp() throws SinkException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(Collections.singletonList(esInFlight43_1));
        when(esConnector.getCompletionStatusForESTaskpackages(anyList()))
                .thenReturn(Collections.emptyMap());

        when(jobStoreServiceConnector.getChunkItem(anyInt(), anyInt(), anyShort(), any(State.Phase.class)))
                .thenReturn(new ChunkItem().withId(0).withStatus(ChunkItem.Status.SUCCESS).withData(simpleAddiString.getBytes()));

        when(flowStoreServiceConnector.getSink(anyInt())).thenReturn(sink);

        EsCleanupBean esCleanupBean = getEsCleanupBean();
        when(esCleanupBean.addiRecordPreprocessor.execute(any(AddiRecord.class), anyString())).thenReturn(new AddiRecord("meta".getBytes(), "content".getBytes()));
        esCleanupBean.cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight43_1));
        verify(esInFlightAdmin, times(0)).addEsInFlight(any(EsInFlight.class));
        verify(esConnector, times(0)).deleteESTaskpackages(anyList());
        verify(jobStoreServiceConnector, times(1)).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void cleanup_AbortedCompletedActivePendingInFlight_AbortedCompletedAndNoTargetReferencedAreCleanedUp() throws SinkException, JobStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight(SINK_ID)).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));
        final HashMap<Integer, TaskStatus> taskStatusMap = new HashMap<>();
        taskStatusMap.put(taskStatus_122.getTargetReference(), taskStatus_122);
        taskStatusMap.put(taskStatus_123.getTargetReference(), taskStatus_123);
        taskStatusMap.put(taskStatus_124.getTargetReference(), taskStatus_124);
        when(esConnector.getCompletionStatusForESTaskpackages(anyList()))
                .thenReturn(taskStatusMap);
        when(esConnector.getChunkForTaskPackage(anyInt(), any(Chunk.class)))
                .thenReturn(new ChunkBuilder(Chunk.Type.DELIVERED).build());

        getEsCleanupBean().cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight42_1));
        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight43_1));
        verify(esConnector).deleteESTaskpackages(anyList());
        verify(jobStoreServiceConnector, times(3)).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void createLostChunk_fillsOutValuesCorrectly() throws JSONBException, SinkException {
        String originalChunkString = esInFlight43_1.getIncompleteDeliveredChunk();
        Chunk originalChunk = new JSONBContext().unmarshall(originalChunkString, Chunk.class);

        Chunk lostChunk = getEsCleanupBean().createLostChunk(esInFlight43_1);
        assertThat("ModifiedChunk not null", lostChunk, not(nullValue()));
        assertThat("ModifiedChunk.jobId, is OriginalChunk.jobId", lostChunk.getJobId(), is(originalChunk.getJobId()));
        assertThat("ModifiedChunk.chunkId, is OriginalChunk.chunkId", lostChunk.getChunkId(), is(originalChunk.getChunkId()));
        assertThat("ModifiedChunk.type, is OriginalChunk.type", lostChunk.getType(), is(originalChunk.getType()));
        assertThat("ModifiedChunk.encoding, is OriginalChunk.encoding", lostChunk.getEncoding(), is(originalChunk.getEncoding()));

        assertThat("OriginalChunk.chunkItems.size", originalChunk.size(), is(1));
        Iterator<ChunkItem> originalChunkItemIterator = originalChunk.iterator();
        ChunkItem originalChunkItem = originalChunkItemIterator.next();
        assertThat("OriginalChunk.ChunkItem.Status", originalChunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("OriginalChunk.ChunkItem.Diagnostics", originalChunkItem.getDiagnostics(), is(nullValue()));

        assertThat(lostChunk.size(), is(originalChunk.size()));
        Iterator<ChunkItem> modifiedChunkItemIterator = lostChunk.iterator();
        ChunkItem modifiedChunkItem = modifiedChunkItemIterator.next();
        assertThat("ModifiedChunk.ChunkItem.id, is OriginalChunk.ChunkItem.id", modifiedChunkItem.getId(), is(originalChunkItem.getId()));
        assertThat("ModifiedChunk.ChunkItem.data, is OriginalChunk.ChunkItem.data", modifiedChunkItem.getData(), not(originalChunkItem.getData()));
        assertThat("ModifiedChunk.ChunkItem.Status", modifiedChunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ModifiedChunk.ChunkItem.Diagnostics", modifiedChunkItem.getDiagnostics().size(), is(1));
        assertThat("ModifiedChunk.ChunkItem.Diagnostics.Stacktrace", modifiedChunkItem.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
    }

    private EsCleanupBean getEsCleanupBean() {
        final EsCleanupBean esCleanupBean = new EsCleanupBean();
        esCleanupBean.esInFlightAdmin = esInFlightAdmin;
        esCleanupBean.esConnector = esConnector;

        esCleanupBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        esCleanupBean.jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();

        esCleanupBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        esCleanupBean.flowStoreServiceConnector = flowStoreServiceConnectorBean.getConnector();

        esCleanupBean.sinkId = SINK_ID;
        esCleanupBean.jsonbContext = new JSONBContext();
        esCleanupBean.addiRecordPreprocessor = addiRecordPreprocessor;
        return esCleanupBean;
    }
}
