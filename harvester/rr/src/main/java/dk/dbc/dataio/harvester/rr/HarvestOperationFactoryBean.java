package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
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

    public HarvestOperation createFor(RRHarvesterConfig config) {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStoreBean,
                fileStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector());
        try {
            switch (config.getContent().getHarvesterType()) {
                case IMS:
                    return new ImsHarvestOperation(config,
                            harvesterJobBuilderFactory, taskRepo,
                            vipCoreLibraryRulesConnector, metricRegistry);
                case WORLDCAT:
                    return new WorldCatHarvestOperation(config,
                            harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector,
                            ocnRepo, metricRegistry);
                case UCSYNC:
                    return new UCSyncHarvestOperation(config,harvesterJobBuilderFactory, taskRepo, metricRegistry, vipCoreLibraryRulesConnector);
                default:
                    return new HarvestOperation(config,
                            harvesterJobBuilderFactory, taskRepo,
                            vipCoreLibraryRulesConnector, metricRegistry);
            }
        } catch (ConfigurationException | QueueException | SQLException e) {
            throw new IllegalStateException("ConfigurationException thrown", e);
        }
    }
}
