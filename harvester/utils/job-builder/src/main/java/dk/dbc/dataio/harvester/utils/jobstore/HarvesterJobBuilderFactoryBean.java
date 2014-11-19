package dk.dbc.dataio.harvester.utils.jobstore;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;

@Singleton
public class HarvesterJobBuilderFactoryBean {
    @EJB
    public BinaryFileStoreBean binaryFileStore;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    public HarvesterJobBuilder newHarvesterJobBuilder(JobSpecification jobSpecificationTemplate)
            throws NullPointerException, EJBException, HarvesterException {
        return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnectorBean.getConnector(),
                jobStoreServiceConnectorBean.getConnector(), jobSpecificationTemplate);
    }
}
