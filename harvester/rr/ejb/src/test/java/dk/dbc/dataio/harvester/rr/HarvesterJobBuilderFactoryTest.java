package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class HarvesterJobBuilderFactoryTest {
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobSpecification jobSpecificationTemplate = new JobSpecificationBuilder().build();

    @Test
    public void newHarvesterJobBuilder_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(null, fileStoreServiceConnector, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, null, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, null);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newHarvesterJobBuilder_jobSpecificationArgIsNull_throws() throws HarvesterException {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory =
                new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
        try {
            harvesterJobBuilderFactory.newHarvesterJobBuilder(null);
        } catch (NullPointerException e) {
        }
    }
}