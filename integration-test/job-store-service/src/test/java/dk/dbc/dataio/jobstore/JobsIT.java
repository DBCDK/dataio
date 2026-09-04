package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueTester;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.Watermark;
import jakarta.jms.JMSException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobsIT extends AbstractJobStoreServiceContainerTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Given: a valid job request with a datafile containing 15 items
     * When : submitted to job-store
     * Then : a job is created
     * And : the job is partitioned
     * And : two chunks are sent to the processor queue
     * When : processor results are received for both chunks
     * Then : one message per item is sent to the sink queue
     * When : sink results are received for both chunks
     * Then : the job is completed
     */
    @Test
    public void jobStates() throws JobStoreServiceConnectorException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();

        // When...
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(jobInputStream);

        // Then...
        assertThat("job is created", jobInfoSnapshot, is(notNullValue()));
        assertThat("job is not complete", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // And...
        // (Since we cannot be certain of sequence of partitioning...)
        List<Chunk> chunks = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueTester.Queue.PROCESSING_BUSINESS, 2, 20000)
                .stream().map(this::getChunk)
                .sorted(Comparator.comparing(chunk1 -> chunk1 != null ? chunk1.getChunkId() : 0))
                .collect(Collectors.toList());

        assertThat("1st processor chunk belongs to job", chunks.get(0).getJobId(),
                is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 1st processor chunk", chunks.get(0).getItems().size(),
                is(10));
        assertThat("2nd processor chunk ID", chunks.get(1).getChunkId(),
                is(1L));
        assertThat("2nd processor chunk belongs to job", chunks.get(1).getJobId(),
                is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 2nd processor chunk", chunks.get(1).getItems().size(),
                is(5));

        // And...
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).getFirst();
        assertThat("job is partitioned", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PARTITIONING),
                is(true));
        assertThat("job number of chunks", jobInfoSnapshot.getNumberOfChunks(),
                is(2));
        assertThat("job number of items", jobInfoSnapshot.getNumberOfItems(),
                is(15));

        // When...
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(0), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(1), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        // Then...
        // (And now taking chunk sequence very serious!)
        // Delivery is per item, so the sink queue holds one message per item of the job,
        // not one per chunk. Regroup them into the chunks they came from.
        final List<MockedJmsTextMessage> itemMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueTester.Queue.SINK_BE_CISTERNE, 15, 20000);
        assertItemMessageProtocol(itemMessages, jobInfoSnapshot.getJobId());

        chunks = groupItemMessagesIntoChunks(itemMessages, Chunk.Type.DELIVERED);

        assertThat("number of sink chunks", chunks.size(),
                is(2));
        assertThat("1st sink chunk ID", chunks.get(0).getChunkId(),
                is(0L));
        assertThat("1st sink chunk belongs to job", chunks.get(0).getJobId(),
                is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 1st sink chunk", chunks.get(0).getItems().size(),
                is(10));
        assertThat("2nd sink chunk ID", chunks.get(1).getChunkId(),
                is(1L));
        assertThat("2nd sink chunk belongs to job", chunks.get(1).getJobId(),
                is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 2nd sink chunk", chunks.get(1).getItems().size(),
                is(5));

        // When...
        jobStoreServiceConnector.addChunk(chunks.get(0),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(chunks.get(1),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        // Then...
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).getFirst();
        assertThat("all job phases are done", jobInfoSnapshot.getState().allPhasesAreDone(), is(true));
        assertThat("job is complete", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: a job whose chunks have been partitioned and processed, ready for delivery
     * When : the sink reports each item's delivery result individually (the per-item
     *        watermark protocol, docs/chunk-scheduling-redesign.md)
     * Then : no watermark exists for a record before its item is reported delivered
     * And  : the watermark reflects the exact (jobId, chunkId, itemId) once reported
     * And  : the job completes exactly as it would via the bulk delivery path
     */
    @Test
    public void perItemDeliveryProtocol_watermarkAdvancesAndJobCompletes() throws JobStoreServiceConnectorException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(jobInputStream);

        List<Chunk> chunks = jmsQueueServiceConnector.awaitQueueSizeAndList(
                        JmsQueueTester.Queue.PROCESSING_BUSINESS, 2, 20000)
                .stream().map(this::getChunk)
                .sorted(Comparator.comparing(chunk1 -> chunk1 != null ? chunk1.getChunkId() : 0))
                .collect(Collectors.toList());

        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(0), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(1), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        final List<MockedJmsTextMessage> itemMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueTester.Queue.SINK_BE_CISTERNE, 15, 20000);
        assertItemMessageProtocol(itemMessages, jobInfoSnapshot.getJobId());

        chunks = groupItemMessagesIntoChunks(itemMessages, Chunk.Type.PROCESSED);

        final int sinkId = 1;
        for (int i = 0; i < chunks.size(); i++) {
            final Chunk chunk = chunks.get(i);
            final int chunkId = (int) chunk.getChunkId();
            for (ChunkItem item : chunk.getItems()) {
                // item.getId() restarts at 0 in every chunk, so recordKey needs both
                // jobId and chunkId to stay unique across this job's own chunks.
                final String recordKey = "870970:" + jobInfoSnapshot.getJobId() + "-" + chunkId + "-" + item.getId();
                final short itemId = (short) item.getId();

                assertThat("no watermark before delivery for " + recordKey,
                        jobStoreServiceConnector.getWatermark(sinkId, recordKey), is(Optional.empty()));

                jobStoreServiceConnector.addItemDelivered(
                        new ItemDeliveryResult(sinkId, recordKey, ItemDeliveryResult.Status.DELIVERED, item),
                        chunk.getJobId(), chunkId, itemId);

                assertThat("watermark reflects delivery for " + recordKey,
                        jobStoreServiceConnector.getWatermark(sinkId, recordKey),
                        is(Optional.of(new Watermark(chunk.getJobId(), chunkId, itemId))));
            }

            if (i < chunks.size() - 1) {
                jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).getFirst();
                assertThat("job not complete after only chunk " + chunkId + " is delivered",
                        jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
            }
        }

        // Then... the job completes exactly as it would via the bulk delivery path
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).getFirst();
        assertThat("all job phases are done", jobInfoSnapshot.getState().allPhasesAreDone(), is(true));
        assertThat("job is complete", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: a valid job request backed by a GraalJS flow
     * When : submitted to job-store
     * Then : a job is created and partitioned
     * And : two chunks are sent to the GraalJS processor queue (not the Nashorn queue)
     * When : processor results are received for both chunks
     * Then : two chunks are sent to the sink queue
     * When : sink results are received for both chunks
     * Then : the job is completed
     */
    @Test
    public void jobWithGraaljsFlow_chunksRoutedToGraaljsQueue() throws JobStoreServiceConnectorException {
        // Given...
        final JobInputStream jobInputStream = newGraaljsJobInputStream();

        // When...
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(jobInputStream);

        // Then...
        assertThat("job is created", jobInfoSnapshot, is(notNullValue()));
        assertThat("job is not complete", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // And... chunks must land on the GraalJS queue, not the Nashorn queue
        List<Chunk> chunks = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueTester.Queue.PROCESSING_GRAALJS, 2, 20000)
                .stream().map(this::getChunk)
                .sorted(Comparator.comparing(chunk1 -> chunk1 != null ? chunk1.getChunkId() : 0))
                .collect(Collectors.toList());

        assertThat("Nashorn queue is empty", jmsQueueServiceConnector.getQueueSize(JmsQueueTester.Queue.PROCESSING_BUSINESS), is(0));
        assertThat("1st processor chunk belongs to job", chunks.get(0).getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 1st processor chunk", chunks.get(0).getItems().size(), is(10));
        assertThat("2nd processor chunk ID", chunks.get(1).getChunkId(), is(1L));
        assertThat("2nd processor chunk belongs to job", chunks.get(1).getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("number of items in 2nd processor chunk", chunks.get(1).getItems().size(), is(5));

        // When...
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(0), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(1), Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        // Then... one message per item on the sink queue, regrouped into their chunks
        final List<MockedJmsTextMessage> itemMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueTester.Queue.SINK_BE_CISTERNE, 15, 20000);
        assertItemMessageProtocol(itemMessages, jobInfoSnapshot.getJobId());

        chunks = groupItemMessagesIntoChunks(itemMessages, Chunk.Type.DELIVERED);

        assertThat("number of sink chunks", chunks.size(), is(2));
        assertThat("1st sink chunk ID", chunks.get(0).getChunkId(), is(0L));
        assertThat("1st sink chunk belongs to job", chunks.get(0).getJobId(), is(jobInfoSnapshot.getJobId()));
        assertThat("2nd sink chunk ID", chunks.get(1).getChunkId(), is(1L));
        assertThat("2nd sink chunk belongs to job", chunks.get(1).getJobId(), is(jobInfoSnapshot.getJobId()));

        // When...
        jobStoreServiceConnector.addChunk(chunks.get(0),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(chunks.get(1),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        // Then...
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).getFirst();
        assertThat("all job phases are done", jobInfoSnapshot.getState().allPhasesAreDone(), is(true));
        assertThat("job is complete", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
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

    private JobInputStream newGraaljsJobInputStream() {
        return new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("graaljs-test")
                .withCharset("utf8")
                .withDestination("broend-cisterne")
                .withSubmitterId(870970), true, 0);
    }

    private Chunk newChunkOfType(Chunk chunk, Chunk.Type type) {
        final Chunk chunkWithType = new Chunk(chunk.getJobId(), chunk.getChunkId(), type);
        chunkWithType.addAllItems(chunk.getItems());
        return chunkWithType;
    }

    private Chunk getChunk(MockedJmsTextMessage message) {
        try {
            return jsonbContext.unmarshall(message.getText(), Chunk.class);
        } catch (JMSException | JSONBException e) {
            return null;
        }
    }

    private ChunkItem getChunkItem(MockedJmsTextMessage message) {
        try {
            return jsonbContext.unmarshall(message.getText(), ChunkItem.class);
        } catch (JMSException | JSONBException e) {
            return null;
        }
    }

    /**
     * Asserts what every sink bound item message must carry, so that a header lost in
     * job-store shows up here rather than as a message a sink silently discards.
     */
    private void assertItemMessageProtocol(List<MockedJmsTextMessage> itemMessages, int jobId) {
        for (MockedJmsTextMessage message : itemMessages) {
            assertThat("payload type", header(message, JMSHeader.payload),
                    is(JMSHeader.ITEM_PAYLOAD_TYPE));
            assertThat("jobId header", header(message, JMSHeader.jobId),
                    is(Integer.toString(jobId)));
            assertThat("chunkId header", header(message, JMSHeader.chunkId),
                    is(notNullValue()));
            assertThat("itemId header", header(message, JMSHeader.itemId),
                    is(notNullValue()));
            assertThat("trackingId header", header(message, JMSHeader.trackingId),
                    is(jobId + "/" + header(message, JMSHeader.chunkId)));
            assertThat("sinkId header", header(message, JMSHeader.sinkId),
                    is(notNullValue()));
            assertThat("sinkVersion header", header(message, JMSHeader.sinkVersion),
                    is(notNullValue()));

            final String recordKey = header(message, JMSHeader.recordKey);
            if (recordKey != null) {
                assertThat("recordKey is qualified by the job's submitter number",
                        recordKey, startsWith("870970:"));
            }
        }
    }

    /**
     * Regroups per-item messages into the chunks they were dispatched from, in ascending
     * chunk and item ID order, so a test can still drive the bulk chunk endpoints.
     */
    private List<Chunk> groupItemMessagesIntoChunks(List<MockedJmsTextMessage> itemMessages, Chunk.Type type) {
        final Map<Long, List<MockedJmsTextMessage>> messagesByChunkId = new TreeMap<>();
        for (MockedJmsTextMessage message : itemMessages) {
            final long chunkId = Long.parseLong(header(message, JMSHeader.chunkId));
            messagesByChunkId.computeIfAbsent(chunkId, key -> new ArrayList<>()).add(message);
        }

        final List<Chunk> chunks = new ArrayList<>();
        for (Map.Entry<Long, List<MockedJmsTextMessage>> entry : messagesByChunkId.entrySet()) {
            final List<MockedJmsTextMessage> messages = entry.getValue();
            messages.sort(Comparator.comparingInt(
                    message -> Integer.parseInt(header(message, JMSHeader.itemId))));
            final int jobId = Integer.parseInt(header(messages.get(0), JMSHeader.jobId));
            final Chunk chunk = new Chunk(jobId, entry.getKey(), type);
            chunk.addAllItems(messages.stream().map(this::getChunkItem).collect(Collectors.toList()));
            chunks.add(chunk);
        }
        return chunks;
    }

    private String header(MockedJmsTextMessage message, JMSHeader jmsHeader) {
        try {
            return message.getStringProperty(jmsHeader.name);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to read header " + jmsHeader.name, e);
        }
    }
}
