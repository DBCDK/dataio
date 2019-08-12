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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
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
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenUpdateMessageProcessorBeanTest {
    /* mocks */
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final OpenUpdateConfigBean openUpdateConfigBean = mock(OpenUpdateConfigBean.class);
    private final AddiRecordPreprocessor addiRecordPreprocessor = Mockito.spy(new AddiRecordPreprocessor());

    private final OpenUpdateSinkConfig config = new OpenUpdateSinkConfig()
            .withEndpoint("testEndpoint")
            .withUserId("testUser")
            .withPassword("testPass");

    private final JSONBContext jsonbContext = new JSONBContext();
    private final String queueProvider = "queue";
    private final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
            .setQueueProvider(queueProvider)
            .build();
    private final FlowBinder flowBinder = new FlowBinderBuilder()
            .setId(42)
            .setVersion(10)
            .setContent(flowBinderContent)
            .build();
    private final OpenUpdateMessageProcessorBean openUpdateMessageProcessorBean = new OpenUpdateMessageProcessorBean();
    {
        openUpdateMessageProcessorBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        openUpdateMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        openUpdateMessageProcessorBean.openUpdateConfigBean = openUpdateConfigBean;
        openUpdateMessageProcessorBean.addiRecordPreprocessor = addiRecordPreprocessor;
    }

    @Before
    public void setupMocks() throws SinkException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId())).thenReturn(flowBinder);
        when(openUpdateConfigBean.getConfig(any(ConsumedMessage.class))).thenReturn(config);
    }

    @Test
    public void handleConsumedMessage_jobStoreCommunicationFails_throws() throws InvalidMessageException, SinkException, JobStoreServiceConnectorException {
        final JobStoreServiceConnectorException jobStoreServiceConnectorException = new JobStoreServiceConnectorException("Exception from job-store");
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong()))
                .thenThrow(jobStoreServiceConnectorException);

        try {
            openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
            fail("No SinkException thrown");
        } catch (SinkException e) {
            assertThat(e.getCause(), is(jobStoreServiceConnectorException));
        }
    }

    @Test
    public void handleConsumedMessage_flowStoreCommunicationFails_throws() throws InvalidMessageException, SinkException, FlowStoreServiceConnectorException {
        final FlowStoreServiceConnectorException flowStoreServiceConnectorException =
                new FlowStoreServiceConnectorException("Exception from flow-store");

        when(flowStoreServiceConnector.getFlowBinder(flowBinder.getId()))
                .thenThrow(flowStoreServiceConnectorException);

        try {
            openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
            fail("No SinkException thrown");
        } catch (SinkException e) {
            assertThat(e.getCause(), is(flowStoreServiceConnectorException));
        }
    }

    @Test
    public void handleConsumedMessage_cachesFlowBinder() throws InvalidMessageException, SinkException, FlowStoreServiceConnectorException {
        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
        assertThat(openUpdateMessageProcessorBean.cachedFlowBinders.get(flowBinder.getId()), is(notNullValue()));

        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));

        verify(flowStoreServiceConnector, times(1)).getFlowBinder(flowBinder.getId());
    }

    @Test
    public void handleConsumedMessage_callsAddiRecordPreprocessorWithQueueProvider() throws InvalidMessageException, SinkException {
        openUpdateMessageProcessorBean.handleConsumedMessage(
                getConsumedMessageForChunk(
                        new ChunkBuilder(Chunk.Type.PROCESSED)
                                .setItems(Collections.singletonList(
                                        new ChunkItemBuilder()
                                                .setData(getValidAddi("addi content"))
                                                .build()))
                                .build()));

        verify(addiRecordPreprocessor).preprocess(any(AddiRecord.class), eq(queueProvider));
    }

    @Test
    public void handleConsumedMessage_setsConfig() throws InvalidMessageException, SinkException {
        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat(openUpdateMessageProcessorBean.config, is(config));
    }

    @Test
    public void handleConsumedMessage_setsConnector() throws InvalidMessageException, SinkException {
        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        final OpenUpdateServiceConnector connector = openUpdateMessageProcessorBean.connector;
        assertThat("1st message creates", connector, is(notNullValue()));
        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat("2nd message retains", openUpdateMessageProcessorBean.connector, is(connector));
    }

    @Test
    public void handleConsumedMessage_updatesConnector() throws InvalidMessageException, SinkException {
        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        final OpenUpdateServiceConnector connector = openUpdateMessageProcessorBean.connector;
        assertThat("1st message creates", connector, is(notNullValue()));

        final OpenUpdateSinkConfig updatedConfig = new OpenUpdateSinkConfig()
                .withEndpoint("updatedEndpoint")
                .withUserId("updatedUser")
                .withPassword("updatedPass");
        when(openUpdateConfigBean.getConfig(any(ConsumedMessage.class))).thenReturn(updatedConfig);

        openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
        assertThat("2nd message updates", openUpdateMessageProcessorBean.connector, is(not(connector)));
    }

    private ConsumedMessage getConsumedMessageForChunk(Chunk chunk) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            headers.put(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME, flowBinder.getId());
            headers.put(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME, flowBinder.getVersion());

            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk getIgnoredChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build()))
                .build();
    }

    public static byte[] getValidAddi(String... content) {
        final StringBuilder addi = new StringBuilder();
        for (String s : content) {
            addi.append(String.format("19\n<es:referencedata/>\n%d\n%s\n",
                    s.getBytes(StandardCharsets.UTF_8).length, s));
        }
        return addi.toString().getBytes(StandardCharsets.UTF_8);
    }
}