package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.ticklerepo.TickleRepo;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;

@Stateless
public class HarvestOperationFactoryBean {

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    @Inject
    @ConfigProperty(name = "RAWREPO_RECORD_SERVICE_URL")
    private String recordServiceBaseUrl;

    @EJB
    public BinaryFileStoreBean binaryFileStoreBean;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    public TickleRepo tickleRepo;

    @EJB
    public TaskRepo taskRepo;

    RecordServiceConnector recordServiceConnector;

    @PostConstruct
    public void createRecordServiceConnector() {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client,
                UserAgent.forInternalRequests(), RETRY_POLICY);
        recordServiceConnector = new RecordServiceConnector(failSafeHttpClient, recordServiceBaseUrl);
    }

    public HarvestOperation createFor(TickleRepoHarvesterConfig config) {
        switch (config.getContent().getHarvesterType()) {
            case VIAF:
                return new ViafHarvestOperation(config, flowStoreServiceConnectorBean.getConnector(),
                        binaryFileStoreBean, fileStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(), tickleRepo, taskRepo,
                        recordServiceConnector);
            default:
                return new HarvestOperation(config, flowStoreServiceConnectorBean.getConnector(),
                        binaryFileStoreBean, fileStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(), tickleRepo, taskRepo);
        }
    }
}
