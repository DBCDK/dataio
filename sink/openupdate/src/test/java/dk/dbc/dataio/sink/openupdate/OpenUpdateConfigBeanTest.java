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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
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

public class OpenUpdateConfigBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new OpenUpdateSinkConfig()
            .withUserId("userId")
            .withPassword("password")
            .withEndpoint("endpoint"));

    private OpenUpdateConfigBean openUpdateConfigBean;

    @Before
    public void setup() throws JSONBException {
        openUpdateConfigBean = newOpenUpdateConfigBean();
    }

    @Test
    public void getConfig_sinkNotFound_throws() throws FlowStoreServiceConnectorException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            openUpdateConfigBean.getConfig(consumedMessage);
            fail();
        } catch (SinkException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConfig() throws FlowStoreServiceConnectorException, SinkException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);

        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        // Subject under test
        final OpenUpdateSinkConfig config = openUpdateConfigBean.getConfig(consumedMessage);

        // Verification
        assertThat(config, is(notNullValue()));
        verify(flowStoreServiceConnector).getSink(sink.getId());
    }

    @Test
    public void getConfig_configRefreshedOnlyWhenVersionChanges() throws SinkException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        final OpenUpdateSinkConfig config = openUpdateConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("1st refresh", config, is(notNullValue()));

        final OpenUpdateSinkConfig configUnchanged = openUpdateConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("no refresh", config, is(configUnchanged));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(newSink(new OpenUpdateSinkConfig()
                .withEndpoint("newEndpoint")
                .withUserId("userId")
                .withPassword("password")));
        
        final OpenUpdateSinkConfig configChanged = openUpdateConfigBean.getConfig(newConsumedMessage(10, 2));
        assertThat("2nd refresh", configChanged, is(not(config)));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    @Test
    public void getConfig_validateOnlyFalseDisallowsIgnoredValidationErrors()
            throws FlowStoreServiceConnectorException, SinkException {

        final HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("some error");
        final Sink sink = newSink(new OpenUpdateSinkConfig()
                .withIgnoredValidationErrors(ignoredValidationErrors));
        final OpenUpdateConfigBean configBean = newOpenUpdateConfigBean();
        configBean.validateOnly = false;

        when(flowStoreServiceConnector.getSink(1L)).thenReturn(sink);

        final OpenUpdateSinkConfig config = configBean.getConfig(newConsumedMessage(1, 1));
        assertThat(config.getIgnoredValidationErrors(), is(nullValue()));
    }

    @Test
    public void getConfig_validateOnlyTrueAllowsIgnoredValidationErrors()
            throws FlowStoreServiceConnectorException, SinkException {

        final HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("some error");
        final Sink sink = newSink(new OpenUpdateSinkConfig()
                .withIgnoredValidationErrors(ignoredValidationErrors));
        final OpenUpdateConfigBean configBean = newOpenUpdateConfigBean();
        configBean.validateOnly = true;

        when(flowStoreServiceConnector.getSink(1L)).thenReturn(sink);
        
        final OpenUpdateSinkConfig config = configBean.getConfig(newConsumedMessage(1, 1));
        assertThat(config.getIgnoredValidationErrors(), is(ignoredValidationErrors));
    }

    private OpenUpdateConfigBean newOpenUpdateConfigBean() {
        final OpenUpdateConfigBean openUpdateConfigBean = new OpenUpdateConfigBean();
        openUpdateConfigBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        when(openUpdateConfigBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        return openUpdateConfigBean;
    }

    private ConsumedMessage newConsumedMessage(long sinkId, long sinkVersion) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink newSink(OpenUpdateSinkConfig config) {
        final SinkContent sinkContent = new SinkContentBuilder()
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
