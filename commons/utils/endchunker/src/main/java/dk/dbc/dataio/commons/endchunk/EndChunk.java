package dk.dbc.dataio.commons.endchunk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.List;

import static java.lang.String.format;

public class EndChunk {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) throws JMSException {
        new EndChunk().go();
    }

    public void go() throws JMSException {
        try (ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("tcp://dataio-be-p03:61616");
            JMSContext context = factory.createContext()) {
            JMSProducer producer = context.createProducer();
            Queue queue = context.createQueue("jmsDataioSinks::jmsDataioSinks");
//            Chunk chunk = createEndChunk(9357, 0);
            Chunk chunk = createEndChunk(25463204, 3);
//            Chunk chunk = createEndChunk(25437048, 123892);
            Message message = createMessage(context, chunk, "marcconv", 8451, 2);
            producer.send(queue, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message createMessage(JMSContext context, Chunk chunk, String resource, long sinkId, long sinkVersion) throws JsonProcessingException, JMSException {
        Message message = context.createTextMessage(mapper.writeValueAsString(chunk));
        JMSHeader.payload.addHeader(message, JMSHeader.CHUNK_PAYLOAD_TYPE);
        if(resource != null && !resource.isEmpty()) JMSHeader.resource.addHeader(message, resource);
        JMSHeader.sinkId.addHeader(message, sinkId);
        JMSHeader.sinkVersion.addHeader(message, sinkVersion);
        JMSHeader.jobId.addHeader(message, chunk.getJobId());
        JMSHeader.chunkId.addHeader(message, chunk.getChunkId());
        return message;
    }

    private Chunk createEndChunk(int jobId, long chunkId) {
        Chunk chunk = new Chunk(jobId, chunkId, Chunk.Type.PROCESSED);
        ChunkItem chunkItem = new ChunkItem()
                .withId(0)
                .withStatus(ChunkItem.Status.SUCCESS)
                .withType(ChunkItem.Type.JOB_END)
                .withData("Job termination item")
                .withTrackingId(format("%d.JOB_END", jobId));
        chunk.addAllItems(List.of(chunkItem));
        return chunk;
    }
}
