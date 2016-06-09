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
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.utils.ush.UshSolrConnector;
import dk.dbc.dataio.harvester.utils.ush.UshSolrDocument;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final UshSolrConnector ushSolrConnector = mock(UshSolrConnector.class);
    private final UshSolrConnector.ResultSet resultSet = mock(UshSolrConnector.ResultSet.class);

    private BinaryFileStore binaryFileStore;
    private final int ushHarvesterJobId = 42;
    private final Date solrTimeOfLastHarvest = new Date(0);
    private final Date ushTimeOfLastHarvest = new Date(1);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup() {
        binaryFileStore = new BinaryFileStoreFsImpl(folder.getRoot().toPath());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_configArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(null, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), null, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, null, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, binaryFileStore, null, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, null);
    }

    @Test
    public void execute_redoesAnyUncommittedConfigUpdatesAndLeavesNoWalBehind() throws HarvesterException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Collections.singletonList(new UshSolrDocument()).iterator());
        harvestOperation.execute();
        assertThat(walFileExists(), is(false));
        verify(harvestOperation).redoConfigUpdateIfUncommitted();
    }

    @Test
    public void execute_returnsNumberOfRecordsAdded() throws HarvesterException, FlowStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Arrays.asList(new UshSolrDocument(), new UshSolrDocument()).iterator());

        assertThat(harvestOperation.execute(), is(2));
    }

    @Test
    public void execute_updatesHarvesterConfigInFlowStore() throws HarvesterException, FlowStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Collections.singletonList(new UshSolrDocument()).iterator());

        harvestOperation.execute();

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(any(HarvesterConfig.class));
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

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues() throws HarvesterException {
        final JobSpecification expectedJobSpecificationTemplate = new JobSpecificationBuilder()
                .setSubmitterId(424242)
                .setDestination("testbase")
                .setPackaging("xml")
                .setFormat("marc2")
                .setCharset("utf8")
                .setMailForNotificationAboutVerification("placeholder")
                .setMailForNotificationAboutProcessing("placeholder")
                .setResultmailInitials("placeholder")
                .setDataFile("placeholder")
                .setType(JobSpecification.Type.TRANSIENT)
                .setAncestry(new JobSpecification.Ancestry()
                    .withHarvesterToken("ush-solr:1:1:0:1"))
                .build();

        final UshSolrHarvesterConfig ushSolrHarvesterConfig = newUshSolrHarvesterConfig();
        ushSolrHarvesterConfig.getContent()
                .withSubmitterNumber((int) expectedJobSpecificationTemplate.getSubmitterId())
                .withDestination(expectedJobSpecificationTemplate.getDestination())
                .withFormat(expectedJobSpecificationTemplate.getFormat());

        final HarvestOperation harvestOperation = newHarvestOperation(ushSolrHarvesterConfig);
        assertThat(harvestOperation.getJobSpecificationTemplate(), is(expectedJobSpecificationTemplate));
    }

    private HarvestOperation newHarvestOperation() throws HarvesterException {
        return newHarvestOperation(newUshSolrHarvesterConfig());
    }

    private HarvestOperation newHarvestOperation(UshSolrHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = new HarvestOperation(config, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
        harvestOperation.ushSolrConnector = ushSolrConnector;
        when(ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(anyString(), any(Date.class), any(Date.class))).thenReturn(resultSet);

        return harvestOperation;
    }

    private UshSolrHarvesterConfig newUshSolrHarvesterConfig() {
        return new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
            .withUshHarvesterJobId(ushHarvesterJobId)
            .withTimeOfLastHarvest(solrTimeOfLastHarvest)
                .withFormat("format")
                .withDestination("destination")
                .withSubmitterNumber(42)
                .withUshHarvesterProperties(
                    new UshHarvesterProperties()
                        .withLastHarvestFinishedDate(ushTimeOfLastHarvest)
                .withStorageUrl("url")));
    }

    private void createWalWithUncommittedEntry() throws HarvesterException {
        final HarvesterWal harvesterWal = new HarvesterWal(newUshSolrHarvesterConfig(), new BinaryFileStoreFsImpl(folder.getRoot().toPath()));
        harvesterWal.write(HarvesterWal.WalEntry.create(1, 1, solrTimeOfLastHarvest, ushTimeOfLastHarvest));
    }

    private boolean walFileExists() {
        return Files.exists(Paths.get(folder.getRoot().getAbsolutePath(), ushHarvesterJobId + ".wal"));
    }
}