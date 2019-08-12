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

package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import dk.dbc.dataio.sink.testutil.MockedMessageDrivenContext;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EsMessageProcessorBean unit tests.
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsMessageProcessorBeanTest {
    private static final String PAYLOAD_TYPE = JmsConstants.CHUNK_PAYLOAD_TYPE;
    private JSONBContext jsonbContext = new JSONBContext();
    private final String chunkResultWithOneValidAddiRecord = generateChunkResultJsonWithResource("/1record.addi");
    private EsConnectorBean esConnector;
    private EsInFlightBean esInFlightAdmin;
    private AddiRecordPreprocessor addiRecordPreprocessor;
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    private FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private final static int SINK_ID = 333;
    private EsSinkConfig sinkConfig = new EsSinkConfig().withUserId(42).withDatabaseName("dbname");


    @Before
    public void setupMocks() {
        esConnector = mock(EsConnectorBean.class);
        esInFlightAdmin = mock(EsInFlightBean.class);
        addiRecordPreprocessor = mock(AddiRecordPreprocessor.class);

        jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

        flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);

        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void onMessage_messageArgPayloadIsChunkResultWithJsonWithInvalidAddi_deliveredChunkAdded() throws Exception {
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final String processedChunkJson = jsonbContext.marshall(processedChunk);
        final SinkContent sinkContent = new SinkContentBuilder().setSinkConfig(sinkConfig).build();
        final Sink sink = new SinkBuilder().setId(SINK_ID).setContent(sinkContent).build();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, processedChunkJson, sink.getVersion());

        when(flowStoreServiceConnector.getSink(SINK_ID)).thenReturn(sink);

        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
        verify(jobStoreServiceConnector, times(1)).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void onMessage_configureVersionSpecificConfigValueCalled_highestVersionSet() throws Exception {
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();

        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final String processedChunkJson = jsonbContext.marshall(processedChunk);

        final SinkContent sinkContent = new SinkContentBuilder().setSinkConfig(sinkConfig).build();
        final Sink version1 = new SinkBuilder().setId(SINK_ID).setVersion(1).setContent(sinkContent).build();
        final Sink version2 = new SinkBuilder().setId(SINK_ID).setVersion(2).setContent(sinkContent).build();

        final MockedJmsTextMessage textMessage1 = getMockedJmsTextMessage(PAYLOAD_TYPE, processedChunkJson, version1.getVersion());
        final MockedJmsTextMessage textMessage2 = getMockedJmsTextMessage(PAYLOAD_TYPE, processedChunkJson, version2.getVersion());

        when(flowStoreServiceConnector.getSink(SINK_ID)).thenReturn(version1, version2);

        // send textMessage containing sink version 1
        esMessageProcessorBean.onMessage(textMessage1);
        verify(flowStoreServiceConnector, times(1)).getSink(SINK_ID); // called onMessage
        assertThat("highestVersionSeen updated", esMessageProcessorBean.highestVersionSeen, is(version1.getVersion())); // highestVersionSeen updated to newest

        // resend textMessage containing sink version 1
        esMessageProcessorBean.onMessage(textMessage1);
        verify(flowStoreServiceConnector, times(1)).getSink(SINK_ID); // not called onMessage
        assertThat("highestVersionSeen not updated", esMessageProcessorBean.highestVersionSeen, is(version1.getVersion())); // highestVersionSeen not updated

        // send textMessage containing updated sink version
        esMessageProcessorBean.onMessage(textMessage2);
        verify(flowStoreServiceConnector, times(2)).getSink(SINK_ID); // called onMessage
        assertThat("highestVersionSeen updated", esMessageProcessorBean.highestVersionSeen, is(version2.getVersion())); // highestVersionSeen updated to newest
    }

    @Test
    public void onMessage_esConnectorThrowsSinkException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class), any(EsSinkConfig.class))).thenThrow(new SinkException("TEST"));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        try {
            esMessageProcessorBean.onMessage(textMessage);
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_esConnectorThrowsSystemException_transactionRollback() throws JMSException, SinkException, InterruptedException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class), any(EsSinkConfig.class))).thenThrow(new RuntimeException("TEST"));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        try {
            esMessageProcessorBean.onMessage(textMessage);
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void onMessage_esInFlightAdminThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class), any(EsSinkConfig.class))).thenReturn(42);
        doThrow(new RuntimeException("TEST")).when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        try {
            esMessageProcessorBean.onMessage(textMessage);
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    /*
     * Private methods
     */

    private TestableMessageConsumerBean getInitializedBean() {
        final TestableMessageConsumerBean testableMessageConsumerBean = new TestableMessageConsumerBean();
        testableMessageConsumerBean.setMessageDrivenContext(new MockedMessageDrivenContext());
        testableMessageConsumerBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        testableMessageConsumerBean.esConnector = esConnector;
        testableMessageConsumerBean.esInFlightAdmin = esInFlightAdmin;

        testableMessageConsumerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        testableMessageConsumerBean.flowStoreServiceConnector = flowStoreServiceConnector;
        testableMessageConsumerBean.sinkId = SINK_ID;
        testableMessageConsumerBean.addiRecordPreprocessor = addiRecordPreprocessor;
        return testableMessageConsumerBean;
    }

    private String generateChunkResultJsonWithResource(String resourceName) {
        try {
            final ChunkItem item = new ChunkItemBuilder()
                    .setData(StringUtil.asBytes(getResourceAsString(resourceName)))
                    .build();
            return jsonbContext.marshall(new ChunkBuilder(Chunk.Type.PROCESSED)
                    .setItems(Collections.singletonList(item))
                    .build());
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getResourceAsString(String resourceName) {
        final URL resource = EsMessageProcessorBeanTest.class.getResource(resourceName);
        try {
            return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private MockedJmsTextMessage getMockedJmsTextMessage(String payloadType, String payload) throws JMSException {
        return getMockedJmsTextMessage(payloadType, payload, 1);
    }

    private MockedJmsTextMessage getMockedJmsTextMessage(String payloadType, String payload, long sinkVersion) throws JMSException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, payloadType);
        textMessage.setLongProperty(JmsConstants.SINK_VERSION_PROPERTY_NAME, sinkVersion);
        textMessage.setText(payload);
        return textMessage;
    }

    private static class TestableMessageConsumerBean extends EsMessageProcessorBean {
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
        public MessageDrivenContext getMessageDrivenContext() {
            return this.messageDrivenContext;
        }
    }
}
