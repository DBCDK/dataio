package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import org.junit.Test;

import javax.jms.JMSException;
import javax.ws.rs.client.Client;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobsBeanIT extends AbstractJobStoreTest {
    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;
    private static final String DATA_FILE_RESOURCE = "/data.xml";

    @Test
    public void createJob_newJobIsCreated_chunksArePutOnProcessorQueue()
            throws InterruptedException, JsonException, URISyntaxException, JMSException, JobStoreServiceConnectorException {
        final JobSpecification jobSpecification = setupJobPrerequisites(restClient);
        final JobInfo jobInfo = createJob(restClient, jobSpecification);

        final List<MockedJmsTextMessage> processorQueue = JmsQueueConnector.awaitQueueList(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);
        assertThat(processorQueue.size(), is(2));
        final Chunk chunk1 = assertChunkMessageForProcessor(processorQueue.get(0));
        assertThat(chunk1.getJobId(), is(jobInfo.getJobId()));
        final Chunk chunk2 = assertChunkMessageForProcessor(processorQueue.get(1));
        assertThat(chunk2.getJobId(), is(jobInfo.getJobId()));
    }

    public static JobSpecification setupJobPrerequisites(Client restClient) throws URISyntaxException {
        final String packaging = "xml";
        final String format = "nmxml";
        final String charset = "utf8";
        final String destination = "database";
        final long submitterNumber = 422442;
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
