package dk.dbc.dataio.harvester.oai;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.OaiHarvesterConfig;
import dk.dbc.oai.OaiConnectorException;
import dk.dbc.oai.OaiConnectorFactory;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, OaiHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB
    BinaryFileStoreBean binaryFileStoreBean;
    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public int executeFor(OaiHarvesterConfig config) throws HarvesterException {
        try {
            return new HarvestOperation(config,
                    binaryFileStoreBean,
                    flowStoreServiceConnectorBean.getConnector(),
                    fileStoreServiceConnectorBean.getConnector(),
                    jobStoreServiceConnectorBean.getConnector(),
                    OaiConnectorFactory.create(config.getContent().getEndpoint()))
                    .execute();
        } catch (OaiConnectorException e) {
            throw new HarvesterException(e);
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
