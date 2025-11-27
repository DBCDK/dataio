package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import java.sql.SQLException;

@Stateless
public class HarvestOperationFactoryBean {
    @EJB
    public BinaryFileStoreBean binaryFileStoreBean;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    public OcnRepo ocnRepo;

    @EJB
    public TaskRepo taskRepo;

    @Inject
    private VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    public HarvestOperation createFor(RRV3HarvesterConfig config, String workerKey) {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStoreBean,
                fileStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector());
        try {
            return switch (config.getContent().getHarvesterType()) {
                case IMS -> new ImsHarvestOperation(workerKey, config, harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector, metricRegistry);
                case WORLDCAT -> new WorldCatHarvestOperation(workerKey, config, harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector, ocnRepo, metricRegistry);
                default -> new HarvestOperation(workerKey, config, harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector, metricRegistry);
            };
        } catch (ConfigurationException | QueueException | SQLException e) {
            throw new IllegalStateException("ConfigurationException thrown", e);
        }
    }
}
