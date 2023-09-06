package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.weekresolver.WeekResolverConnector;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, PeriodicJobsHarvesterConfig> {
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
    WeekResolverConnector weekresolverConnector;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;
    final static AtomicInteger THREAD_ID = new AtomicInteger();
    static final ExecutorService WITH_HOLDINGS_EXECUTOR = Executors.
                newFixedThreadPool(5, runnable -> new Thread(runnable,
                        "standard-with-holdings"+THREAD_ID.getAndIncrement()));
    @Override
    public int executeFor(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = getHarvesterOperation(config);

        return harvestOperation.execute();
    }

    public String validateQuery(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = getHarvesterOperation(config);

        return harvestOperation.validateQuery();
    }

    private HarvestOperation getHarvesterOperation(PeriodicJobsHarvesterConfig config) {
        final HarvestOperation harvestOperation;
        LOGGER.info("Starting {} harvest", config.getContent().getHarvesterType());
        switch (config.getContent().getHarvesterType()) {
            case DAILY_PROOFING:
                harvestOperation = new DailyProofingHarvestOperation(config,
                        binaryFileStoreBean,
                        fileStoreServiceConnectorBean.getConnector(),
                        flowStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(),
                        weekresolverConnector,
                        executor);
                break;
            case SUBJECT_PROOFING:
                harvestOperation = new SubjectProofingHarvestOperation(config,
                        binaryFileStoreBean,
                        fileStoreServiceConnectorBean.getConnector(),
                        flowStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(),
                        weekresolverConnector,
                        executor);
                break;
            case STANDARD_WITH_HOLDINGS:
                harvestOperation = new RecordsWithoutHoldingsHarvestOperation(config,
                        binaryFileStoreBean,
                        fileStoreServiceConnectorBean.getConnector(),
                        flowStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(),
                        weekresolverConnector,
                        WITH_HOLDINGS_EXECUTOR);
                break;
            default:
                harvestOperation = new HarvestOperation(config,
                        binaryFileStoreBean,
                        fileStoreServiceConnectorBean.getConnector(),
                        flowStoreServiceConnectorBean.getConnector(),
                        jobStoreServiceConnectorBean.getConnector(),
                        weekresolverConnector,
                        executor);
        }

        return harvestOperation;
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
