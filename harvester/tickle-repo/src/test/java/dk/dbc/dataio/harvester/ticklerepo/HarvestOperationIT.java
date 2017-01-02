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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.ticklerepo.TickleRepo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperationIT extends IntegrationTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void populateTickle() {
        executeScriptResource("/tickle-repo.sql");
    }

    @BeforeClass
    public static void setInitialContext() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind("bfs/home", testFolder.toString());

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshotBuilder().build());
    }

    @Test
    public void nonExistingDatasetHarvestOperationThrowsOnCreation() {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent()
                .withDatasetName("non-existing");

        assertThat(() -> createHarvestOperation(config), isThrowing(IllegalStateException.class));
    }

    @Test
    public void recordsHarvested() throws HarvesterException {
        final TickleRepoHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = createHarvestOperation(config);
        assertThat("Number of records harvested", harvestOperation.execute(), is(3));

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_1")
                .withTrackingId("t_3_1")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_2")
                .withTrackingId("t_3_2")
                .withDeleted(true));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(123456)
                .withFormat("test-format")
                .withBibliographicRecordId("id_3_3")
                .withTrackingId("t_3_3")
                .withDeleted(false));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation("data_3_1"));
        addiContentExpectations.add(new Expectation("data_3_2"));
        addiContentExpectations.add(new Expectation("data_3_3"));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);
    }

    private HarvestOperation createHarvestOperation(TickleRepoHarvesterConfig config) {
        return new HarvestOperation(config,
                flowStoreServiceConnector,
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean("bfs/home"),
                fileStoreServiceConnector,
                jobStoreServiceConnector,
                new TickleRepo(entityManager));
    }

    private TickleRepoHarvesterConfig newConfig() {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
        config.getContent()
                .withDatasetName("dataset")
                .withFormat("test-format")
                .withDestination("test-destination")
                .withType(JobSpecification.Type.TEST)
                .withLastBatchHarvested(2);
        return config;
    }
}