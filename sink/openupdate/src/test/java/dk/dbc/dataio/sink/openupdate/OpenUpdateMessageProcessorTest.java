package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import org.eclipse.microprofile.metrics.Counter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenUpdateMessageProcessorTest {
    /* mocks */
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final OpenUpdateConfig openUpdateConfig = mock(OpenUpdateConfig.class);
    private final AddiRecordPreprocessor addiRecordPreprocessor = Mockito.spy(new AddiRecordPreprocessor());
    private final Counter chunkitemsCounter = mock(Counter.class);
    private final OpenUpdateSinkConfig config = new OpenUpdateSinkConfig().withEndpoint("testEndpoint").withUserId("testUser").withPassword("testPass");
    private final JSONBContext jsonbContext = new JSONBContext();
    private final String queueProvider = "queue";
    private final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder().setQueueProvider(queueProvider).build();
    private final FlowBinder flowBinder = new FlowBinderBuilder().setId(42).setVersion(10).setContent(flowBinderContent).build();
    private final ServiceHub serviceHub = new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build();
    private final OpenUpdateMessageProcessor openUpdateMessageProcessor = new OpenUpdateMessageProcessor(serviceHub, flowStoreServiceConnector, openUpdateConfig, addiRecordPreprocessor);

    public static byte[] getValidAddi(String... content) {
        StringBuilder addi = new StringBuilder();
        for (String s : content) {
            addi.append(String.format("19\n<es:referencedata/>\n%d\n%s\n",
                    s.getBytes(StandardCharsets.UTF_8).length, s));
        }
        return addi.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Before
    public void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId())).thenReturn(flowBinder);
        when(openUpdateConfig.getConfig(any(ConsumedMessage.class))).thenReturn(config);
        doNothing().when(chunkitemsCounter).inc();
    }

    @Test
    public void handleConsumedMessage_jobStoreCommunicationFails_throws() throws InvalidMessageException, JobStoreServiceConnectorException {
        JobStoreServiceConnectorException jobStoreServiceConnectorException = new JobStoreServiceConnectorException("Exception from job-store");
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong()))
                .thenThrow(jobStoreServiceConnectorException);

        try {
            openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
            fail("No RuntimeException thrown");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(jobStoreServiceConnectorException));
        }
    }

    @Test
    public void handleConsumedMessage_flowStoreCommunicationFails_throws() throws InvalidMessageException, FlowStoreServiceConnectorException {
        FlowStoreServiceConnectorException flowStoreServiceConnectorException =
                new FlowStoreServiceConnectorException("Exception from flow-store");

        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId()))
                .thenThrow(flowStoreServiceConnectorException);

        try {
            openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
            fail("No SinkException thrown");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(flowStoreServiceConnectorException));
        }
    }

    @Test
    public void handleConsumedMessage_cachesFlowBinder() throws InvalidMessageException, FlowStoreServiceConnectorException {
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
        assertThat(openUpdateMessageProcessor.cachedFlowBinders.getIfPresent(flowBinder.getId()), is(notNullValue()));

        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
    }

    @Test
    public void handleConsumedMessage_callsAddiRecordPreprocessorWithQueueProvider() throws InvalidMessageException {
        ChunkItem chunkItem = new ChunkItemBuilder().setData(getValidAddi("addi content")).build();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(List.of(chunkItem)).build();
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(chunk));

        verify(addiRecordPreprocessor).preprocess(any(AddiRecord.class), eq(queueProvider));
    }

    @Test
    public void handleConsumedMessage_setsConfig() throws InvalidMessageException {
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat(openUpdateMessageProcessor.config, is(config));
    }

    @Test
    public void handleConsumedMessage_setsConnector() throws InvalidMessageException {
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        OpenUpdateServiceConnector connector = openUpdateMessageProcessor.connector;
        assertThat("1st message creates", connector, is(notNullValue()));
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat("2nd message retains", openUpdateMessageProcessor.connector, is(connector));
    }

    @Test
    public void handleConsumedMessage_updatesConnector() throws InvalidMessageException {
        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        OpenUpdateServiceConnector connector = openUpdateMessageProcessor.connector;
        assertThat("1st message creates", connector, is(notNullValue()));

        OpenUpdateSinkConfig updatedConfig = new OpenUpdateSinkConfig()
                .withEndpoint("updatedEndpoint")
                .withUserId("updatedUser")
                .withPassword("updatedPass");
        when(openUpdateConfig.getConfig(any(ConsumedMessage.class))).thenReturn(updatedConfig);

        openUpdateMessageProcessor.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat("2nd message updates", openUpdateMessageProcessor.connector, is(not(connector)));
    }

    private ConsumedMessage getConsumedMessageForChunk(Chunk chunk) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, 42L);
            headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, 10L);
            headers.put(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, flowBinder.getId());
            headers.put(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, flowBinder.getVersion());

            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk getIgnoredChunk() {
        ChunkItem chunkItem = new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build();
        return new ChunkBuilder(Chunk.Type.PROCESSED).setItems(List.of(chunkItem)).build();
    }
}
