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

package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldCatConfigBeanTest {
    private String PAYLOAD;
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private Sink sink;

    @Before
    public void setup() throws JSONBException {

        final SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.WORLDCAT)
                .setSinkConfig(new WorldCatSinkConfig()
                        .withUserId("user")
                        .withPassword("password")
                        .withProjectId("project")
                        .withEndpoint("endpoint")
                        .withRetryDiagnostics(Collections.singletonList("retryDiagnostic")))
                .build();

        sink    = new SinkBuilder().setContent(sinkContent).build();
        PAYLOAD = new JSONBContext().marshall(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    }

    @Test
    public void getConnector_sinkNotFound_throwsSinkException() throws FlowStoreServiceConnectorException {
        final WorldCatConfigBean configBean = getInitializedBean();
        final ConsumedMessage consumedMessage = getConsumedMessage(42, 1);
        final String message = "Error message from flowStore";

        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            // Subject under test
            configBean.getConnector(consumedMessage);

            // Verification
            fail();
        } catch (SinkException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConnector_connectorConfigured_ok() throws FlowStoreServiceConnectorException, SinkException {
        final WorldCatConfigBean configBean = getInitializedBean();
        final ConsumedMessage consumedMessage = getConsumedMessage(42, 1);

        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        // Subject under test
        final WciruServiceConnector connector = configBean.getConnector(consumedMessage);

        // Verification
        assertThat(connector, not(nullValue()));
        verify(flowStoreServiceConnector, times(1)).getSink(sink.getId());
    }

    @Test
    public void getConnector_connectorReConfiguredOnlyWhenVersionChanges_ok() throws SinkException, FlowStoreServiceConnectorException {
        final WorldCatConfigBean configBean = getInitializedBean();

        when(flowStoreServiceConnector.getSink(eq(10L))).thenReturn(sink);
        when(flowStoreServiceConnector.getSink(eq(12L))).thenReturn(sink);

        final WciruServiceConnector connector = configBean.getConnector(getConsumedMessage(10, 1));
        assertThat(connector, not(nullValue()));
        verify(flowStoreServiceConnector, times(1)).getSink(10L);

        final WciruServiceConnector connectorUnchanged = configBean.getConnector(getConsumedMessage(11, 1));
        assertThat(connector, is(connectorUnchanged));
        verify(flowStoreServiceConnector, times(0)).getSink(11L);

        final WciruServiceConnector connectorModified = configBean.getConnector(getConsumedMessage(12, 2));
        assertThat(connectorUnchanged, not(connectorModified));
        verify(flowStoreServiceConnector, times(1)).getSink(12L);
    }

     /*
     * Private methods
     */

    private WorldCatConfigBean getInitializedBean() {
        WorldCatConfigBean configBean = new WorldCatConfigBean();
        configBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        when(configBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        return configBean;
    }

    private ConsumedMessage getConsumedMessage(long sinkId, long sinkVersion) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, PAYLOAD);
    }
}