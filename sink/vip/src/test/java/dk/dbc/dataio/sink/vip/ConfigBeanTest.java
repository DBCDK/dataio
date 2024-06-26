package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = createPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = createSink(new VipSinkConfig().withEndpoint("endpoint"));
    private ConfigBean configBean;

    @BeforeEach
    public void setup() {
        configBean = new ConfigBean(flowStoreServiceConnector);
    }

    @Test
    public void sinkNotFound() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = createConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));
        try {
            configBean.getConfig(consumedMessage);
            Assertions.fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConfig() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = createConsumedMessage(42, 1);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        VipSinkConfig config = configBean.getConfig(consumedMessage);
        assertThat(config, is(notNullValue()));
        verify(flowStoreServiceConnector, times(1)).getSink(sink.getId());
    }

    @Test
    public void configRefreshedOnlyWhenVersionChanges() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        VipSinkConfig config = configBean.getConfig(createConsumedMessage(10, 1));
        assertThat("1st refresh", config, is(notNullValue()));

        VipSinkConfig configUnchanged = configBean.getConfig(createConsumedMessage(10, 1));
        assertThat("no refresh", config, is(configUnchanged));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(createSink(new VipSinkConfig().withEndpoint("newEndpoint")));

        VipSinkConfig configChanged = configBean.getConfig(createConsumedMessage(10, 2));
        assertThat("2nd refresh", configChanged, is(not(config)));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    private ConsumedMessage createConsumedMessage(long sinkId, long sinkVersion) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink createSink(VipSinkConfig config) {
        SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.VIP)
                .setSinkConfig(config)
                .build();
        return new SinkBuilder().setContent(sinkContent).build();
    }

    private String createPayload(Chunk chunk) {
        try {
            return new JSONBContext().marshall(chunk);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
