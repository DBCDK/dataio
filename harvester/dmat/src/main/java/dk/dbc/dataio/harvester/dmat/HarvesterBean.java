package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.ticklerepo.TickleRepo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, DMatHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    static final Metadata recordCounterMetadata = Metadata.builder()
            .withName("dataio_harvester_dmat_record_counter")
            .withDescription("Number of records harvested")
            .withType(MetricType.COUNTER)
            .withUnit("record")
            .build();
    static final Metadata exceptionCounterMetadata = Metadata.builder()
            .withName("dataio_harvester_dmat_exception_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exception")
            .build();

    @EJB
    BinaryFileStoreBean binaryFileStoreBean;
    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    TickleRepo tickleRepo;

    @Inject
    DMatServiceConnector dMatServiceConnector;

    @Inject
    @ConfigProperty(name = "DMAT_DOWNLOAD_URL", defaultValue = "NONE")
    private String dmatDownloadBaseUrl;

    @Inject
    @ConfigProperty(name = "TICKLEREPO_PUBLISHER_DATASET", defaultValue = "150015-forlag")
    private String publisherDatasetName;

    @Inject
    RecordServiceConnector recordServiceConnector;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Override
    public int executeFor(DMatHarvesterConfig config) throws HarvesterException {
        try {
            final HarvestOperation harvestOperation = new HarvestOperation(config,
                    binaryFileStoreBean,
                    fileStoreServiceConnectorBean.getConnector(),
                    flowStoreServiceConnectorBean.getConnector(),
                    jobStoreServiceConnectorBean.getConnector(),
                    dMatServiceConnector, recordServiceConnector,
                    dmatDownloadBaseUrl, tickleRepo, publisherDatasetName);
            final int numberOfRecordsHarvested = harvestOperation.execute();
            metricRegistry.counter(recordCounterMetadata).inc(numberOfRecordsHarvested);
            return numberOfRecordsHarvested;
        } catch (HarvesterException | RuntimeException e) {
            metricRegistry.counter(exceptionCounterMetadata).inc();
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
