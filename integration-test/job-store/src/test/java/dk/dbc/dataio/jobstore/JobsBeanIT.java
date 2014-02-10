package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.types.JobState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jms.JMSException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JobsBeanIT {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;
    private static final String DATA_FILE_RESOURCE = "/data.xml";

    private static Client restClient;

    @BeforeClass
    public static void setUpClass() throws ClassNotFoundException {
        restClient = HttpClient.newClient();
    }

    @AfterClass
    public static void clearJobStore() {
        ITUtil.clearJobStore();
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

    @Test
    public void createJob_newJobIsCreated_newJobMessagePutOnProcessorQueue() throws InterruptedException, JsonException, URISyntaxException, JMSException {
        final JobInfo jobInfo = createJob(restClient);

        final List<MockedJmsTextMessage> processorQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 1, MAX_QUEUE_WAIT_IN_MS);
        assertThat(processorQueue.size(), is(1));
        final NewJob newJob = assertNewJobMessageForProcessor(processorQueue.get(0));
        assertThat(newJob.getJobId(), is(jobInfo.getJobId()));
        assertThat(newJob.getChunkCount(), is(2L));
    }

    private NewJob assertNewJobMessageForProcessor(MockedJmsTextMessage message) throws JMSException, JsonException {
        assertThat(message, is(notNullValue()));
        return JsonUtil.fromJson(message.getText(), NewJob.class, MixIns.getMixIns());
    }

    static JobInfo createJob(Client restClient) throws URISyntaxException, JsonException {
        final JobSpecification jobSpecification = setupJobPrerequisites(restClient);
        final Response response = ITUtil.createJob(restClient, JsonUtil.toJson(jobSpecification));
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));
        return JsonUtil.fromJson(response.readEntity(String.class), JobInfo.class, MixIns.getMixIns());
    }

    static JobState getState(Client restClient, long jobId) throws JsonException {
        final Response response = ITUtil.getJobState(restClient, jobId);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));
        return JsonUtil.fromJson(response.readEntity(String.class), JobState.class, MixIns.getMixIns());
    }

    static ChunkResult getProcessorResult(Client restClient, long jobId, long chunkId) throws JsonException {
        final Response response = ITUtil.getJobProcessorResult(restClient, jobId, chunkId);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.OK.getStatusCode()));
        return JsonUtil.fromJson(response.readEntity(String.class), ChunkResult.class, MixIns.getMixIns());
    }

    private static JobSpecification setupJobPrerequisites(Client restClient) throws URISyntaxException {
        final String packaging = "xml";
        final String format = "nmxml";
        final String charset = "utf8";
        final String destination = "database";
        final long submitterNumber = 42;
        final long flowId = ITUtil.createFlow(restClient, ITUtil.FLOW_STORE_BASE_URL, new FlowContentJsonBuilder().build());
        final long sinkId = ITUtil.createSink(restClient, ITUtil.FLOW_STORE_BASE_URL, new SinkContentJsonBuilder().build());
        final long submitterId = ITUtil.createSubmitter(restClient, ITUtil.FLOW_STORE_BASE_URL,
                new SubmitterContentJsonBuilder()
                        .setNumber(submitterNumber)
                        .build());
        ITUtil.createFlowBinder(restClient, ITUtil.FLOW_STORE_BASE_URL,
                new FlowBinderContentJsonBuilder()
                        .setPackaging(packaging)
                        .setFormat(format)
                        .setCharset(charset)
                        .setDestination(destination)
                        .setRecordSplitter("DefaultXMLRecordSplitter")
                        .setFlowId(flowId)
                        .setSinkId(sinkId)
                        .setSubmitterIds(Arrays.asList(submitterId))
                        .build());

        final Path dataFile = java.nio.file.Paths.get(JobsBeanIT.class.getResource(DATA_FILE_RESOURCE).toURI());

        return new JobSpecification(packaging, format, charset, destination, submitterNumber, "", "", "",
                dataFile.toAbsolutePath().toString());
    }
}
