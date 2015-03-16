package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;

public class HarvesterJobBuilderFactory {
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public HarvesterJobBuilderFactory(BinaryFileStore binaryFileStore,
        FileStoreServiceConnector fileStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector) {
        this.binaryFileStore = binaryFileStore;
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    public HarvesterJobBuilder newHarvesterJobBuilder(JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector,
                jobSpecificationTemplate);
    }
}
