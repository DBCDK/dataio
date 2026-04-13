package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, PeriodicJobsHarvesterConfig> {

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB
    BinaryFileStoreBean binaryFileStoreBean;
    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Inject
    @ConfigProperty(name = "WEEKRESOLVER_SERVICE_URL")
    private String weekResolverUrl;
    WeekResolverConnector weekresolverConnector;

    @Inject
    @ConfigProperty(name = "FBI_INFO_URL")
    private String fbiInfoUrl;
    private FbiInfoConnector fbiInfoConnector;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;
    final static AtomicInteger THREAD_ID = new AtomicInteger();
    static final ExecutorService WITH_HOLDINGS_EXECUTOR = Executors.
                newFixedThreadPool(5, runnable -> new Thread(runnable,
                        "standard-with-holdings"+THREAD_ID.getAndIncrement()));

    @PostConstruct
    public void init() {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client,
                UserAgent.forInternalRequests(), RETRY_POLICY);
        weekresolverConnector = new WeekResolverConnector(failSafeHttpClient, weekResolverUrl);
        fbiInfoConnector = new FbiInfoConnector(UserAgent.forInternalRequests(), fbiInfoUrl);
    }

    @Override
    public int executeFor(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = getHarvesterOperation(config);

        return harvestOperation.execute();
    }

    public String validateQuery(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = getHarvesterOperation(config);

        return harvestOperation.validateQuery();
    }

    private interface HarvesterChoice {
        HarvestOperation make(PeriodicJobsHarvesterConfig config, BinaryFileStore binaryFileStore, FileStoreServiceConnector fileCon, FlowStoreServiceConnector flowCon, JobStoreServiceConnector jobCon, WeekResolverConnector weekCon, FbiInfoConnector fbiInfoConnector, ManagedExecutorService executor);
    }

    private HarvestOperation getHarvesterOperation(PeriodicJobsHarvesterConfig config) {
        LOGGER.info("Starting {} harvest", config.getContent().getHarvesterType());

        HarvesterChoice choice = switch (config.getContent().getHarvesterType()) {
            case DAILY_PROOFING -> DailyProofingHarvestOperation::new;
            case SUBJECT_PROOFING -> SubjectProofingHarvestOperation::new;
            case STANDARD_WITH_HOLDINGS -> RecordsWithoutHoldingsHarvestOperation::new;
            case STANDARD_WITHOUT_EXPANSION -> RecordsWithoutExpansionHarvestOperation::new;
            case HAS_COVER_PAGE -> HasCoverHarvestOperation::new;
            default -> HarvestOperation::new;
        };
        return choice.make(config, binaryFileStoreBean, fileStoreServiceConnectorBean.getConnector(), flowStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector(), weekresolverConnector, fbiInfoConnector, executor);
    }

    @Asynchronous
    public void asyncExecuteFor(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        executeFor(config);
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
