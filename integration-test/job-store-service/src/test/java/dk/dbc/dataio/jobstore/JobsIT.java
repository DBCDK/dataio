package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueTester;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.jms.JMSException;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
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
     * Then : two chunks are sent to the sink queue
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
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).get(0);
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
        chunks = jmsQueueServiceConnector.awaitQueueSizeAndList(
                        JmsQueueTester.Queue.SINK_BE_CISTERNE, 2, 20000)
                .stream().map(this::getChunk)
                .collect(Collectors.toList());

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
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(0), Chunk.Type.DELIVERED),
                jobInfoSnapshot.getJobId(), chunks.get(0).getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(chunks.get(1), Chunk.Type.DELIVERED),
                jobInfoSnapshot.getJobId(), chunks.get(1).getChunkId());

        // Then...
        jobInfoSnapshot = jobStoreServiceConnector.listJobs("job:id = " + jobInfoSnapshot.getJobId()).get(0);
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
}
