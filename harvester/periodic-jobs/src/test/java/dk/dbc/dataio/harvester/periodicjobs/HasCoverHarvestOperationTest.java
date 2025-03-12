package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class HasCoverHarvestOperationTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final WeekResolverConnector weekResolverConnector = mock(WeekResolverConnector.class);
    private final RecordSearcher recordSearcher = mock(RecordSearcher.class);
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(new SameThreadExecutorService());
    private FileStoreServiceConnector fileStoreServiceConnector;
    private BinaryFileStore binaryFileStore;
    private final FbiInfoConnector fbiInfoConnector = mock(FbiInfoConnector.class);

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setupMocks() {
        try {
            binaryFileStore = new BinaryFileStoreFsImpl(Files.createDirectory(tmpFolder.resolve("fs-" + UUID.randomUUID())));
            fileStoreServiceConnector = new MockedFileStoreServiceConnector();
            ((MockedFileStoreServiceConnector) fileStoreServiceConnector)
                    .destinations.add(Files.createFile(tmpFolder.resolve(UUID.randomUUID() + ".tmp")));
            when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                    .thenReturn(new JobInfoSnapshot());
            when(jobStoreServiceConnector.addEmptyJob(any(JobInputStream.class)))
                    .thenReturn(new JobInfoSnapshot());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test @Disabled("Explorative test to verify that the connector is working against the actual service")
    public void test() {
        FbiInfoConnector connector = new FbiInfoConnector("http://fbiinfo-service.cisterne.svc.cloud.dbc.dk/api/v1");
        Set<RecordIdDTO> filter = connector.hasCoverFilter(List.of(new RecordIdDTO("137198827", 870970), new RecordIdDTO("123123123", 870970)));
        assertThat(filter.size(), is(1));
    }

    @Test
    public void executeWithRecordIdsFile() throws HarvesterException, IOException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        Path originalFile = Paths.get("src/test/resources/record-ids-noinvalid.txt");
        Path copy = Paths.get("target/record-ids-noinvalid.txt");
        Files.copy(originalFile, copy, StandardCopyOption.REPLACE_EXISTING);
        BinaryFileFsImpl recordsIdFile = new BinaryFileFsImpl(copy);

        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456"));

        Date timeOfSearch = new Date();

        HarvestOperation harvestOperation = newHarvestOperation(config);
        HarvestOperation.MAX_NUMBER_OF_TASKS = 3;
        harvestOperation.timeOfSearch = timeOfSearch;
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();
        RecordIdDTO recordWithCover = new RecordIdDTO("id2", 123456);
        when(fbiInfoConnector.hasCoverFilter(any())).thenReturn(Set.of(recordWithCover));
        assertThat("records harvested", harvestOperation.execute(recordsIdFile), is(9));
    }

    private HarvestOperation newHarvestOperation(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = spy(new HasCoverHarvestOperation(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                weekResolverConnector,
                fbiInfoConnector,
                managedExecutorService,
                rawRepoConnector));
        doReturn(recordSearcher).when(harvestOperation).createRecordSearcher();
        return harvestOperation;
    }
}
