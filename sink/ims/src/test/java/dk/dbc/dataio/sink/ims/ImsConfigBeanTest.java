package dk.dbc.dataio.sink.ims;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImsConfigBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new ImsSinkConfig()
            .withEndpoint("endpoint"));

    private ImsConfigBean imsConfigBean;

    @Before
    public void setup() {
        imsConfigBean = newImsConfigBean();
    }

    @Test
    public void getConfig_sinkNotFound_throws() throws FlowStoreServiceConnectorException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            imsConfigBean.getConfig(consumedMessage);
            fail();
        } catch (SinkException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConfig() throws FlowStoreServiceConnectorException, SinkException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        final ImsSinkConfig config = imsConfigBean.getConfig(consumedMessage);
        assertThat(config, is(notNullValue()));
        verify(flowStoreServiceConnector, times(1)).getSink(sink.getId());
    }

    @Test
    public void getConfig_configRefreshedOnlyWhenVersionChanges() throws SinkException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        final ImsSinkConfig config = imsConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("1st refresh", config, is(notNullValue()));

        final ImsSinkConfig configUnchanged = imsConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("no refresh", config, is(configUnchanged));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(newSink(new ImsSinkConfig()
                .withEndpoint("newEndpoint")));

        final ImsSinkConfig configChanged = imsConfigBean.getConfig(newConsumedMessage(10, 2));
        assertThat("2nd refresh", configChanged, is(not(config)));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    private ImsConfigBean newImsConfigBean() {
        final ImsConfigBean configBean = new ImsConfigBean();
        configBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        when(configBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        return configBean;
    }

    private ConsumedMessage newConsumedMessage(long sinkId, long sinkVersion) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink newSink(ImsSinkConfig config) {
        final SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.IMS)
                .setSinkConfig(config)
                .build();

        return new SinkBuilder().setContent(sinkContent).build();
    }

    private String newPayload(Chunk chunk) {
        try {
            return new JSONBContext().marshall(chunk);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
