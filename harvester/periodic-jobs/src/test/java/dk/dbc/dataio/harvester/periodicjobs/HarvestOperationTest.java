/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.testee.NonContainerManagedExecutorService;
import dk.dbc.testee.SameThreadExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class HarvestOperationTest {
    private static final String SOLR_COLLECTION = "testCollection";

    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final RecordSearcher recordSearcher = mock(RecordSearcher.class);
    private final ManagedExecutorService managedExecutorService = new NonContainerManagedExecutorService(
            new SameThreadExecutorService());
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
                eq(SOLR_COLLECTION), eq("datefield:[2019-01-14T07:00Z TO *]"), any(BinaryFile.class));

        assertThat("timeOfSearch", harvestOperation.timeOfSearch, is(notNullValue()));
    }

    @Test
    public void executeWithRecordIdsFile() throws HarvesterException, IOException {
        final Path originalFile = Paths.get("src/test/resources/record-ids.txt");
        final Path copy = Paths.get("target/record-ids.txt");
        Files.copy(originalFile, copy, StandardCopyOption.REPLACE_EXISTING);
        final BinaryFileFsImpl recordsIdFile = new BinaryFileFsImpl(copy);

        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());

        final HarvestOperation harvestOperation = newHarvestOperation(config);
        HarvestOperation.MAX_NUMBER_OF_TASKS = 3;
        doReturn(recordServiceConnector).when(harvestOperation).createRecordServiceConnector();

        assertThat("records harvested", harvestOperation.execute(recordsIdFile), is(10));
        assertThat("record-ids.txt is deleted", !Files.exists(copy), is(true));
    }

    private HarvestOperation newHarvestOperation(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        final HarvestOperation harvestOperation = spy(new HarvestOperation(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                managedExecutorService,
                rawRepoConnector));
        doReturn(recordSearcher).when(harvestOperation).createRecordSearcher();
        return harvestOperation;
    }
}