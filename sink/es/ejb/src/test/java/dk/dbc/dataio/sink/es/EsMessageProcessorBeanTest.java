package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.sink.InvalidMessageSinkException;
import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.testutil.MockedTextMessage;
import dk.dbc.dataio.sink.testutil.MockedMessageDrivenContext;
import dk.dbc.dataio.sink.testutil.NotTextMessage;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
    private final String chunkResultWithOneValidAddiRecord = generateChunkResultJsonWithResource("/1record.addi");
    private final EsConnectorBean esConnector = mock(EsConnectorBean.class);
    private final EsInFlightBean esInFlightAdmin = mock(EsInFlightBean.class);

    @Test
    public void onMessage_messageArgIsNull_noTransactionRollback() {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        esMessageProcessorBean.onMessage(null);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgIsNull_throws() throws InvalidMessageSinkException {
        getInitializedBean().validateMessage(null);
    }

    @Test
    public void onMessage_messageArgIsNotOfTypeTextMessage_noTransactionRollback() {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        esMessageProcessorBean.onMessage(new NotTextMessage());
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgIsNotOfTypeTextMessage_throws() throws InvalidMessageSinkException {
        getInitializedBean().validateMessage(new NotTextMessage());
    }

    @Test
    public void onMessage_messageArgIsPayloadIsNull_noTransactionRollback() throws JMSException {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(null);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsNull_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(null);
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsEmpty_noTransactionRollback() throws JMSException {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("");
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsEmpty_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidChunkResultJson_noTransactionRollback() throws JMSException {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{'jobId': 42}");
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsInvalidChunkResultJson_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{'jobId': 42}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadTriggersDefaultConstructorWhenUnmarshalling_noTransactionRollback() throws JMSException {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{}");
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadIsChunkResultWithJsonWithInvalidAddi_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_messageArgPayloadIsChunkResultWithJsonWithInvalidAddi_noTransactionRollback() throws JMSException {
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
    }

    @Test(expected = InvalidMessageSinkException.class)
    public void validateMessage_messageArgPayloadTriggersDefaultConstructorWhenUnmarshalling_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_esConnectorThrowsSinkException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new SinkException("TEST"));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esConnectorThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenThrow(new RuntimeException("TEST"));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esInFlightAdminThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenReturn(42);
        doThrow(new RuntimeException("TEST")).when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_processingSucceeds_esThrottlerIsUpdated() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(EsWorkload.class))).thenReturn(42);
        doNothing().when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(chunkResultWithOneValidAddiRecord);
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(false));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY - 1));
    }

    private EsMessageProcessorBean getInitializedBean() {
        final EsMessageProcessorBean esMessageProcessorBean = new EsMessageProcessorBean();
        esMessageProcessorBean.messageDrivenContext = new MockedMessageDrivenContext();
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esRecordsCapacity = RECORDS_CAPACITY;
        configuration.esDatabaseName = ES_DATABASE_NAME;
        esMessageProcessorBean.configuration = configuration;
        esMessageProcessorBean.esThrottler = new EsThrottlerBean();
        esMessageProcessorBean.esThrottler.configuration = configuration;
        esMessageProcessorBean.esThrottler.initialize();
        esMessageProcessorBean.esConnector = esConnector;
        esMessageProcessorBean.esInFlightAdmin = esInFlightAdmin;
        return esMessageProcessorBean;
    }

    private String generateChunkResultJsonWithResource(String resourceName) {
        try {
            final ChunkItem item = new ChunkItemBuilder()
                    .setData(encodeBase64(getResourceAsString(resourceName)))
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

    private String encodeBase64(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(StandardCharsets.UTF_8));
    }
}
