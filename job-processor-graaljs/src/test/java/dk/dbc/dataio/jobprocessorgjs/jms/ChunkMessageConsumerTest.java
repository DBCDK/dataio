package dk.dbc.dataio.jobprocessorgjs.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobprocessorgjs.service.ChunkProcessor;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChunkMessageConsumerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static TextMessage textMessage(String json, long flowId, long flowVersion)
            throws JMSException {
        TextMessage msg = mock(TextMessage.class);
        when(msg.getText()).thenReturn(json);
        when(msg.getObjectProperty(JMSHeader.flowId.name)).thenReturn(flowId);
        when(msg.getObjectProperty(JMSHeader.flowVersion.name)).thenReturn(flowVersion);
        when(msg.getObjectProperty(JMSHeader.additionalArgs.name)).thenReturn("{\"format\":\"iso\"}");
        return msg;
    }

    @Test
    void onMessage_partitionedChunk_processesAndForwardsToJobStore() throws Exception {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED).setJobId(1).setChunkId(2).build();
        Chunk processed = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(1).setChunkId(2).build();

        AtomicLong capturedFlowId = new AtomicLong();
        ChunkProcessor processor = mock(ChunkProcessor.class);
        when(processor.process(any(), anyLong(), anyLong(), anyString()))
                .thenAnswer(inv -> { capturedFlowId.set(inv.getArgument(1)); return processed; });

        JobStoreServiceConnector connector = mock(JobStoreServiceConnector.class);
        ChunkMessageConsumer consumer = new ChunkMessageConsumer(processor, connector);

        consumer.onMessage(textMessage(MAPPER.writeValueAsString(chunk), 42L, 3L));

        assertThat("flowId passed to processor", capturedFlowId.get(), is(42L));
    }

    @Test
    void onMessage_processedChunkType_throwsWithoutCallingProcessor() {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        ChunkProcessor processor = mock(ChunkProcessor.class);
        ChunkMessageConsumer consumer = new ChunkMessageConsumer(processor, mock(JobStoreServiceConnector.class));

        assertThrows(IllegalArgumentException.class,
                () -> consumer.onMessage(textMessage(MAPPER.writeValueAsString(chunk), 1L, 1L)));
    }

    @Test
    void onMessage_malformedJson_throwsIOException() throws JMSException {
        TextMessage msg = mock(TextMessage.class);
        when(msg.getText()).thenReturn("not-json");

        ChunkMessageConsumer consumer = new ChunkMessageConsumer(
                mock(ChunkProcessor.class), mock(JobStoreServiceConnector.class));

        assertThrows(IOException.class, () -> consumer.onMessage(msg));
    }
}
