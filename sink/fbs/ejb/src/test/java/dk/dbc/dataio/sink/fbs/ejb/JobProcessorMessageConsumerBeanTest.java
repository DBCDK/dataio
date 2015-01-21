package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import org.junit.Test;

import javax.xml.ws.WebServiceException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobProcessorMessageConsumerBeanTest {
    private static final String MESSAGE_ID = "id";
    private static final String PAYLOAD_TYPE = "ChunkResult";
    private static final String PAYLOAD = new ChunkResultJsonBuilder().build();

    private final FbsPusherBean fbsPusherBean = mock(FbsPusherBean.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = mock(JobProcessorMessageProducerBean.class);

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_consumedMessageArgContainsInvalidPayload_throws() throws InvalidMessageException, SinkException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, "payload");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void handleConsumedMessage_pusherThrowsSinkException_throws() throws InvalidMessageException, SinkException {
        final SinkException sinkException = new SinkException("DIED");
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenReturn(sinkChunkResult);
        doThrow(sinkException).when(jobProcessorMessageProducerBean).send(sinkChunkResult);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        try {
            getInitializedBean().handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (SinkException e) {
            assertThat(e, is(sinkException));
        }
    }

    @Test
    public void handleConsumedMessage_pusherThrowsWebServiceException_throws() throws InvalidMessageException, SinkException {
        final WebServiceException webServiceException = new WebServiceException("died");
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenThrow(webServiceException);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        try {
            getInitializedBean().handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (WebServiceException e) {
            assertThat(e, is(webServiceException));
        }
    }

    @Test
    public void handleConsumedMessage_pusherReturnsResult_sendsResult() throws InvalidMessageException, SinkException {
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenReturn(sinkChunkResult);
        doNothing().when(jobProcessorMessageProducerBean).send(sinkChunkResult);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    private JobProcessorMessageConsumerBean getInitializedBean() {
        final JobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = new JobProcessorMessageConsumerBean();
        jobProcessorMessageConsumerBean.fbsPusher = fbsPusherBean;
        jobProcessorMessageConsumerBean.jobProcessorMessageProducer = jobProcessorMessageProducerBean;
        return jobProcessorMessageConsumerBean;
    }
}