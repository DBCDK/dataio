package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.jms.JmsQueueTester;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import jakarta.jms.JMSException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

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
     * And : the job-termination item of chunk 0 is sent to the sink queue, unkeyed
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
                JmsQueueTester.Queue.SINK_PERIODIC_JOBS, 1, 10000);
        // Delivery is per item, so the body is the termination item itself and its
        // identity is carried in the message headers.
        final MockedJmsTextMessage jmsMessage = jmsMessages.get(0);
        final ChunkItem endItem = jsonbContext.unmarshall(jmsMessage.getText(), ChunkItem.class);
        final int jobId = Integer.parseInt(jmsMessage.getStringProperty(JMSHeader.jobId.name));
        final long chunkId = Long.parseLong(jmsMessage.getStringProperty(JMSHeader.chunkId.name));
        assertThat("payload type", jmsMessage.getStringProperty(JMSHeader.payload.name),
                is(JMSHeader.ITEM_PAYLOAD_TYPE));
        assertThat("chunk ID", chunkId, is(0L));
        assertThat("chunk belongs to job", jobId, is(jobInfoSnapshot.getJobId()));
        assertThat("item ID", jmsMessage.getStringProperty(JMSHeader.itemId.name), is("0"));
        assertThat("item is termination item", endItem.getType().get(0),
                is(ChunkItem.Type.JOB_END));
        // The termination item is a per-job barrier, not a record, so it carries neither
        // a watermark key nor a broker group and is delivered unconditionally.
        assertThat("termination item has no record key",
                jmsMessage.getStringProperty(JMSHeader.recordKey.name), is(nullValue()));
        assertThat("termination item has no broker group",
                jmsMessage.getStringProperty("JMSXGroupID"), is(nullValue()));

        // When...
        final Chunk result = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        result.insertItem(ChunkItem.successfulChunkItem()
                .withId(0)
                .withData("done")
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8));
        jobInfoSnapshot = jobStoreServiceConnector.addChunk(result, jobId, chunkId);
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
