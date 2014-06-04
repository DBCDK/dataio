package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.test.json.ChunkResultJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkChunkResultBuilder;
import dk.dbc.dataio.sink.fbs.types.FbsSinkException;
import org.junit.Test;

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
    public void handleConsumedMessage_consumedMessageArgContainsInvalidPayload_throws() throws InvalidMessageException, FbsSinkException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, "payload");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void handleConsumedMessage_pusherThrowsFbsSinkException_throws() throws InvalidMessageException, FbsSinkException {
        final FbsSinkException fbsSinkException = new FbsSinkException("DIED");
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        when(fbsPusherBean.push(any(ChunkResult.class))).thenReturn(sinkChunkResult);
        doThrow(fbsSinkException).when(jobProcessorMessageProducerBean).send(sinkChunkResult);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        try {
            getInitializedBean().handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (FbsSinkException e) {
            assertThat(e, is(fbsSinkException));
        }
    }

    @Test
    public void handleConsumedMessage_pusherReturnsResult_sendsResult() throws InvalidMessageException, FbsSinkException {
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder().build();
        when(fbsPusherBean.push(any(ChunkResult.class))).thenReturn(sinkChunkResult);
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