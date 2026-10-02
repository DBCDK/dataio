package dk.dbc.dataio.commons.types.jms;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JMSHeaderTest {
    private static final String RECORD_KEY = "870970:42";
    private static final short ITEM_ID = 3;

    private final Map<String, Object> properties = new HashMap<>();
    private final Message message = propertyStoringMessage();

    @Test
    void wireNamesMatchConstantNames() {
        assertThat(JMSHeader.itemId.name, is("itemId"));
        assertThat(JMSHeader.recordKey.name, is("recordKey"));
    }

    @Test
    void itemIdIsWrittenAsShortProperty() throws JMSException {
        JMSHeader.itemId.addHeader(message, ITEM_ID);

        verify(message).setShortProperty(JMSHeader.itemId.name, ITEM_ID);
        assertThat(JMSHeader.itemId.getHeader(message, Short.class), is(ITEM_ID));
    }

    @Test
    void recordKeyIsReadBackUnparsed() throws JMSException {
        JMSHeader.recordKey.addHeader(message, RECORD_KEY);

        assertThat(JMSHeader.recordKey.getHeader(message, String.class), is(RECORD_KEY));
    }

    @Test
    void absentHeadersOnMessageAreReadAsNull() throws JMSException {
        assertThat(JMSHeader.itemId.getHeader(message, Short.class), is(nullValue()));
        assertThat(JMSHeader.recordKey.getHeader(message, String.class), is(nullValue()));
    }

    @Test
    void absentHeadersOnConsumedMessageAreReadAsNull() {
        ConsumedMessage consumedMessage = new ConsumedMessage("messageId", new HashMap<>(), "payload");

        assertThat(JMSHeader.itemId.getHeader(consumedMessage, Short.class), is(nullValue()));
        assertThat(JMSHeader.recordKey.getHeader(consumedMessage, String.class), is(nullValue()));
    }

    @Test
    void headersOnConsumedMessageKeepTheirType() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        headers.put(JMSHeader.recordKey.name, RECORD_KEY);
        ConsumedMessage consumedMessage = new ConsumedMessage("messageId", headers, "payload");

        assertThat(JMSHeader.itemId.getHeader(consumedMessage, Short.class), is(ITEM_ID));
        assertThat(JMSHeader.recordKey.getHeader(consumedMessage, String.class), is(RECORD_KEY));
    }

    private Message propertyStoringMessage() {
        Message mockedMessage = mock(Message.class);
        try {
            doAnswer(invocation -> {
                properties.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(mockedMessage).setShortProperty(anyString(), anyShort());
            doAnswer(invocation -> {
                properties.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(mockedMessage).setStringProperty(anyString(), anyString());
            when(mockedMessage.getObjectProperty(anyString()))
                    .thenAnswer(invocation -> properties.get(invocation.getArgument(0)));
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
        return mockedMessage;
    }
}
