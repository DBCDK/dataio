package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueServiceConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.junit.Test;

import javax.jms.JMSException;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class EmptyJobsIT extends AbstractJobStoreServiceContainerTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    /**
     * Given: an "empty job" request with illegal type
     * When : submitted to job-store
     * Then : request is rejected with a BAD_REQUEST code
     */
    @Test
    public void invalidType() throws JobStoreServiceConnectorException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();
        jobInputStream.getJobSpecification().withType(JobSpecification.Type.PERSISTENT);
        try {
            // When...
            jobStoreServiceConnector.addEmptyJob(jobInputStream);
            fail("Illegal type for empty job was not rejected");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Then...
            assertThat("job-store service response status", e.getStatusCode(),
                    is(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    /**
     * Given: an "empty job" request with illegal datafile
     * When : submitted to job-store
     * Then : request is rejected with a BAD_REQUEST code
     */
    @Test
    public void invalidDatafile() throws JobStoreServiceConnectorException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();
        jobInputStream.getJobSpecification().withDataFile("file");
        try {
            // When...
            jobStoreServiceConnector.addEmptyJob(jobInputStream);
            fail("Illegal datafile for empty job was not rejected");
        } catch (JobStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Then...
            assertThat("job-store service response status", e.getStatusCode(),
                    is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

    }

    /**
     * Given: a valid "empty job" request
     * When : submitted to job-store
     * Then : a job is created
     * And : a job-termination chunk with ID 0 is sent to the sink queue
     * When : a delivery result for the chunk is submitted to job-store
     * Then : the job is completed
     */
    @Test
    public void emptyJob() throws JobStoreServiceConnectorException, JMSException, JSONBException {
        // Given...
        final JobInputStream jobInputStream = newJobInputStream();

        // When...
        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addEmptyJob(jobInputStream);

        // Then...
        assertThat("job is created", jobInfoSnapshot, is(notNullValue()));
        assertThat("job is not complete", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
        // And...
        final List<MockedJmsTextMessage> jmsMessages = jmsQueueServiceConnector.awaitQueueSizeAndList(
                JmsQueueServiceConnector.Queue.SINK, 1, 10000);
        final MockedJmsTextMessage jmsMessage = jmsMessages.get(0);
        final Chunk endChunk = jsonbContext.unmarshall(jmsMessage.getText(), Chunk.class);
        assertThat("chunk ID", endChunk.getChunkId(), is(0L));
        assertThat("chunk belongs to job", endChunk.getJobId(), is((long) jobInfoSnapshot.getJobId()));
        assertThat("number of items in chunk", endChunk.getItems().size(), is(1));
        assertThat("chunk is termination chunk", endChunk.getItems().get(0).getType().get(0),
                is(ChunkItem.Type.JOB_END));

        // When...
        final Chunk result = new Chunk(endChunk.getJobId(), endChunk.getChunkId(), Chunk.Type.DELIVERED);
        result.insertItem(ChunkItem.successfulChunkItem()
                .withId(0)
                .withData("done")
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8));
        jobInfoSnapshot = jobStoreServiceConnector.addChunk(result, endChunk.getJobId(), endChunk.getChunkId());
        assertThat("job is complete", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }

    private JobInputStream newJobInputStream() {
        return new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.PERIODIC)
                .withDataFile(FileStoreUrn.EMPTY_JOB_FILE.toString())
                .withPackaging("addi-xml")
                .withFormat("periode")
                .withCharset("utf8")
                .withDestination("test")
                .withSubmitterId(876070), true, 0);
    }
}
