package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueTester;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Drives the per-item delivery protocol through the real sink framework
 * ({@link SinkMessageConsumerAdapter}) against the containerized job-store, so that the
 * three halves of the protocol - job-store dispatching item messages, the framework
 * checking and reporting, and the job-store endpoints being called - are exercised
 * together rather than one at a time against mocks.
 * <p>
 * Each test asserts against watermarks written by its own jobs, never against their
 * absence, since the job-store container is shared with the other tests of this class.
 */
public class SinkDeliveryProtocolIT extends AbstractJobStoreServiceContainerTest {
    private static final int ITEMS_PER_JOB = 15;
    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Given: a job whose chunks are processed and dispatched as one message per item
     * When : a sink built on the framework consumes every item message
     * Then : each item is delivered once and its watermark names the delivered version
     * And  : the job completes
     */
    @Test
    public void everyItemIsDeliveredAndWatermarked() throws Exception {
        JobInfoSnapshot job = processedJob();
        TestSink sink = new TestSink();

        List<Message> itemMessages = consumeItemMessages(ITEMS_PER_JOB);
        deliver(sink, itemMessages);

        assertThat("every item was delivered", sink.delivered.size(), is(ITEMS_PER_JOB));

        int watermarked = 0;
        for (Message message : itemMessages) {
            String recordKey = stringHeader(message, JMSHeader.recordKey);
            if (recordKey == null) {
                continue;
            }
            watermarked++;
            assertThat("watermark for " + recordKey,
                    jobStoreServiceConnector.getWatermark(sinkId(message), recordKey),
                    is(Optional.of(watermarkOf(message))));
        }
        assertThat("items carrying a record key", watermarked, is(ITEMS_PER_JOB));

        assertThat("job is complete", completionTimeOf(job), is(notNullValue()));
    }

    /**
     * Given: two jobs holding the same records, delivered oldest first
     * When : the older job's item messages are redelivered after the newer job's
     * Then : the sink is never asked to deliver them, since the watermark supersedes them
     * And  : the watermark still names the newer version
     * <p>
     * The two jobs are delivered one after the other rather than concurrently because
     * dependency tracking, still in place until the graph is removed, blocks the second
     * job's chunks until the first job's are delivered - they hold the same records.
     */
    @Test
    public void supersededItemsAreSkippedWithoutBeingDelivered() throws Exception {
        JobInfoSnapshot olderJob = processedJob();
        TestSink sink = new TestSink();
        List<Message> olderItemMessages = consumeItemMessages(ITEMS_PER_JOB);
        deliver(sink, olderItemMessages);
        assertThat("older job is complete", completionTimeOf(olderJob), is(notNullValue()));

        processedJob();
        List<Message> newerItemMessages = consumeItemMessages(ITEMS_PER_JOB);
        deliver(sink, newerItemMessages);

        // When... the older job's items are redelivered behind the newer job's
        sink.delivered.clear();
        deliver(sink, olderItemMessages);

        assertThat("no superseded item was delivered", sink.delivered.size(), is(0));
        for (Message message : newerItemMessages) {
            String recordKey = stringHeader(message, JMSHeader.recordKey);
            assertThat("watermark still names the newer version for " + recordKey,
                    jobStoreServiceConnector.getWatermark(sinkId(message), recordKey),
                    is(Optional.of(watermarkOf(message))));
        }
    }

    /**
     * Given: an item that has already been delivered and reported
     * When : the very same item message is redelivered, as stale recovery does
     * Then : it is delivered again rather than skipped, since it is not an older version
     * And  : the watermark is unchanged
     */
    @Test
    public void exactRetransmitIsDelivered() throws Exception {
        processedJob();
        TestSink sink = new TestSink();

        List<Message> itemMessages = consumeItemMessages(ITEMS_PER_JOB);
        deliver(sink, itemMessages);

        Message retransmitted = itemMessages.getFirst();
        Watermark before = jobStoreServiceConnector
                .getWatermark(sinkId(retransmitted), stringHeader(retransmitted, JMSHeader.recordKey)).orElse(null);
        sink.delivered.clear();

        deliver(sink, List.of(retransmitted));

        assertThat("item was delivered again", sink.delivered.size(), is(1));
        assertThat("watermark is unchanged",
                jobStoreServiceConnector.getWatermark(sinkId(retransmitted),
                        stringHeader(retransmitted, JMSHeader.recordKey)),
                is(Optional.of(before)));
    }

    /**
     * Given: a sink opting out of watermark filtering
     * When : it consumes every item message of a job
     * Then : every item is delivered and no watermark of theirs is advanced
     * And  : the job completes, since results are still reported per item
     */
    @Test
    public void watermarkOptOutDeliversEveryItemWithoutAdvancingWatermarks() throws Exception {
        JobInfoSnapshot job = processedJob();
        TestSink sink = new TestSink() {
            @Override
            protected boolean usesDeliveryWatermark() {
                return false;
            }
        };

        List<Message> itemMessages = consumeItemMessages(ITEMS_PER_JOB);
        Map<String, Optional<Watermark>> before = watermarksOf(itemMessages);

        deliver(sink, itemMessages);

        assertThat("every item was delivered", sink.delivered.size(), is(ITEMS_PER_JOB));
        assertThat("no watermark was touched", watermarksOf(itemMessages), is(before));
        assertThat("job is complete", completionTimeOf(job), is(notNullValue()));
    }

    private void deliver(SinkMessageConsumerAdapter sink, List<Message> messages)
            throws InvalidMessageException {
        for (Message message : messages) {
            ConsumedMessage consumedMessage = sink.validateMessage(message);
            sink.handleConsumedMessage(consumedMessage);
        }
    }

    /**
     * Submits a job and hands its processed chunks back, leaving job-store to dispatch one
     * message per item to the sink queue
     */
    private JobInfoSnapshot processedJob() throws JobStoreServiceConnectorException {
        JobInfoSnapshot job = jobStoreServiceConnector.addJob(newJobInputStream());
        List<Chunk> chunks = jmsQueueServiceConnector.awaitQueueSizeAndList(
                        JmsQueueTester.Queue.PROCESSING_BUSINESS, 2, 20000)
                .stream().map(this::getChunk)
                .sorted(Comparator.comparing(Chunk::getChunkId))
                .toList();
        for (Chunk chunk : chunks) {
            Chunk processed = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.PROCESSED);
            processed.addAllItems(chunk.getItems());
            jobStoreServiceConnector.addChunk(processed, job.getJobId(), chunk.getChunkId());
        }
        jmsQueueServiceConnector.emptyQueue(JmsQueueTester.Queue.PROCESSING_BUSINESS);
        return job;
    }

    /**
     * Receives the given number of messages off the sink queue as the JMS messages they
     * are, rather than through the browsing the other tests of this module use, since the
     * framework reads typed properties off the message it is handed
     */
    private List<Message> consumeItemMessages(int expectedNumberOfMessages) {
        jmsQueueServiceConnector.awaitQueueSize(
                JmsQueueTester.Queue.SINK_BE_CISTERNE, expectedNumberOfMessages, 30000);
        List<Message> messages = new ArrayList<>();
        try (JMSContext context = new ActiveMQConnectionFactory("tcp://" + artemisHostPort).createContext();
             JMSConsumer consumer = context.createConsumer(
                     context.createQueue(JmsQueueTester.Queue.SINK_BE_CISTERNE.getQueueName()))) {
            Message message;
            while ((message = consumer.receive(1000)) != null) {
                messages.add(message);
            }
        }
        assertThat("number of item messages consumed", messages.size(), is(expectedNumberOfMessages));
        return messages;
    }

    private Map<String, Optional<Watermark>> watermarksOf(List<Message> messages)
            throws JobStoreServiceConnectorException {
        Map<String, Optional<Watermark>> watermarks = new LinkedHashMap<>();
        for (Message message : messages) {
            String recordKey = stringHeader(message, JMSHeader.recordKey);
            if (recordKey != null) {
                watermarks.put(recordKey, jobStoreServiceConnector.getWatermark(sinkId(message), recordKey));
            }
        }
        return watermarks;
    }

    private Object completionTimeOf(JobInfoSnapshot job) throws JobStoreServiceConnectorException {
        return jobStoreServiceConnector.listJobs("job:id = " + job.getJobId()).getFirst().getTimeOfCompletion();
    }

    private Watermark watermarkOf(Message message) {
        return new Watermark(jobIdOf(message),
                header(message, JMSHeader.chunkId, Long.class).intValue(),
                header(message, JMSHeader.itemId, Short.class));
    }

    private int jobIdOf(Message message) {
        return header(message, JMSHeader.jobId, Integer.class);
    }

    private int sinkId(Message message) {
        return header(message, JMSHeader.sinkId, Long.class).intValue();
    }

    private String stringHeader(Message message, JMSHeader jmsHeader) {
        return header(message, jmsHeader, String.class);
    }

    private <T> T header(Message message, JMSHeader jmsHeader, Class<T> type) {
        try {
            return jmsHeader.getHeader(message, type);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to read header " + jmsHeader.name, e);
        }
    }

    private Chunk getChunk(MockedJmsTextMessage message) {
        try {
            return jsonbContext.unmarshall(message.getText(), Chunk.class);
        } catch (JMSException | JSONBException e) {
            throw new IllegalStateException("Unable to read chunk", e);
        }
    }

    private JobInputStream newJobInputStream() {
        return new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination("broend-cisterne")
                .withSubmitterId(870970), true, 0);
    }

    /**
     * Records what it was asked to deliver, and reports every item as delivered
     */
    private static class TestSink extends SinkMessageConsumerAdapter {
        private final List<ChunkItem> delivered = new ArrayList<>();

        TestSink() {
            super(new ServiceHub.Builder()
                    .withJobStoreServiceConnector(AbstractJobStoreServiceContainerTest.jobStoreServiceConnector)
                    .test());
        }

        @Override
        protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
            delivered.add(item);
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED, new ChunkItem()
                    .withId(item.getId())
                    .withStatus(ChunkItem.Status.SUCCESS)
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(item.getTrackingId())
                    .withData("delivered by " + SinkDeliveryProtocolIT.class.getSimpleName()));
        }

        @Override
        public String getQueue() {
            return JmsQueueTester.Queue.SINK_BE_CISTERNE.getQueueName();
        }

        @Override
        public String getAddress() {
            return "sink";
        }
    }
}
