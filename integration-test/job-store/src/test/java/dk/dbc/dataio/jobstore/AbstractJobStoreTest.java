package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;

public abstract class AbstractJobStoreTest {
    protected static FileStoreServiceConnector fileStoreServiceConnector;
    protected static FlowStoreServiceConnector flowStoreServiceConnector;
    protected static JobStoreServiceConnector jobStoreServiceConnector;

    @BeforeClass
    public static void setupClass() throws ClassNotFoundException {
        final Client httpClient = HttpClient.newClient(new ClientConfig()
                .register(new Jackson2xFeature()));

        fileStoreServiceConnector = new FileStoreServiceConnector(httpClient, ITUtil.FILE_STORE_BASE_URL);
        flowStoreServiceConnector = new FlowStoreServiceConnector(httpClient, ITUtil.FLOW_STORE_BASE_URL);
        jobStoreServiceConnector = new JobStoreServiceConnector(httpClient, ITUtil.NEW_JOB_STORE_BASE_URL);
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
    public void clearFlowStore() {
        ITUtil.clearFlowStore();
    }
}
