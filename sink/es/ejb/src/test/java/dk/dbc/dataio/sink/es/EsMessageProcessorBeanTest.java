package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.sink.InvalidMessageSinkException;
import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.junit.Test;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TimerService;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.transaction.UserTransaction;
import java.security.Identity;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

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
    public void validateMessage_messageArgPayloadTriggersDefaultConstructorWhenUnmarshalling_throws() throws InvalidMessageSinkException, JMSException {
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("{}");
        getInitializedBean().validateMessage(textMessage);
    }

    @Test
    public void onMessage_esConnectorThrowsSinkException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(ChunkResult.class))).thenThrow(new SinkException("TEST"));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esConnectorThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(ChunkResult.class))).thenThrow(new RuntimeException("TEST"));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_esInFlightAdminThrowsSystemException_transactionRollback() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(ChunkResult.class))).thenReturn(42);
        doThrow(new RuntimeException("TEST")).when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
        esMessageProcessorBean.onMessage(textMessage);
        assertThat(esMessageProcessorBean.messageDrivenContext.getRollbackOnly(), is(true));
        assertThat(esMessageProcessorBean.esThrottler.getAvailableSlots(), is(RECORDS_CAPACITY));
    }

    @Test
    public void onMessage_processingSucceeds_esThrottlerIsUpdated() throws JMSException, SinkException {
        when(esConnector.insertEsTaskPackage(any(ChunkResult.class))).thenReturn(42);
        doNothing().when(esInFlightAdmin).addEsInFlight(any(EsInFlight.class));
        final EsMessageProcessorBean esMessageProcessorBean = getInitializedBean();
        final MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(new ChunkResultJsonBuilder().build());
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

    private static class MockedTextMessage implements TextMessage {
        private String payload;
        @Override
        public void setText(String s) throws JMSException {
            payload = s;
        }

        @Override
        public String getText() throws JMSException {
            return payload;
        }

        @Override public String getJMSMessageID() throws JMSException { return "mockedMsg"; }
        @Override public void setJMSMessageID(String s) throws JMSException { }
        @Override public long getJMSTimestamp() throws JMSException { return 0; }
        @Override public void setJMSTimestamp(long l) throws JMSException { }
        @Override public byte[] getJMSCorrelationIDAsBytes() throws JMSException { return new byte[0]; }
        @Override public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException { }
        @Override public void setJMSCorrelationID(String s) throws JMSException { }
        @Override public String getJMSCorrelationID() throws JMSException { return null; }
        @Override public Destination getJMSReplyTo() throws JMSException { return null; }
        @Override public void setJMSReplyTo(Destination destination) throws JMSException { }
        @Override public Destination getJMSDestination() throws JMSException { return null; }
        @Override public void setJMSDestination(Destination destination) throws JMSException { }
        @Override public int getJMSDeliveryMode() throws JMSException { return 0; }
        @Override public void setJMSDeliveryMode(int i) throws JMSException { }
        @Override public boolean getJMSRedelivered() throws JMSException { return false; }
        @Override public void setJMSRedelivered(boolean b) throws JMSException { }
        @Override public String getJMSType() throws JMSException { return null; }
        @Override public void setJMSType(String s) throws JMSException { }
        @Override public long getJMSExpiration() throws JMSException { return 0; }
        @Override public void setJMSExpiration(long l) throws JMSException { }
        @Override public long getJMSDeliveryTime() throws JMSException { return 0; }
        @Override public void setJMSDeliveryTime(long l) throws JMSException { }
        @Override public int getJMSPriority() throws JMSException { return 0; }
        @Override public void setJMSPriority(int i) throws JMSException { }
        @Override public void clearProperties() throws JMSException { }
        @Override public boolean propertyExists(String s) throws JMSException { return false; }
        @Override public boolean getBooleanProperty(String s) throws JMSException { return false; }
        @Override public byte getByteProperty(String s) throws JMSException { return 0; }
        @Override public short getShortProperty(String s) throws JMSException { return 0; }
        @Override public int getIntProperty(String s) throws JMSException { return 0; }
        @Override public long getLongProperty(String s) throws JMSException { return 0; }
        @Override public float getFloatProperty(String s) throws JMSException { return 0; }
        @Override public double getDoubleProperty(String s) throws JMSException { return 0; }
        @Override public String getStringProperty(String s) throws JMSException { return null; }
        @Override public Object getObjectProperty(String s) throws JMSException { return null; }
        @Override public Enumeration getPropertyNames() throws JMSException { return null; }
        @Override public void setBooleanProperty(String s, boolean b) throws JMSException { }
        @Override public void setByteProperty(String s, byte b) throws JMSException { }
        @Override public void setShortProperty(String s, short i) throws JMSException { }
        @Override public void setIntProperty(String s, int i) throws JMSException { }
        @Override public void setLongProperty(String s, long l) throws JMSException { }
        @Override public void setFloatProperty(String s, float v) throws JMSException { }
        @Override public void setDoubleProperty(String s, double v) throws JMSException { }
        @Override public void setStringProperty(String s, String s2) throws JMSException { }
        @Override public void setObjectProperty(String s, Object o) throws JMSException { }
        @Override public void acknowledge() throws JMSException { }
        @Override public void clearBody() throws JMSException { }
        @Override public <T> T getBody(Class<T> tClass) throws JMSException { return null; }
        @Override public boolean isBodyAssignableTo(Class aClass) throws JMSException { return false; }
    }

    private static class NotTextMessage implements Message {
        @Override public String getJMSMessageID() throws JMSException { return null; }
        @Override public void setJMSMessageID(String s) throws JMSException { }
        @Override public long getJMSTimestamp() throws JMSException { return 0; }
        @Override public void setJMSTimestamp(long l) throws JMSException { }
        @Override public byte[] getJMSCorrelationIDAsBytes() throws JMSException { return new byte[0]; }
        @Override public void setJMSCorrelationIDAsBytes(byte[] bytes) throws JMSException { }
        @Override public void setJMSCorrelationID(String s) throws JMSException { }
        @Override public String getJMSCorrelationID() throws JMSException { return null; }
        @Override public Destination getJMSReplyTo() throws JMSException { return null; }
        @Override public void setJMSReplyTo(Destination destination) throws JMSException { }
        @Override public Destination getJMSDestination() throws JMSException { return null; }
        @Override public void setJMSDestination(Destination destination) throws JMSException { }
        @Override public int getJMSDeliveryMode() throws JMSException { return 0; }
        @Override public void setJMSDeliveryMode(int i) throws JMSException { }
        @Override public boolean getJMSRedelivered() throws JMSException { return false; }
        @Override public void setJMSRedelivered(boolean b) throws JMSException { }
        @Override public String getJMSType() throws JMSException { return null; }
        @Override public void setJMSType(String s) throws JMSException { }
        @Override public long getJMSExpiration() throws JMSException { return 0; }
        @Override public void setJMSExpiration(long l) throws JMSException { }
        @Override public long getJMSDeliveryTime() throws JMSException { return 0; }
        @Override public void setJMSDeliveryTime(long l) throws JMSException { }
        @Override public int getJMSPriority() throws JMSException { return 0; }
        @Override public void setJMSPriority(int i) throws JMSException { }
        @Override public void clearProperties() throws JMSException { }
        @Override public boolean propertyExists(String s) throws JMSException { return false; }
        @Override public boolean getBooleanProperty(String s) throws JMSException { return false; }
        @Override public byte getByteProperty(String s) throws JMSException { return 0; }
        @Override public short getShortProperty(String s) throws JMSException { return 0; }
        @Override public int getIntProperty(String s) throws JMSException { return 0; }
        @Override public long getLongProperty(String s) throws JMSException { return 0; }
        @Override public float getFloatProperty(String s) throws JMSException { return 0; }
        @Override public double getDoubleProperty(String s) throws JMSException { return 0; }
        @Override public String getStringProperty(String s) throws JMSException { return null; }
        @Override public Object getObjectProperty(String s) throws JMSException { return null; }
        @Override public Enumeration getPropertyNames() throws JMSException { return null; }
        @Override public void setBooleanProperty(String s, boolean b) throws JMSException { }
        @Override public void setByteProperty(String s, byte b) throws JMSException { }
        @Override public void setShortProperty(String s, short i) throws JMSException { }
        @Override public void setIntProperty(String s, int i) throws JMSException { }
        @Override public void setLongProperty(String s, long l) throws JMSException { }
        @Override public void setFloatProperty(String s, float v) throws JMSException { }
        @Override public void setDoubleProperty(String s, double v) throws JMSException { }
        @Override public void setStringProperty(String s, String s2) throws JMSException { }
        @Override public void setObjectProperty(String s, Object o) throws JMSException { }
        @Override public void acknowledge() throws JMSException { }
        @Override public void clearBody() throws JMSException { }
        @Override public <T> T getBody(Class<T> tClass) throws JMSException { return null; }
        @Override public boolean isBodyAssignableTo(Class aClass) throws JMSException { return false; }
    }

    @SuppressWarnings("deprecation")
    private static class MockedMessageDrivenContext implements MessageDrivenContext {
        private boolean rollbackOnly = false;
        @Override public EJBHome getEJBHome() throws IllegalStateException { return null; }
        @Override public EJBLocalHome getEJBLocalHome() throws IllegalStateException { return null; }
        @Override public Properties getEnvironment() { return null; }
        @Override public Identity getCallerIdentity() { return null; }
        @Override public Principal getCallerPrincipal() throws IllegalStateException { return null; }
        @Override public boolean isCallerInRole(Identity identity) { return false; }
        @Override public boolean isCallerInRole(String s) throws IllegalStateException { return false; }
        @Override public UserTransaction getUserTransaction() throws IllegalStateException { return null; }
        @Override public void setRollbackOnly() throws IllegalStateException { rollbackOnly = true; }
        @Override public boolean getRollbackOnly() throws IllegalStateException { return rollbackOnly; }
        @Override public TimerService getTimerService() throws IllegalStateException { return null; }
        @Override public Object lookup(String s) throws IllegalArgumentException { return null; }
        @Override public Map<String, Object> getContextData() { return null; }
    }
}
