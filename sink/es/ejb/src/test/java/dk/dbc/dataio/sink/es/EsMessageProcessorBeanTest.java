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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
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
    private static final String ES_DATABASE_NAME = "dbname";
    private static final String PAYLOAD_TYPE = JmsConstants.CHUNK_PAYLOAD_TYPE;
    private static final String PROCESSING_TAG = "dataio:sink-processing";
    private JSONBContext jsonbContext = new JSONBContext();
    private final String chunkResultWithOneValidAddiRecord = generateChunkResultJsonWithResource("/1record.addi");
    private EsConnectorBean esConnector;
    private EsInFlightBean esInFlightAdmin;
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    private JobStoreServiceConnector jobStoreServiceConnector;


    @Before
    public void setupMocks() {
        esConnector = mock(EsConnectorBean.class);
        esInFlightAdmin = mock(EsInFlightBean.class);
        jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void onMessage_messageArgPayloadIsChunkResultWithJsonWithInvalidAddi_deliveredChunkAdded() throws JMSException, SinkException, JSONBException, JobStoreServiceConnectorException {
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
        final String processedChunkJson = jsonbContext.marshall(processedChunk);
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, processedChunkJson);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
        verify(jobStoreServiceConnector, times(1)).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }

    @Test
    public void onMessage_esConnectorThrowsSinkException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new SinkException("TEST"));
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
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new RuntimeException("TEST"));
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
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenReturn(42);
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

    @Test
    public void getEsWorkloadFromChunkResult_chunkResultArgIsNull_throws() throws SinkException {
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        try {
            esMessageProcessorBean.getEsWorkloadFromChunkResult(null);
            fail("No exception thrown");
        } catch(NullPointerException e) {
        }
    }

    @Test
    public void getEsWorkloadFromChunkResult_chunkResultArgIsNonEmpty_returnsEsWorkload() throws SinkException {
        final byte[] validAddiProcessingFalse = AddiRecordPreprocessorTest.getValidAddiWithProcessingFalse();
        final byte[] validAddiProcessingTrue = AddiRecordPreprocessorTest.getValidAddiWithProcessingTrueAndValidMarcXContentData();
        final byte[] validAddiWithoutProcessing = AddiRecordPreprocessorTest.getValidAddiWithoutProcessing();
        final byte[] validAddiProcessingTrueInvalidMarcX = AddiRecordPreprocessorTest.getValidAddiWithProcessingTrueAndInvalidMarcXContentData();

        final ArrayList<ChunkItem> chunkItems = new ArrayList<>();
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(0)
                .setData(validAddiWithoutProcessing)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // ignored by processor
                .setId(1)
                .setStatus(ChunkItem.Status.IGNORE)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // failed by processor
                .setId(2)
                .setStatus(ChunkItem.Status.FAILURE)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processor produces invalid addi
                .setId(3)
                .setData(StringUtil.asBytes("invalid"))
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(4)
                .setData(validAddiWithoutProcessing)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processor produces empty addi
                .setId(5)
                .setData(StringUtil.asBytes(""))
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // sink processing removed from meta data and processed successfully
                .setId(6)
                .setData(validAddiProcessingFalse)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               //sink processing removed from meta data, content data converted to iso2709 and processed successfully
                .setId(7)
                .setData(validAddiProcessingTrue)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()
                .setId(8)
                .setData(validAddiProcessingTrueInvalidMarcX)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());

        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final EsWorkload esWorkloadFromChunkResult = esMessageProcessorBean.getEsWorkloadFromChunkResult(processedChunk);

        assertThat(esWorkloadFromChunkResult, is(notNullValue()));
        assertThat(esWorkloadFromChunkResult.getAddiRecords().size(), is(4));
        assertThat(esWorkloadFromChunkResult.getDeliveredChunk().size(), is(9));
        Iterator<ChunkItem> iterator = esWorkloadFromChunkResult.getDeliveredChunk().iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat("chunkItem 0 ID", item0.getId(), is(0L));
        assertThat("chunkItem 0 status", item0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 0 data", asString(item0.getData()), is("1"));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat("chunkItem 1 ID", item1.getId(), is(1L));
        assertThat("chunkItem 1 status", item1.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat("chunkItem 2 ID", item2.getId(), is(2L));
        assertThat("chunkItem 2 status", item2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item3 = iterator.next();
        assertThat("chunkItem 3 ID", item3.getId(), is(3L));
        assertThat("chunkItem 3 status", item3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item4 = iterator.next();
        assertThat("chunkItem 4 ID", item4.getId(), is(4L));
        assertThat("chunkItem 4 status", item4.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 4 data", asString(item0.getData()), is("1"));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item5 = iterator.next();
        assertThat("chunkItem 5 ID", item5.getId(), is(5L));
        assertThat("chunkItem 5 status", item5.getStatus(), is(ChunkItem.Status.FAILURE));
        ChunkItem item6 = iterator.next();
        assertThat("chunkItem 6 ID", item6.getId(), is(6L));
        assertThat("chunkItem 6 status", item6.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 6 data", asString(item0.getData()), is("1"));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item7 = iterator.next();
        assertThat("chunkItem 7 ID", item7.getId(), is(7L));
        assertThat("chunkItem 7 status", item7.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 7 data", asString(item0.getData()), is("1"));
        ChunkItem item8 = iterator.next();
        assertThat("chunkItem 8 ID", item8.getId(), is(8L));
        assertThat("chunkItem 8 status", item8.getStatus(), is(ChunkItem.Status.FAILURE));

        // assert that the processing tag has been removed from the meta data for the 2 items
        // that successfully have been processed.
        AddiRecord addiRecord3 = esWorkloadFromChunkResult.getAddiRecords().get(2);
        byte[] metadata = addiRecord3.getMetaData();
        assertThat(new String(metadata, StandardCharsets.UTF_8).contains(PROCESSING_TAG), is(false));

        AddiRecord addiRecord4 = esWorkloadFromChunkResult.getAddiRecords().get(3);
        byte[] metadata2 = addiRecord4.getMetaData();
        assertThat(new String(metadata2, StandardCharsets.UTF_8).contains(PROCESSING_TAG), is(false));
    }

    private TestableMessageConsumerBean getInitializedBean() {
        final TestableMessageConsumerBean testableMessageConsumerBean = new TestableMessageConsumerBean();
        testableMessageConsumerBean.setMessageDrivenContext(new MockedMessageDrivenContext());
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esDatabaseName = ES_DATABASE_NAME;
        testableMessageConsumerBean.configuration = configuration;
        testableMessageConsumerBean.esConnector = esConnector;
        testableMessageConsumerBean.esInFlightAdmin = esInFlightAdmin;

        testableMessageConsumerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        testableMessageConsumerBean.setup();
        return testableMessageConsumerBean;
    }

    private String generateChunkResultJsonWithResource(String resourceName) {
        try {
            final ChunkItem item = new ChunkItemBuilder()
                    .setData(StringUtil.asBytes(getResourceAsString(resourceName)))
                    .build();
            return jsonbContext.marshall(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
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
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, payloadType);
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
