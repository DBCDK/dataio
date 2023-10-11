package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class HarvesterJobBuilderFactoryTest {
    private static final String EMPTY = "";
    private final BinaryFileStore binaryFileStore = mock(BinaryFileStore.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobSpecification jobSpecificationTemplate = getJobSpecificationTemplate();

    @Test(expected = NullPointerException.class)
    public void newHarvesterJobBuilder_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(null, fileStoreServiceConnector, jobStoreServiceConnector);
        harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void newHarvesterJobBuilder_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStore, null, jobStoreServiceConnector);
        harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void newHarvesterJobBuilder_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, null);
        harvesterJobBuilderFactory.newHarvesterJobBuilder(jobSpecificationTemplate);
    }

    @Test(expected = NullPointerException.class)
    public void newHarvesterJobBuilder_jobSpecificationArgIsNull_throws() throws HarvesterException {
        HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
        harvesterJobBuilderFactory.newHarvesterJobBuilder(null);
    }

    private JobSpecification getJobSpecificationTemplate() {
        return new JobSpecification().withPackaging("packaging").withFormat("format").withCharset("utf8").withDestination("destination").withSubmitterId(222).withMailForNotificationAboutVerification(EMPTY).withMailForNotificationAboutProcessing(EMPTY).withResultmailInitials(EMPTY).withDataFile("datafile").withType(JobSpecification.Type.TEST);
    }
}
