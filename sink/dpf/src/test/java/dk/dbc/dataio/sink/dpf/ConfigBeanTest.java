package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.*;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class ConfigBeanTest {
    private final FlowStoreServiceConnector flowStore = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new DpfSinkConfig()
            .withUpdateServiceUserId("userId")
            .withUpdateServicePassword("password")
            .withUpdateServiceAvailableQueueProviders(Collections.singletonList("dpf")));
    private final FlowBinder flowBinder = newFlowBinder("queueProvider");

    private ConfigBean configBean;

    @Before
    public void setup() {
        configBean = newConfigBean();
    }

    @Test
    public void sinkNotFoundOnRefresh() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStore.getSink(anyLong()))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            configBean.refresh(consumedMessage);
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void flowBinderNotFoundOnRefresh() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = newConsumedMessage(10, 1);
        final String message = "Error message from flowStore";
        when(flowStore.getSink(10L)).thenReturn(sink);
        when(flowStore.getFlowBinder(anyLong()))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            configBean.refresh(consumedMessage);
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void configRefreshesOnlyWhenVersionChanges() throws FlowStoreServiceConnectorException {
        when(flowStore.getSink(eq(10L))).thenReturn(sink);
        when(flowStore.getFlowBinder(eq(10L))).thenReturn(flowBinder);

        configBean.refresh(newConsumedMessage(10, 1));
        DpfSinkConfig config = configBean.getConfig();
        assertThat("1st refresh", config, is(sink.getContent().getSinkConfig()));

        configBean.refresh(newConsumedMessage(10, 1));
        DpfSinkConfig configUnchanged = configBean.getConfig();
        assertThat("no refresh", config, is(sameInstance(configUnchanged)));

        when(flowStore.getSink(10L)).thenReturn(newSink(new DpfSinkConfig()
                .withUpdateServiceUserId("newUserId")
                .withUpdateServicePassword("newPassword")));

        configBean.refresh(newConsumedMessage(10, 2));
        DpfSinkConfig configChanged = configBean.getConfig();
        assertThat("2nd refresh", configChanged, is(not(sameInstance(config))));

        verify(flowStore, times(2)).getSink(10L);
    }

    @Test
    public void queueProviderRefreshesOnlyWhenFlowBinderVersionChanges() throws FlowStoreServiceConnectorException {
        when(flowStore.getSink(10L)).thenReturn(sink);
        when(flowStore.getFlowBinder(10L)).thenReturn(flowBinder);

        configBean.refresh(newConsumedMessage(10, 1));
        assertThat("1st refresh", configBean.getQueueProvider(),
                is(flowBinder.getContent().getQueueProvider()));

        configBean.refresh(newConsumedMessage(10, 1));
        assertThat("no refresh", configBean.getQueueProvider(),
                is(sameInstance(flowBinder.getContent().getQueueProvider())));

        when(flowStore.getFlowBinder(10L))
                .thenReturn(newFlowBinder("updatedQueueProvider"));

        configBean.refresh(newConsumedMessage(10, 2));
        assertThat("2nd refresh", configBean.getQueueProvider(),
                is(not(sameInstance(flowBinder.getContent().getQueueProvider()))));

        verify(flowStore, times(2)).getSink(10L);
    }

    private ConfigBean newConfigBean() {
        return new ConfigBean(flowStore);
    }

    private ConsumedMessage newConsumedMessage(long id, long version) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, id);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, version);
        headers.put(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, id);
        headers.put(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, version);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink newSink(DpfSinkConfig config) {
        SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.DPF)
                .setSinkConfig(config)
                .build();
        return new SinkBuilder().setContent(sinkContent).build();
    }

    private FlowBinder newFlowBinder(String queueProvider) {
        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setQueueProvider(queueProvider)
                .build();
        return new FlowBinderBuilder().setContent(flowBinderContent).build();
    }

    private String newPayload(Chunk chunk) {
        try {
            return new JSONBContext().marshall(chunk);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
