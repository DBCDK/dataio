package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import static dk.dbc.dataio.commons.types.Constants.ZONE_CPH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private static final String SOLR_COLLECTION = "testCollection";

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final WeekResolverConnector weekResolverConnector = mock(WeekResolverConnector.class);
    private final RecordSearcher recordSearcher = mock(RecordSearcher.class);
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(new SameThreadExecutorService());
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private BinaryFileStore binaryFileStore;
    private FbiInfoConnector fbiInfoConnector = mock(FbiInfoConnector.class);

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void setupMocks() {
        try {
            binaryFileStore = new BinaryFileStoreFsImpl(Files.createDirectory(tmpFolder.resolve("fs-" + UUID.randomUUID())));
            fileStoreServiceConnector = new MockedFileStoreServiceConnector();
            fileStoreServiceConnector.destinations.add(Files.createFile(tmpFolder.resolve(UUID.randomUUID() + ".tmp")));
            when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                    .thenReturn(new JobInfoSnapshot());
            when(jobStoreServiceConnector.addEmptyJob(any(JobInputStream.class)))
                    .thenReturn(new JobInfoSnapshot());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void executeWithSolrSearch() throws HarvesterException {
        ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00.00Z").atZone(ZONE_CPH);
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withCollection(SOLR_COLLECTION)
                        .withQuery("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]")
                        .withTimeOfLastHarvest(Date.from(timeOfLastHarvest.toInstant())));

        HarvestOperation harvestOperation = newHarvestOperation(config);
        doReturn(42).when(harvestOperation).execute(any(BinaryFile.class));

        assertThat("number of records harvested", harvestOperation.execute(), is(42));

        verify(recordSearcher).search(
                eq(SOLR_COLLECTION), eq("datefield:[2019-01-14T07:00:00.000000Z TO *]"), any(BinaryFile.class));

        assertThat("timeOfSearch", harvestOperation.timeOfSearch, is(notNullValue()));
    }

    @Test
    public void executeRecordIdsFromFile() throws HarvesterException, FileStoreServiceConnectorException, RecordServiceConnectorException, IOException {
        Path file = Paths.get("src/test/resources/record-ids-noinvalid.txt");
        String fileId = fileStoreServiceConnector.addFile(Files.newInputStream(file));
        ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00.00Z").atZone(ZONE_CPH);
        PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456")
                        .withQueryFileId(fileId)
                        .withTimeOfLastHarvest(Date.from(timeOfLastHarvest.toInstant())));
        fileStoreServiceConnector.destinations.add(Files.createFile(tmpFolder.resolve(UUID.randomUUID() + ".tmp")));
        HarvestOperation harvestOperation = newHarvestOperation(config);
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();
        Assertions.assertEquals(10, harvestOperation.execute(), "There should be 10 records harvested");
        RecordIdDTO r1 = new RecordIdDTO("id4", 123456);
        RecordIdDTO r2 = new RecordIdDTO("id10", 987654);
        verify(recordServiceConnector).getRecordDataCollection(eq(r1), any());
        verify(recordServiceConnector).getRecordDataCollection(eq(r2), any());
    }

    private HarvestOperation newHarvestOperation(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = spy(new HarvestOperation(config,
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
