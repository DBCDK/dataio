/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.ticklerepo.TickleRepo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViafHarvestOperationIT extends IntegrationTest {
    private final static String DBC_AGENCY = "870979";
    private final static String DBC_RECORD_ID = "ok";
    private final static String DBC_RECORD_ID_NOT_FOUND = "not-found";
    private final static String DBC_RECORD_ID_INVALID = "invalid";

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private Path harvesterTmpFile;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setInitialContext() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupMocks() throws IOException, JobStoreServiceConnectorException,
                                    RecordServiceConnectorException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind("bfs/home", testFolder.toString());

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tmpFolder.newFile().toPath();
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());
        when(recordServiceConnector.recordExists(DBC_AGENCY, DBC_RECORD_ID))
                .thenReturn(true);
        when(recordServiceConnector.getRecordContent(DBC_AGENCY, DBC_RECORD_ID))
                .thenReturn(getMarcXchangeCollectionFor(getDbcMarcXchangeRecord(DBC_RECORD_ID))
                        .getBytes(StandardCharsets.UTF_8));
        when(recordServiceConnector.recordExists(DBC_AGENCY, DBC_RECORD_ID_INVALID))
                .thenReturn(true);
        when(recordServiceConnector.getRecordContent(DBC_AGENCY, DBC_RECORD_ID_INVALID))
                .thenReturn("invalid".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void harvest() {
        final TickleRepoHarvesterConfig config = newConfig();
        config.getContent().withLastBatchHarvested(2);
        final HarvestOperation harvestOperation = createHarvestOperation(config);
        final JpaTestEnvironment ticklerepo = environment.get("ticklerepo");
        final int numberOfRecordsHarvested = ticklerepo.getPersistenceContext().run(harvestOperation::execute);
        assertThat("Number of records harvested", numberOfRecordsHarvested, is(5));

        /*
            viaf_1: has existing DBC ID
            viaf_2: invalid marcXchange
            viaf_3: is DELETED and has no DBC ID
            viaf_4: has non-existing DBC ID
            viaf_5: has existing DBC ID but DBC record is invalid marcXchange
         */

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(424242)
                .withFormat("viaf-format")
                .withBibliographicRecordId("viaf_1")
                .withTrackingId("track_viaf_1")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(424242)
                .withFormat("viaf-format")
                .withBibliographicRecordId("viaf_2")
                .withTrackingId("track_viaf_2")
                .withDeleted(false)
                .withDiagnostic(new Diagnostic(
                        Diagnostic.Level.FATAL, "Content is not allowed in prolog")));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(424242)
                .withFormat("viaf-format")
                .withBibliographicRecordId("viaf_3")
                .withTrackingId("track_viaf_3")
                .withDeleted(true));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(424242)
                .withFormat("viaf-format")
                .withBibliographicRecordId("viaf_4")
                .withTrackingId("track_viaf_4")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(424242)
                .withFormat("viaf-format")
                .withBibliographicRecordId("viaf_5")
                .withTrackingId("track_viaf_5")
                .withDeleted(false)
                .withDiagnostic(new Diagnostic(
                        Diagnostic.Level.FATAL, "Content is not allowed in prolog")));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(getMarcXchangeCollectionFor(
                getViafMarcXchangeRecord("viaf_1", DBC_RECORD_ID),
                getDbcMarcXchangeRecord(DBC_RECORD_ID))));
        addiContentExpectations.add(new Expectation((byte[]) null));
        addiContentExpectations.add(new Expectation(getMarcXchangeCollectionFor(
                getViafMarcXchangeRecord("viaf_3", null))));
        addiContentExpectations.add(new Expectation(getMarcXchangeCollectionFor(
                getViafMarcXchangeRecord("viaf_4", DBC_RECORD_ID_NOT_FOUND))));
        addiContentExpectations.add(new Expectation(getMarcXchangeCollectionFor(
                getViafMarcXchangeRecord("viaf_5", DBC_RECORD_ID_INVALID))));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);
    }

    private HarvestOperation createHarvestOperation(TickleRepoHarvesterConfig config) {
        return new ViafHarvestOperation(config,
                flowStoreServiceConnector,
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean("bfs/home"),
                fileStoreServiceConnector,
                jobStoreServiceConnector,
                new TickleRepo(environment.get("ticklerepo").getEntityManager()),
                new TaskRepo(environment.get("taskrepo").getEntityManager()),
                recordServiceConnector);
    }

    private TickleRepoHarvesterConfig newConfig() {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
        config.getContent()
                .withDatasetName("viaf")
                .withFormat("viaf-format")
                .withDestination("test-destination")
                .withType(JobSpecification.Type.TEST)
                .withLastBatchHarvested(0);
        return config;
    }

    private String getViafMarcXchangeRecord(String viafId, String dbcId) {
        return
                "<record>" +
                    "<leader>321</leader>" +
                    "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + viafId + "</subfield>" +
                    "</datafield>" +
                    (dbcId != null ?
                    "<datafield ind1='0' ind2='0' tag='700'>" +
                        "<subfield code='0'>(DBC)" + DBC_AGENCY + dbcId + "</subfield>" +
                    "</datafield>" : "") +
                "</record>";
    }

    private String getDbcMarcXchangeRecord(String dbcId) {
        return
                "<record>" +
                    "<leader>654</leader>" +
                    "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + DBC_RECORD_ID + "</subfield>" +
                    "</datafield>" +
                "</record>";
    }

    private String getMarcXchangeCollectionFor(String... records) {
        return
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        String.join("", records) +
                "</collection>";
    }
}