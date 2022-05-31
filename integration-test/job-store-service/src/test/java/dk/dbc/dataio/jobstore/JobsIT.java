package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueServiceConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.List;

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
     * And : two chunks are sent to the processor queue
     * And : the job is partitioned
     * When : processor results are received for both chunks
     * Then : two chunks are sent to the sink queue
     * When : sink results are received for both chunks
     * Then : the job is completed
     */
    @Ignore
    @Test
    public void jobStates() throws JobStoreServiceConnectorException, JMSException, JSONBException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();

        // When...
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(jobInputStream);

        // Then...
        assertThat("job is created", jobInfoSnapshot, is(notNullValue()));
        assertThat("job is not complete", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // And...
        List<MockedJmsTextMessage> jmsMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueServiceConnector.Queue.PROCESSING, 2, 10000);

        MockedJmsTextMessage jmsMessage0 = jmsMessages.get(0);
        final Chunk processorChunk0 = jsonbContext.unmarshall(jmsMessage0.getText(), Chunk.class);
        assertThat("1st processor chunk ID", processorChunk0.getChunkId(),
                is(0L));
        assertThat("1st processor chunk belongs to job", processorChunk0.getJobId(),
                is((long) jobInfoSnapshot.getJobId()));
        assertThat("number of items in 1st processor chunk", processorChunk0.getItems().size(),
                is(10));
        MockedJmsTextMessage jmsMessage1 = jmsMessages.get(1);
        final Chunk processorChunk1 = jsonbContext.unmarshall(jmsMessage1.getText(), Chunk.class);
        assertThat("2nd processor chunk ID", processorChunk1.getChunkId(),
                is(1L));
        assertThat("2nd processor chunk belongs to job", processorChunk1.getJobId(),
                is((long) jobInfoSnapshot.getJobId()));
        assertThat("number of items in 2nd processor chunk", processorChunk1.getItems().size(),
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
        jobStoreServiceConnector.addChunk(newChunkOfType(processorChunk0, Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), processorChunk0.getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(processorChunk1, Chunk.Type.PROCESSED),
                jobInfoSnapshot.getJobId(), processorChunk1.getChunkId());

        // Then...
        jmsMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueServiceConnector.Queue.SINK, 2, 10000);

        jmsMessage0 = jmsMessages.get(0);
        final Chunk sinkChunk0 = jsonbContext.unmarshall(jmsMessage0.getText(), Chunk.class);
        assertThat("1st sink chunk ID", sinkChunk0.getChunkId(),
                is(0L));
        assertThat("1st sink chunk belongs to job", sinkChunk0.getJobId(),
                is((long) jobInfoSnapshot.getJobId()));
        assertThat("number of items in 1st sink chunk", sinkChunk0.getItems().size(),
                is(10));
        jmsMessage1 = jmsMessages.get(1);
        final Chunk sinkChunk1 = jsonbContext.unmarshall(jmsMessage1.getText(), Chunk.class);
        assertThat("2nd sink chunk ID", sinkChunk1.getChunkId(),
                is(1L));
        assertThat("2nd sink chunk belongs to job", sinkChunk1.getJobId(),
                is((long) jobInfoSnapshot.getJobId()));
        assertThat("number of items in 2nd sink chunk", sinkChunk1.getItems().size(),
                is(5));

        // When...
        jobStoreServiceConnector.addChunk(newChunkOfType(processorChunk0, Chunk.Type.DELIVERED),
                jobInfoSnapshot.getJobId(), sinkChunk0.getChunkId());
        jobStoreServiceConnector.addChunk(newChunkOfType(processorChunk1, Chunk.Type.DELIVERED),
                jobInfoSnapshot.getJobId(), sinkChunk1.getChunkId());

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
}
