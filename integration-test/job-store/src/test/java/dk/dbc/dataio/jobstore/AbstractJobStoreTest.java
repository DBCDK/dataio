package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.jms.JMSException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractJobStoreTest {
    protected static Client restClient;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException {
        restClient = HttpClient.newClient(new ClientConfig()
                .register(new Jackson2xFeature()));
    }

    @AfterClass
    public static void clearJobStore() {
        ITUtil.clearJobStore();
    }

    @AfterClass
    public static void clearFileStore() {
        ITUtil.clearFileStore();
    }

    @After
    public void emptyQueues() {
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);
    }

    @After
    public void emptyFlowStoreDatabase() throws SQLException, ClassNotFoundException {
        try (final Connection connection = ITUtil.newDbConnection(ITUtil.FLOW_STORE_DATABASE_NAME)) {
            ITUtil.clearAllDbTables(connection);
        }
    }

    static NewJob assertNewJobMessageForProcessor(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        return JsonUtil.fromJson(message.getText(), NewJob.class, MixIns.getMixIns());
    }

    static JobInfo createJob(Client restClient, JobSpecification jobSpecification) throws URISyntaxException, JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(restClient, ITUtil.JOB_STORE_BASE_URL);
        return jobStoreServiceConnector.createJob(jobSpecification);
    }

    static Chunk getChunk(Client restClient, long jobId, long chunkId) throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(restClient, ITUtil.JOB_STORE_BASE_URL);
        return jobStoreServiceConnector.getChunk(jobId, chunkId);
    }

    static JobState getState(Client restClient, long jobId) throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(restClient, ITUtil.JOB_STORE_BASE_URL);
        return jobStoreServiceConnector.getState(jobId);
    }

    static ChunkResult getProcessorResult(Client restClient, long jobId, long chunkId) throws JsonException {
        final Response response = ITUtil.getJobProcessorResult(restClient, jobId, chunkId);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));
        return JsonUtil.fromJson(response.readEntity(String.class), ChunkResult.class, MixIns.getMixIns());
    }

    static SinkChunkResult getSinkResult(Client restClient, long jobId, long chunkId) throws JobStoreServiceConnectorException {
        final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(restClient, ITUtil.JOB_STORE_BASE_URL);
        return jobStoreServiceConnector.getSinkChunkResult(jobId, chunkId);
    }
}
