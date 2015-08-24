package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

/*
 * This is a simple white-box test of the EsCleanupBean.cleanup() method.
 */
public class EsCleanupBeanTest {
    private EsInFlightBean esInFlightAdmin;
    private EsConnectorBean esConnector;
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private EsInFlight esInFlight41_1;
    private EsInFlight esInFlight42_1;
    private EsInFlight esInFlight42_2;
    private EsInFlight esInFlight43_1;
    private TaskStatus taskStatus_122;
    private TaskStatus taskStatus_123;
    private TaskStatus taskStatus_124;
    private TaskStatus taskStatus_125;
    private List<EsInFlight> emptyEsInFlightList = Collections.emptyList();

    @Before
    public void setup() throws JsonException {

        final ExternalChunk incompleteDeliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).setItems(
                    Arrays.asList(new ChunkItemBuilder().setStatus(ChunkItem.Status.SUCCESS).build())).build();
        String incompleteDeliveredChunkJson = JsonUtil.toJson(incompleteDeliveredChunk);
                
        esInFlight41_1 = new EsInFlight();
        esInFlight41_1.setJobId(41L);
        esInFlight41_1.setChunkId(1L);
        esInFlight41_1.setTargetReference(122);
        esInFlight41_1.setRecordSlots(3);
        esInFlight41_1.setResourceName("resource 1");
        esInFlight41_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight42_1 = new EsInFlight();
        esInFlight42_1.setJobId(42L);
        esInFlight42_1.setChunkId(1L);
        esInFlight42_1.setTargetReference(123);
        esInFlight42_1.setRecordSlots(10);
        esInFlight42_1.setResourceName("resource 1");
        esInFlight42_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight42_2 = new EsInFlight();
        esInFlight42_2.setJobId(42L);
        esInFlight42_2.setChunkId(2L);
        esInFlight42_2.setTargetReference(124);
        esInFlight42_2.setRecordSlots(5);
        esInFlight42_2.setResourceName("resource 1");
        esInFlight42_2.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        esInFlight43_1 = new EsInFlight();
        esInFlight43_1.setJobId(43L);
        esInFlight43_1.setChunkId(1L);
        esInFlight43_1.setTargetReference(125);
        esInFlight43_1.setRecordSlots(10);
        esInFlight43_1.setResourceName("resource 1");
        esInFlight43_1.setIncompleteDeliveredChunk(incompleteDeliveredChunkJson);

        taskStatus_122 = new TaskStatus(3, 122);
        taskStatus_123 = new TaskStatus(2, 123);
        taskStatus_124 = new TaskStatus(1, 124);
        taskStatus_125 = new TaskStatus(0, 125);
    }

    @Before
    public void setupMocks() {
        esInFlightAdmin = mock(EsInFlightBean.class);
        esConnector = mock(EsConnectorBean.class);
        jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void cleanup_emptyEsInFlight_nothingHappens() {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(emptyEsInFlightList);

        getEsCleanupBean().cleanup();
    }

    @Test
    public void cleanup_noFinishedInFlight_nothingHappens() throws SinkException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight42_2));
        when(esConnector.getCompletionStatusForESTaskpackages(anyListOf(Integer.class))).thenReturn(Arrays.asList(taskStatus_124));

        getEsCleanupBean().cleanup();
    }

    @Test
    public void cleanup_AbortedCompletedActivePendingInFlight_AbortedCompletedIsCleanedUp() throws SinkException, JobStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));
        when(esConnector.getCompletionStatusForESTaskpackages(anyListOf(Integer.class))).thenReturn(Arrays.asList(taskStatus_122, taskStatus_123, taskStatus_124, taskStatus_125));
        final ArrayList<ChunkItem> esItems = new ArrayList<>();
        esItems.add(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(StringUtil.asBytes("succeeded"))
                .build());
        when(esConnector.getChunkForTaskPackage(anyInt(), any(ExternalChunk.class)))
                .thenReturn(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());

        getEsCleanupBean().cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight42_1));
        verify(esConnector).deleteESTaskpackages(anyListOf(Integer.class));
        verify(jobStoreServiceConnector, times(2)).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }

    @Test
    public void cleanup_InFlightNoTargetReferenceFound_lostChunkIsCleanedUp() throws SinkException, JSONBException, JobStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight43_1));
        when(esConnector.getCompletionStatusForESTaskpackages(anyListOf(Integer.class))).thenReturn(new ArrayList());

        getEsCleanupBean().cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight43_1));
        verify(esConnector, times(0)).deleteESTaskpackages(anyListOf(Integer.class));
        verify(jobStoreServiceConnector, times(1)).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }

    @Test
    public void cleanup_AbortedCompletedActivePendingInFlight_AbortedCompletedAndNoTargetReferencedAreCleanedUp() throws SinkException, JobStoreServiceConnectorException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));
        when(esConnector.getCompletionStatusForESTaskpackages(anyListOf(Integer.class))).thenReturn(Arrays.asList(taskStatus_122, taskStatus_123, taskStatus_124));
        final ArrayList<ChunkItem> esItems = new ArrayList<>();
        esItems.add(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(StringUtil.asBytes("succeeded"))
                .build());
        when(esConnector.getChunkForTaskPackage(anyInt(), any(ExternalChunk.class)))
                .thenReturn(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build());

        getEsCleanupBean().cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight42_1));
        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight43_1));
        verify(esConnector).deleteESTaskpackages(anyListOf(Integer.class));
        verify(jobStoreServiceConnector, times(3)).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }

    @Test
    public void createLostChunk_fillsOutValuesCorrectly() throws JSONBException, SinkException {
        String originalChunkString = esInFlight43_1.getIncompleteDeliveredChunk();
        ExternalChunk originalExternalChunk = new JSONBContext().unmarshall(originalChunkString, ExternalChunk.class);

        ExternalChunk lostExternalChunk = getEsCleanupBean().createLostChunk(esInFlight43_1);
        assertThat("ModifiedExternalChunk not null", lostExternalChunk, not(nullValue()));
        assertThat("ModifiedExternalChunk.jobId, is OriginalExternalChunk.jobId", lostExternalChunk.getJobId(), is(originalExternalChunk.getJobId()));
        assertThat("ModifiedExternalChunk.chunkId, is OriginalExternalChunk.chunkId", lostExternalChunk.getChunkId(), is(originalExternalChunk.getChunkId()));
        assertThat("ModifiedExternalChunk.type, is OriginalExternalChunk.type", lostExternalChunk.getType(), is(originalExternalChunk.getType()));
        assertThat("ModifiedExternalChunk.encoding, is OriginalExternalChunk.encoding", lostExternalChunk.getEncoding(), is(originalExternalChunk.getEncoding()));

        assertThat("OriginalExternalChunk.chunkItems.size", originalExternalChunk.size(), is(1));
        Iterator<ChunkItem> originalChunkItemIterator = originalExternalChunk.iterator();
        ChunkItem originalChunkItem = originalChunkItemIterator.next();
        assertThat("OriginalExternalChunk.ChunkItem.Status", originalChunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat(lostExternalChunk.size(), is(originalExternalChunk.size()));
        Iterator<ChunkItem> modifiedChunkItemIterator = lostExternalChunk.iterator();
        ChunkItem modifiedChunkItem = modifiedChunkItemIterator.next();
        assertThat("ModifiedExternalChunk.ChunkItem.id, is OriginalExternalChunk.ChunkItem.id", modifiedChunkItem.getId(), is(originalChunkItem.getId()));
        assertThat("ModifiedExternalChunk.ChunkItem.data, is OriginalExternalChunk.ChunkItem.data", modifiedChunkItem.getData(), not(originalChunkItem.getData()));
        assertThat("ModifiedExternalChunk.ChunkItem.Status", modifiedChunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    private EsCleanupBean getEsCleanupBean() {
        final EsCleanupBean esCleanupBean = new EsCleanupBean();
        esCleanupBean.esInFlightAdmin = esInFlightAdmin;
        esCleanupBean.esConnector = esConnector;
        esCleanupBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return esCleanupBean;
    }
}
