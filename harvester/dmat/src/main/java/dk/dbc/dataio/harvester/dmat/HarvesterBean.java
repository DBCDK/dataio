package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, DMatHarvesterConfig> {
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
    DMatServiceConnector dMatServiceConnector;

    @Inject
    @ConfigProperty(name = "DMAT_DOWNLOAD_URL", defaultValue = "NONE")
    private String dmatDownloadBaseUrl;

    @Inject
    RecordServiceConnector recordServiceConnector;

    @Inject
    MetricsHandlerBean metricsHandler;

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
            metricsHandler.increment(DmatHarvesterMetrics.UNHANDLED_EXCEPTIONS);
            throw e;
        } catch(Exception e) {
            LOGGER.error(String.format("HarvestOperation resulted in unhandled exception %s", e.getMessage()), e);
            metricsHandler.increment(DmatHarvesterMetrics.UNHANDLED_EXCEPTIONS);
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
