/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

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
    private static final String SOLR_ZK_HOST = "host:port/test";

    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final RawRepoConnector rawRepoConnector = mock(RawRepoConnector.class);
    private final RecordSearcher recordSearcher = mock(RecordSearcher.class);
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
            Mockito.when(rawRepoConnector.getSolrZkHost()).thenReturn(SOLR_ZK_HOST);
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

    private HarvestOperation newHarvestOperation(PeriodicJobsHarvesterConfig config) {
        final HarvestOperation harvestOperation = spy(new HarvestOperation(config,
                binaryFileStore,
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                rawRepoConnector));
        doReturn(recordSearcher).when(harvestOperation).createRecordSearcher(SOLR_ZK_HOST);
        return harvestOperation;
    }
}