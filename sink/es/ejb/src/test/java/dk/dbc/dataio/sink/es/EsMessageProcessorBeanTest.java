package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.testutil.MockedMessageDrivenContext;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
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
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
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
    private static final int RECORDS_CAPACITY = 1;
    private static final String ES_DATABASE_NAME = "dbname";
    private static final String PAYLOAD_TYPE = "ChunkResult";
    private final String chunkResultWithOneValidAddiRecord = generateChunkResultJsonWithResource("/1record.addi");
    private final EsConnectorBean esConnector = mock(EsConnectorBean.class);
    private final EsInFlightBean esInFlightAdmin = mock(EsInFlightBean.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducer = mock(JobProcessorMessageProducerBean.class);

    @Test
    public void onMessage_messageArgPayloadIsChunkResultWithJsonWithInvalidAddi_sinkChunkResultSentToQueue() throws JMSException, SinkException {
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, new ChunkResultJsonBuilder().build());
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
        verify(jobProcessorMessageProducer, times(1)).send(any(SinkChunkResult.class));
    }

    @Test
    public void onMessage_esThrottlerThrowsInterruptedException_transactionRollback() throws InterruptedException, JMSException {
        final EsThrottlerBean esThrottlerBean = mock(EsThrottlerBean.class);
        doThrow(new InterruptedException("TEST")).when(esThrottlerBean).acquireRecordSlots(anyInt());
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean(esThrottlerBean);
        esMessageProcessorBean.onMessage(getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord));
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(true));
    }

    @Test
    public void onMessage_esConnectorThrowsSinkException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new SinkException("TEST"));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esConnectorThrowsSystemException_transactionRollback() throws JMSException, SinkException, InterruptedException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new RuntimeException("TEST"));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esInFlightAdminThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenReturn(42);
        doThrow(new RuntimeException("TEST")).when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_processingSucceeds_esThrottlerIsUpdated() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenReturn(42);
        doNothing().when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = getMockedJmsTextMessage(PAYLOAD_TYPE, chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.getMessageDrivenContext().getRollbackOnly(), is(false));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY - 1));
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
        final String validAddi = "1\na\n1\nb\n";
        final ArrayList<ChunkItem> chunkItems = new ArrayList<>();
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(1)
                .setData(Base64Util.base64encode(validAddi))
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // ignored by processor
                .setId(2)
                .setStatus(ChunkItem.Status.IGNORE)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // failed by processor
                .setId(3)
                .setStatus(ChunkItem.Status.FAILURE)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processor produces invalid addi
                .setId(4)
                .setData("invalid")
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(5)
                .setData(Base64Util.base64encode(validAddi))
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        final ChunkResult chunkResult = new ChunkResultBuilder()
                .setItems(chunkItems)
                .build();

        final TestableMessageConsumerBean esMessageProcessorBean = getInitializedBean();
        final EsWorkload esWorkloadFromChunkResult = esMessageProcessorBean.getEsWorkloadFromChunkResult(chunkResult);

        assertThat(esWorkloadFromChunkResult, is(notNullValue()));
        assertThat(esWorkloadFromChunkResult.getAddiRecords().size(), is(2));
        assertThat(esWorkloadFromChunkResult.getSinkChunkResult().getItems().size(), is(5));
        assertThat("chunkItem 1 ID", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(0).getId(), is(1L));
        assertThat("chunkItem 1 status", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(0).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 2 ID", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(1).getId(), is(2L));
        assertThat("chunkItem 2 status", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(1).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem 3 ID", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(2).getId(), is(3L));
        assertThat("chunkItem 3 status", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem 4 ID", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(3).getId(), is(4L));
        assertThat("chunkItem 4 status", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(3).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem 5 ID", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(4).getId(), is(5L));
        assertThat("chunkItem 5 status", esWorkloadFromChunkResult.getSinkChunkResult().getItems().get(4).getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    private TestableMessageConsumerBean getInitializedBean() {
        return getInitializedBean(null);
    }

    private TestableMessageConsumerBean getInitializedBean(EsThrottlerBean esThrottlerBean) {
        final TestableMessageConsumerBean testableMessageConsumerBean = new TestableMessageConsumerBean();
        testableMessageConsumerBean.setMessageDrivenContext(new MockedMessageDrivenContext());
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esRecordsCapacity = RECORDS_CAPACITY;
        configuration.esDatabaseName = ES_DATABASE_NAME;
        testableMessageConsumerBean.configuration = configuration;
        if (esThrottlerBean == null) {
            testableMessageConsumerBean.esThrottler = new EsThrottlerBean();
            testableMessageConsumerBean.esThrottler.configuration = configuration;
            testableMessageConsumerBean.esThrottler.initialize();
        } else {
            testableMessageConsumerBean.esThrottler = esThrottlerBean;
        }
        testableMessageConsumerBean.esConnector = esConnector;
        testableMessageConsumerBean.esInFlightAdmin = esInFlightAdmin;
        testableMessageConsumerBean.jobProcessorMessageProducer = jobProcessorMessageProducer;
        return testableMessageConsumerBean;
    }

    private String generateChunkResultJsonWithResource(String resourceName) {
        try {
            final ChunkItem item = new ChunkItemBuilder()
                    .setData(Base64Util.base64encode(getResourceAsString(resourceName)))
                    .build();
            return JsonUtil.toJson(new ChunkResultBuilder()
                    .setItems(Arrays.asList(item))
                    .build());
        } catch (JsonException e) {
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
