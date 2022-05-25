package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorFactory;
import dk.dbc.ticklerepo.TickleRepo;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class HarvestOperationFactoryBean {
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

    /*
       Our current version of payara application server does not
       include the microprofile libraries, so for now we are not
       able to @Inject the RecordServiceConnector.

       Therefore CDI scanning of the rawrepo-record-service-connector
       jar dependency has to be disabled via scanning-exclude
       element in glassfish-web.xml
     */

    //@Inject
    RecordServiceConnector recordServiceConnector;

    @PostConstruct
    public void createRecordServiceConnector() {
        recordServiceConnector = RecordServiceConnectorFactory.create(
                System.getenv("RAWREPO_RECORD_SERVICE_URL"));
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
