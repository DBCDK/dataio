package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import dk.dbc.weekresolver.WeekResolverConnector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(
            new SameThreadExecutorService());
    private FileStoreServiceConnector fileStoreServiceConnector;
    private BinaryFileStore binaryFileStore;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setupMocks() {
        try {
            environmentVariables.set("TZ", "Europe/Copenhagen");
            binaryFileStore = new BinaryFileStoreFsImpl(tmpFolder.newFolder().toPath());
            fileStoreServiceConnector = new MockedFileStoreServiceConnector();
                ((MockedFileStoreServiceConnector) fileStoreServiceConnector)
                    .destinations.add(tmpFolder.newFile().toPath());
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
        final ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00.00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withCollection(SOLR_COLLECTION)
                        .withQuery("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]")
                        .withTimeOfLastHarvest(Date.from(timeOfLastHarvest.toInstant())));

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        doReturn(42).when(harvestOperation).execute(any(BinaryFile.class));

        assertThat("number of records harvested", harvestOperation.execute(), is(42));

        verify(recordSearcher).search(
                eq(SOLR_COLLECTION), eq("datefield:[2019-01-14T07:00:00.000000Z TO *]"), any(BinaryFile.class));

        assertThat("timeOfSearch", harvestOperation.timeOfSearch, is(notNullValue()));
    }

    @Test
    public void executeWithSolrSearchFromFile() throws HarvesterException, FileNotFoundException, FileStoreServiceConnectorException {
        final File fileStoreFile = new File("src/test/resources/solr-queries.txt");
        final String fileId = fileStoreServiceConnector.addFile(new FileInputStream(fileStoreFile));
        final ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00.00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withCollection(SOLR_COLLECTION)
                        .withQueryFileId(fileId)
                        .withTimeOfLastHarvest(Date.from(timeOfLastHarvest.toInstant())));

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        doReturn(2).when(harvestOperation).execute(any(BinaryFile.class));

        assertThat("number of records harvested", harvestOperation.execute(), is(2));

        verify(recordSearcher).search(
                eq(SOLR_COLLECTION), eq("id:\"20663723:870970\""), any(BinaryFile.class));
        verify(recordSearcher).search(
                eq(SOLR_COLLECTION), eq("id:\"20663650:870970\""), any(BinaryFile.class));
        assertThat("timeOfSearch", harvestOperation.timeOfSearch, is(notNullValue()));
    }

    @Test
    public void executeWithRecordIdsFile() throws HarvesterException, IOException, JobStoreServiceConnectorException,
                                                  FlowStoreServiceConnectorException {
        final Path originalFile = Paths.get("src/test/resources/record-ids.txt");
        final Path copy = Paths.get("target/record-ids.txt");
        Files.copy(originalFile, copy, StandardCopyOption.REPLACE_EXISTING);
        final BinaryFileFsImpl recordsIdFile = new BinaryFileFsImpl(copy);

        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456"));

        final Date timeOfSearch = new Date();

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        HarvestOperation.MAX_NUMBER_OF_TASKS = 3;
        harvestOperation.timeOfSearch = timeOfSearch;
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        assertThat("records harvested", harvestOperation.execute(recordsIdFile), is(10));
        assertThat("record-ids.txt is deleted", !Files.exists(copy), is(true));
        assertThat("time of last harvest", config.getContent().getTimeOfLastHarvest(), is(timeOfSearch));

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(config);
    }

    @Test
    public void executeWithRecordIdsFileWhenTimeOfSearchIsNotDefined()
            throws HarvesterException, IOException, JobStoreServiceConnectorException,
                   FlowStoreServiceConnectorException {
        final Path originalFile = Paths.get("src/test/resources/record-ids.txt");
        final Path copy = Paths.get("target/record-ids.txt");
        Files.copy(originalFile, copy, StandardCopyOption.REPLACE_EXISTING);
        final BinaryFileFsImpl recordsIdFile = new BinaryFileFsImpl(copy);

        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456"));

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        assertThat("records harvested", harvestOperation.execute(recordsIdFile), is(10));
        assertThat("record-ids.txt is deleted", !Files.exists(copy), is(true));

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        // Updated config is only sent to flow-store when time-of-search is defined
        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(config);
    }

    @Test
    public void executeWhenNoRecordIdsFound() throws IOException, HarvesterException,
            JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final BinaryFile emptyFile = new BinaryFileFsImpl(tmpFolder.newFile().toPath());

        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("123456"));

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        harvestOperation.timeOfSearch = new Date();
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        assertThat("records harvested", harvestOperation.execute(emptyFile), is(0));
        assertThat("empty file is deleted", !emptyFile.exists(), is(true));
        verify(jobStoreServiceConnector).addEmptyJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(config);
    }

    private HarvestOperation newHarvestOperation(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        final HarvestOperation harvestOperation = spy(new HarvestOperation(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                weekResolverConnector,
                managedExecutorService,
                rawRepoConnector));
        doReturn(recordSearcher).when(harvestOperation).createRecordSearcher();
        return harvestOperation;
    }
}
