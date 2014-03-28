package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.sink.testutil.MockedMessageDrivenContext;
import dk.dbc.dataio.sink.testutil.MockedTextMessage;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import javax.jms.JMSException;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

public class DummyMessageProcessorBeanTest {

    private TextMessageSenderBean mockTextMessageSender = mock(TextMessageSenderBean.class);

    @Before
    public void setup() {
         mock(TextMessageSenderBean.class);
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
}
