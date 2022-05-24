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

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
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
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldCatConfigBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

    private final String payload = newPayload(new ChunkBuilder(Chunk.Type.PROCESSED).build());
    private final Sink sink = newSink(new WorldCatSinkConfig()
            .withUserId("userId")
            .withPassword("password")
            .withEndpoint("endpoint"));

    private WorldCatConfigBean worldCatConfigBean;

    @Before
    public void setup() throws JSONBException {
        worldCatConfigBean = newWorldCatConfigBean();
    }

    @Test
    public void getConfig_sinkNotFound_throws() throws FlowStoreServiceConnectorException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        final String message = "Error message from flowStore";
        when(flowStoreServiceConnector.getSink(anyLong())).thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException(message, 404));

        try {
            worldCatConfigBean.getConfig(consumedMessage);
            fail();
        } catch (SinkException e) {
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void getConfig() throws FlowStoreServiceConnectorException, SinkException {
        final ConsumedMessage consumedMessage = newConsumedMessage(42, 1);
        when(flowStoreServiceConnector.getSink(anyLong())).thenReturn(sink);

        final WorldCatSinkConfig config = worldCatConfigBean.getConfig(consumedMessage);
        assertThat(config, is(sink.getContent().getSinkConfig()));
        verify(flowStoreServiceConnector).getSink(sink.getId());
    }

    @Test
    public void getConfig_configRefreshesOnlyWhenVersionChanges() throws SinkException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.getSink(10L)).thenReturn(sink);

        final WorldCatSinkConfig config = worldCatConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("1st refresh", config, is(sink.getContent().getSinkConfig()));

        final WorldCatSinkConfig configUnchanged = worldCatConfigBean.getConfig(newConsumedMessage(10, 1));
        assertThat("no refresh", config, is(sameInstance(configUnchanged)));

        when(flowStoreServiceConnector.getSink(10L)).thenReturn(newSink(new WorldCatSinkConfig()
                .withEndpoint("newEndpoint")
                .withUserId("userId")
                .withPassword("password")));

        final WorldCatSinkConfig configChanged = worldCatConfigBean.getConfig(newConsumedMessage(10, 2));
        assertThat("2nd refresh", configChanged, is(not(sameInstance(config))));

        verify(flowStoreServiceConnector, times(2)).getSink(10L);
    }

    private WorldCatConfigBean newWorldCatConfigBean() {
        final WorldCatConfigBean worldCatConfigBean = new WorldCatConfigBean();
        worldCatConfigBean.flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        when(worldCatConfigBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        return worldCatConfigBean;
    }

    private ConsumedMessage newConsumedMessage(long sinkId, long sinkVersion) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsConstants.SINK_ID_PROPERTY_NAME, sinkId);
        headers.put(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        return new ConsumedMessage("messageId", headers, payload);
    }

    private Sink newSink(WorldCatSinkConfig config) {
        final SinkContent sinkContent = new SinkContentBuilder()
                .setSinkType(SinkContent.SinkType.WORLDCAT)
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
