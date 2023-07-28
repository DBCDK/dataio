package dk.dbc.dataio.dlq.errorhandler;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.JMSException;
import java.util.Collections;
import java.util.Map;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.destination;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.redelivery;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.rejected;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class DmqMessageConsumerBeanTest {
    private Map<String, Object> headers;
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final DLQMessageConsumer dlqMessageConsumer = new DLQMessageConsumer(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build());

    public DmqMessageConsumerBeanTest() {

    }

    @Before
    public void setup() {
        headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        PrometheusMetricRegistry.create().resetAll();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalid_noTransactionRollback() throws JMSException {
        MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        JMSHeader.payload.addHeader(textMessage, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.jobId.addHeader(textMessage, 0);
        textMessage.setText("{'invalid': 'instance'}");
        dlqMessageConsumer.onMessage(textMessage);
        long rej = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false"), rejected.is("true")).getCount();
        Assert.assertEquals("Message should be rejected", 1, rej);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalid_throws() throws JobStoreException, InvalidMessageException {
        ConsumedMessage consumedMessage = new ConsumedMessage("id", headers, "{'invalid': 'instance'}");
        dlqMessageConsumer.handleConsumedMessage(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsUnknown_throws() throws JobStoreException, InvalidMessageException {
        ConsumedMessage consumedMessage = new ConsumedMessage("id", Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, "Unknown"), "{'unknown': 'instance'}");
        dlqMessageConsumer.handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_deadPartitionedChunk_singleChunkAdded() throws JMSException, JSONBException, JobStoreServiceConnectorException {
        Chunk originalChunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        JMSHeader.payload.addHeader(textMessage, JMSHeader.CHUNK_PAYLOAD_TYPE);
        JMSHeader.jobId.addHeader(textMessage, 0);
        textMessage.setText(dlqMessageConsumer.jsonbContext.marshall(originalChunk));
        dlqMessageConsumer.onMessage(textMessage);
        long rec = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false")).getCount();
        long rej = Metric.dataio_message_count.counter(destination.is(dlqMessageConsumer.getFQN()), redelivery.is("false"), rejected.is("true")).getCount();
        Mockito.verify(jobStoreServiceConnector, times(1)).addChunk(any(Chunk.class), eq(originalChunk.getJobId()), eq(originalChunk.getChunkId()));
        Assert.assertEquals("Message should be successfully consumed", 1, rec);
        Assert.assertEquals("Message should be accepted", 0, rej);
    }
}
