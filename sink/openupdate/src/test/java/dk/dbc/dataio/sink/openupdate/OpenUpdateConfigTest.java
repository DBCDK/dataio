package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenUpdateConfigTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new OpenUpdateSinkConfig()
            .withUserId("userId")
            .withPassword("password")
            .withEndpoint("endpoint"));

    private OpenUpdateConfig openUpdateConfig;

    @Before
    public void setup() {
        openUpdateConfig = newOpenUpdateConfigBean();
    }

    @Test
    public void getConfig_sinkNotFound_throws() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Unable to retrieve configuration from flowstore";
        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            openUpdateConfig.getConfig(consumedMessage);
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConfig() throws FlowStoreServiceConnectorException {
        ConsumedMessage consumedMessage = newConsumedMessage(42, 1);

        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        // Subject under test
        OpenUpdateSinkConfig config = openUpdateConfig.getConfig(consumedMessage);

        // Verification
        assertThat(config, is(notNullValue()));
        verify(flowStoreServiceConnector).getSink(sink.getId());
    }

    @Test
    public void getConfig_configRefreshedOnlyWhenVersionChanges() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        OpenUpdateSinkConfig config = openUpdateConfig.getConfig(newConsumedMessage(10, 1));
        assertThat("1st refresh", config, is(notNullValue()));

        OpenUpdateSinkConfig configUnchanged = openUpdateConfig.getConfig(newConsumedMessage(10, 1));
        assertThat("no refresh", config, is(configUnchanged));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(newSink(new OpenUpdateSinkConfig()
                .withEndpoint("newEndpoint")
                .withUserId("userId")
                .withPassword("password")));

        OpenUpdateSinkConfig configChanged = openUpdateConfig.getConfig(newConsumedMessage(10, 2));
        assertThat("2nd refresh", configChanged, is(not(config)));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    @Test
    public void getConfig_validateOnlyFalseDisallowsIgnoredValidationErrors()
            throws FlowStoreServiceConnectorException {

        HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("some error");
        Sink sink = newSink(new OpenUpdateSinkConfig()
                .withIgnoredValidationErrors(ignoredValidationErrors));
        OpenUpdateConfig configBean = newOpenUpdateConfigBean();
        configBean.validateOnly = false;

        when(flowStoreServiceConnector.getSink(1L)).thenReturn(sink);

        OpenUpdateSinkConfig config = configBean.getConfig(newConsumedMessage(1, 1));
        assertThat(config.getIgnoredValidationErrors(), is(nullValue()));
    }

    @Test
    public void getConfig_validateOnlyTrueAllowsIgnoredValidationErrors() throws FlowStoreServiceConnectorException {

        HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("some error");
        Sink sink = newSink(new OpenUpdateSinkConfig()
                .withIgnoredValidationErrors(ignoredValidationErrors));
        OpenUpdateConfig configBean = newOpenUpdateConfigBean();
        configBean.validateOnly = true;

        when(flowStoreServiceConnector.getSink(1L)).thenReturn(sink);

        OpenUpdateSinkConfig config = configBean.getConfig(newConsumedMessage(1, 1));
        assertThat(config.getIgnoredValidationErrors(), is(ignoredValidationErrors));
    }

    private OpenUpdateConfig newOpenUpdateConfigBean() {
        return new OpenUpdateConfig(flowStoreServiceConnector);
    }

    private ConsumedMessage newConsumedMessage(long sinkId, long sinkVersion) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink newSink(OpenUpdateSinkConfig config) {
        SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.OPENUPDATE)
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
