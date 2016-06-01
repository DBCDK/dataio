/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final int ushHarvesterJobId = 42;
    private final Date solrTimeOfLastHarvest = new Date(0);
    private final Date ushTimeOfLastHarvest = new Date(1);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_configArgIsNull_throws() {
        new HarvestOperation(null, flowStoreServiceConnector, newHarvesterJobBuilder());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() {
        new HarvestOperation(newUshSolrHarvesterConfig(), null, newHarvesterJobBuilder());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_harvesterJobBuilderArgIsNull_throws() {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, null);
    }

    @Test
    public void execute_redoesAnyUncommitttedConfigUpdatesAndLeavesNoWalBehind() throws HarvesterException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        harvestOperation.execute();

        assertThat(walFileExists(), is(false));
        verify(harvestOperation).redoConfigUpdateIfUncommitted();
    }

    @Test
    public void redoConfigUpdateIfUncommitted_noUncommittedWalEntryExists_returns() throws HarvesterException, JobStoreServiceConnectorException {
        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.redoConfigUpdateIfUncommitted();

        verify(jobStoreServiceConnector, times(0)).listJobs(any(JobListCriteria.class));
    }

    @Test
    public void redoConfigUpdateIfUncommitted_uncommittedWalEntryExistsButNoDataIoJobExists_commitsWal()
            throws HarvesterException, JobStoreServiceConnectorException, IOException, FlowStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.emptyList());
        createWalWithUncommittedEntry();
        assertThat(walFileExists(), is(true));

        final UshSolrHarvesterConfig config = newUshSolrHarvesterConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        harvestOperation.redoConfigUpdateIfUncommitted();
        assertThat("WAL file exists", walFileExists(), is(false));
        assertThat("Config not updated", config.getContent().getTimeOfLastHarvest(), is(solrTimeOfLastHarvest));

        verify(flowStoreServiceConnector, times(0)).updateHarvesterConfig(any(UshSolrHarvesterConfig.class));
    }

    @Test
    public void redoConfigUpdateIfUncommitted_uncommitedWalEntryExistsAndDataIoJobExists_updatesConfigAndCommitsWal()
            throws HarvesterException, JobStoreServiceConnectorException, IOException, FlowStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.singletonList(new JobInfoSnapshotBuilder().build()));
        createWalWithUncommittedEntry();
        assertThat(walFileExists(), is(true));

        final UshSolrHarvesterConfig config = newUshSolrHarvesterConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        harvestOperation.redoConfigUpdateIfUncommitted();
        assertThat("WAL file exists", walFileExists(), is(false));
        assertThat("Config updated", config.getContent().getTimeOfLastHarvest(), is(ushTimeOfLastHarvest));

        verify(flowStoreServiceConnector).updateHarvesterConfig(any(UshSolrHarvesterConfig.class));
    }

    @Test
    public void harvesterTokenExistsInDataIo_harvesterTokenNotFoundInDataIO_returnsFalse()
            throws HarvesterException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.emptyList());
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.harvesterTokenExistsInDataIo("token"), is(false));
    }

    @Test
    public void harvesterTokenExistsInDataIo_harvesterTokenFoundInDataIO_returnsTrue()
            throws HarvesterException, JobStoreServiceConnectorException {
        when(jobStoreServiceConnector.listJobs(any(JobListCriteria.class))).thenReturn(Collections.singletonList(new JobInfoSnapshotBuilder().build()));
        final HarvestOperation harvestOperation = newHarvestOperation();
        assertThat(harvestOperation.harvesterTokenExistsInDataIo("token"), is(true));
    }

    private HarvestOperation newHarvestOperation() {
        return newHarvestOperation(newUshSolrHarvesterConfig());
    }

    private HarvestOperation newHarvestOperation(UshSolrHarvesterConfig config) {
        return new HarvestOperation(config, flowStoreServiceConnector, newHarvesterJobBuilder());
    }

    private HarvesterJobBuilder newHarvesterJobBuilder() {
        try {
            final BinaryFileStore binaryFileStore = new BinaryFileStoreFsImpl(folder.getRoot().toPath());
            return new HarvesterJobBuilder(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, new JobSpecificationBuilder().build());
        } catch (HarvesterException e) {
            throw new IllegalStateException(e);
        }
    }

    private UshSolrHarvesterConfig newUshSolrHarvesterConfig() {
        return new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
            .withUshHarvesterJobId(ushHarvesterJobId)
            .withTimeOfLastHarvest(solrTimeOfLastHarvest)
            .withUshHarvesterProperties(
                    new UshHarvesterProperties()
                        .withLastHarvestedDate(ushTimeOfLastHarvest)));
    }

    private void createWalWithUncommittedEntry() throws HarvesterException {
        final HarvesterWal harvesterWal = new HarvesterWal(newUshSolrHarvesterConfig(), new BinaryFileStoreFsImpl(folder.getRoot().toPath()));
        harvesterWal.write(HarvesterWal.WalEntry.create(1, 1, solrTimeOfLastHarvest, ushTimeOfLastHarvest));
    }

    private boolean walFileExists() {
        return Files.exists(Paths.get(folder.getRoot().getAbsolutePath(), ushHarvesterJobId + ".wal"));
    }
}