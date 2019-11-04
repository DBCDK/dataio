/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new DpfSinkConfig()
            .withUpdateServiceUserId("userId")
            .withUpdateServicePassword("password")
            .withUpdateServiceAvailableQueueProviders(Collections.singletonList("dpf")));

    private ConfigBean configBean;

    @Before
    public void setup() {
        configBean = newConfigBean();
    }

    @Test
    public void sinkNotFoundOnRefresh() throws FlowStoreServiceConnectorException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStoreServiceConnector.getSink(anyLong()))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            configBean.refreshConfig(consumedMessage);
            fail();
        } catch (SinkException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void configRefreshesOnlyWhenVersionChanges() throws SinkException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        configBean.refreshConfig(newConsumedMessage(10, 1));
        final DpfSinkConfig config = configBean.getConfig();
        assertThat("1st refresh", config, is(sink.getContent().getSinkConfig()));

        configBean.refreshConfig(newConsumedMessage(10, 1));
        final DpfSinkConfig configUnchanged = configBean.getConfig();
        assertThat("no refresh", config, is(sameInstance(configUnchanged)));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(newSink(new DpfSinkConfig()
                .withUpdateServiceUserId("newUserId")
                .withUpdateServicePassword("newPassword")));

        configBean.refreshConfig(newConsumedMessage(10, 2));
        final DpfSinkConfig configChanged = configBean.getConfig();
        assertThat("2nd refresh", configChanged, is(not(sameInstance(config))));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    private ConfigBean newConfigBean() {
        final ConfigBean configBean = new ConfigBean();
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

    private Sink newSink(DpfSinkConfig config) {
        final SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.DPF)
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