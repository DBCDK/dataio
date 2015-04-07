package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueueIT extends AbstractJobStoreTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public TestName test = new TestName();

    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private JMSContext jmsContext;

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void addChunk_jobStateUpdatedAndWorkloadPublished()
            throws IOException, JobStoreServiceConnectorException, JSONBException, JMSException, URISyntaxException {
        final int expectedNumberOfRecords = 11;
        final String fileId = HarvesterJobIT.createMarcxchangeHarvesterDataFile(tmpFolder.newFile(), expectedNumberOfRecords);
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                    .setPackaging("xml")
                    .setFormat("basis")
                    .setCharset("utf8")
                    .setDestination(test.getMethodName())
                    .setSubmitterId(870970)
                    .setDataFile(FileStoreUrn.create(fileId).toString())
                    .build();
        HarvesterJobIT.createFlowStoreEnvironmentMatchingJobSpecification(jobSpecification);

        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(HarvesterJobIT.getJobInputStream(jobSpecification));

        // Swallow 1st Chunk message
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Partitioning phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PARTITIONING), is(true));
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(false));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(false));

        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0).build(),
                new ChunkItemBuilder().setId(1).build(),
                new ChunkItemBuilder().setId(2).build(),
                new ChunkItemBuilder().setId(3).build(),
                new ChunkItemBuilder().setId(4).build(),
                new ChunkItemBuilder().setId(5).build(),
                new ChunkItemBuilder().setId(6).build(),
                new ChunkItemBuilder().setId(7).build(),
                new ChunkItemBuilder().setId(8).build(),
                new ChunkItemBuilder().setId(9).build());

        // Put 1st sink result on queue
        ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();
        jobStoreServiceConnector.addChunk(deliveredChunk, jobInfoSnapshot.getJobId(), 0);

        // Put 1st processor result on queue
        ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();
        jobStoreServiceConnector.addChunk(processedChunk, jobInfoSnapshot.getJobId(), 0);

        // Swallow 2nd Chunk message
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(false));
        assertThat("Processing phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(processedChunk.size()));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(false));
        assertThat("Delivering phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(deliveredChunk.size()));

        // Put 2nd processor result on queue
        processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(1L)
                .build();
        jobStoreServiceConnector.addChunk(processedChunk, jobInfoSnapshot.getJobId(), 1);

        // Put 2nd sink result on queue
        deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(1L)
                .build();
        jobStoreServiceConnector.addChunk(deliveredChunk, jobInfoSnapshot.getJobId(), 1);

        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 0, MAX_QUEUE_WAIT_IN_MS);

        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(true));
        assertThat("Processing phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(expectedNumberOfRecords));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(true));
        assertThat("Delivering phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(expectedNumberOfRecords));
    }

    private JobInfoSnapshot getJob(int jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        return jobStoreServiceConnector.listJobs(criteria).get(0);
    }
}
