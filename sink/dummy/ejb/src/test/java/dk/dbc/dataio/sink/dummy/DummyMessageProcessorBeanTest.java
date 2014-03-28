package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TimerService;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.transaction.UserTransaction;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

public class DummyMessageProcessorBeanTest {

    private TextMessageSender mockTextMessageSender = mock(TextMessageSender.class);

    @Before
    public void setup() {
         mock(TextMessageSender.class);
    }

    Logger LOGGER = LoggerFactory.getLogger(DummyMessageProcessorBeanTest.class);

    @Test
    public void onMessage_nullMessage_noNewMessageOnQueue() {
        DummyMessageProcessorBean dummy = new DummyMessageProcessorBean();
        dummy.textMessageSender = mockTextMessageSender;
        dummy.messageDrivenContext = new MockedMessageDrivenContext();

        dummy.onMessage(null);

        assertThat(dummy.messageDrivenContext.getRollbackOnly(), is(false));
        verify(mockTextMessageSender, times(0)).send(any(String.class), any(List.class));
    }

    @Test
    public void onMessage_nullTextInMessage_noNewMessageOnQueue() throws JMSException {
        DummyMessageProcessorBean dummy = new DummyMessageProcessorBean();
        dummy.textMessageSender = mockTextMessageSender;
        dummy.messageDrivenContext = new MockedMessageDrivenContext();
        MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText(null);

        dummy.onMessage(textMessage);

        assertThat(dummy.messageDrivenContext.getRollbackOnly(), is(false));
        verify(mockTextMessageSender, times(0)).send(any(String.class), any(List.class));
    }

    @Test
    public void onMessage_emptyTextInMessage_noNewMessageOnQueue() throws JMSException {
        DummyMessageProcessorBean dummy = new DummyMessageProcessorBean();
        dummy.textMessageSender = mockTextMessageSender;
        dummy.messageDrivenContext = new MockedMessageDrivenContext();
        MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("");

        dummy.onMessage(textMessage);

        assertThat(dummy.messageDrivenContext.getRollbackOnly(), is(false));
        verify(mockTextMessageSender, times(0)).send(any(String.class), any(List.class));
    }

    @Test
    public void onMessage_invalidJsonInMessage_noNewMessageOnQueue() throws JMSException {
        DummyMessageProcessorBean dummy = new DummyMessageProcessorBean();
        dummy.textMessageSender = mockTextMessageSender;
        dummy.messageDrivenContext = new MockedMessageDrivenContext();
        MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText("This is not valid Json!");

        dummy.onMessage(textMessage);

        assertThat(dummy.messageDrivenContext.getRollbackOnly(), is(false));
        verify(mockTextMessageSender, times(0)).send(any(String.class), any(List.class));
    }

    @Test
    public void onMessage_validJsonChunkInMessage_newMessageOnQueue() throws JMSException, JsonException {
        List<ChunkItem> items = Arrays.asList(new ChunkItem(0, "some data", ChunkItem.Status.SUCCESS));
        ChunkResult chunk = new ChunkResult(0L, 0L, Charset.forName("UTF-8"), items);
        DummyMessageProcessorBean dummy = new DummyMessageProcessorBean();
        dummy.textMessageSender = mockTextMessageSender;
        dummy.messageDrivenContext = new MockedMessageDrivenContext();
        MockedTextMessage textMessage = new MockedTextMessage();
        textMessage.setText((JsonUtil.toJson(chunk)));

        dummy.onMessage(textMessage);

        assertThat(dummy.messageDrivenContext.getRollbackOnly(), is(false));
        verify(mockTextMessageSender, times(1)).send(any(String.class), any(List.class));
    }

    // todo: Use the mocks from es-sink - collect in some sane place like 'commons'
    @SuppressWarnings("deprecation")
    private static class MockedMessageDrivenContext implements MessageDrivenContext {

        private boolean rollbackOnly = false;

        @Override
        public EJBHome getEJBHome() throws IllegalStateException {
            return null;
        }

        @Override
        public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
            return null;
        }

        @Override
        public Properties getEnvironment() {
            return null;
        }

        @Override
        public java.security.Identity getCallerIdentity() {
            return null;
        }

        @Override
        public Principal getCallerPrincipal() throws IllegalStateException {
            return null;
        }

        @Override
        public boolean isCallerInRole(java.security.Identity identity) {
            return false;
        }

        @Override
        public boolean isCallerInRole(String s) throws IllegalStateException {
            return false;
        }

        @Override
        public UserTransaction getUserTransaction() throws IllegalStateException {
            return null;
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException {
            rollbackOnly = true;
        }

        @Override
        public boolean getRollbackOnly() throws IllegalStateException {
            return rollbackOnly;
        }

        @Override
        public TimerService getTimerService() throws IllegalStateException {
            return null;
        }

        @Override
        public Object lookup(String s) throws IllegalArgumentException {
            return null;
        }

        @Override
        public Map<String, Object> getContextData() {
            return null;
        }
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

}
