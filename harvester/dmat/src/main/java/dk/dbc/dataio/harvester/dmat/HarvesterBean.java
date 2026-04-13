package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.JacksonConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, DMatHarvesterConfig> {

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @Inject
    @ConfigProperty(name = "DMAT_SERVICE_URL", defaultValue = "NONE")
    private String dmatServiceBaseUrl;

    @Inject
    @ConfigProperty(name = "DMAT_DOWNLOAD_URL", defaultValue = "NONE")
    private String dmatDownloadBaseUrl;

    @Inject
    @ConfigProperty(name = "RAWREPO_RECORD_SERVICE_URL")
    private String recordServiceBaseUrl;

    @EJB
    BinaryFileStoreBean binaryFileStoreBean;
    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    DMatServiceConnector dMatServiceConnector;
    RecordServiceConnector recordServiceConnector;

    @Inject
    MetricRegistry metricsHandler;

    @PostConstruct
    public void init() {
        final Client dmatClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonConfig())
                .register(new JacksonFeature()));
        final FailSafeHttpClient dmatFailSafeHttpClient = FailSafeHttpClient.create(dmatClient,
                UserAgent.forInternalRequests(), RETRY_POLICY);
        dMatServiceConnector = new DMatServiceConnector(dmatFailSafeHttpClient, dmatServiceBaseUrl);

        final Client recordServiceClient = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final FailSafeHttpClient recordServiceFailSafeHttpClient = FailSafeHttpClient.create(recordServiceClient,
                UserAgent.forInternalRequests(), RETRY_POLICY);
        recordServiceConnector = new RecordServiceConnector(recordServiceFailSafeHttpClient, recordServiceBaseUrl);
    }

    @Override
    public int executeFor(DMatHarvesterConfig config) throws HarvesterException {
        try {
            final HarvestOperation harvestOperation = new HarvestOperation(config,
                    binaryFileStoreBean,
                    fileStoreServiceConnectorBean.getConnector(),
                    flowStoreServiceConnectorBean.getConnector(),
                    jobStoreServiceConnectorBean.getConnector(),
                    dMatServiceConnector, recordServiceConnector,
                    dmatDownloadBaseUrl,
                    metricsHandler);
            return harvestOperation.execute();
        } catch (HarvesterException e) {
            LOGGER.error("HarvestOperation resulted in HarvesterException {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error(String.format("HarvestOperation resulted in RuntimeException %s", e.getMessage()), e);
            metricsHandler.counter(DmatHarvesterMetrics.UNHANDLED_EXCEPTIONS.getMetadata()).inc();
            throw e;
        } catch(Exception e) {
            LOGGER.error(String.format("HarvestOperation resulted in unhandled exception %s", e.getMessage()), e);
            metricsHandler.counter(DmatHarvesterMetrics.UNHANDLED_EXCEPTIONS.getMetadata()).inc();
            throw e;
        }
    }

    @Override
    public HarvesterBean self() {
        return sessionContext.getBusinessObject(HarvesterBean.class);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
