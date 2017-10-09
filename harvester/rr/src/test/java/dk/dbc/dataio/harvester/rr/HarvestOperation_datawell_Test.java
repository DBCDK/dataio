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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.XmlExpectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_datawell_Test {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int LOCAL_LIBRARY = 700000;

    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private final static RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);
    private final static AgencyConnection AGENCY_CONNECTION = mock(AgencyConnection.class);


    /* 1st record is a DBC record */
    private final static RecordId FIRST_RECORD_ID = new RecordId("first", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private final static MockedRecord FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID);
    private final static MockedRecord FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL = new MockedRecord(FIRST_RECORD_ID);
    private final static QueueJob FIRST_QUEUE_JOB = HarvestOperationTest.getQueueJob(FIRST_RECORD_ID, QUEUED_TIME);

    private final static RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_HEAD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_HEAD_ID);
    private final static Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID);

    private final static RecordId FIRST_RECORD_SECTION_ID = new RecordId("first-section", HarvestOperation.DBC_LIBRARY);
    private final static String FIRST_RECORD_SECTION_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_SECTION_ID);
    private final static Record FIRST_RECORD_SECTION = new MockedRecord(FIRST_RECORD_SECTION_ID);

    /* 2nd record is a local record */
    private final static RecordId SECOND_RECORD_ID = new RecordId("second", LOCAL_LIBRARY);
    private final static String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID);
    private final static QueueJob SECOND_QUEUE_JOB = HarvestOperationTest.getQueueJob(SECOND_RECORD_ID, QUEUED_TIME);

    /* 3rd record is a DBC record */
    private final static RecordId THIRD_RECORD_ID = new RecordId("third", HarvestOperation.DBC_LIBRARY);
    private final static String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private final static MockedRecord THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID);
    private final static MockedRecord THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL = new MockedRecord(THIRD_RECORD_ID);
    private final static QueueJob THIRD_QUEUE_JOB = HarvestOperationTest.getQueueJob(THIRD_RECORD_ID, QUEUED_TIME);

    static {
        FIRST_RECORD_HEAD.setContent(FIRST_RECORD_HEAD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD_SECTION.setContent(FIRST_RECORD_SECTION_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("191919,870970");
        FIRST_RECORD.setTrackingId("tracking id");
        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setTrackingId("tracking id");
        FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL.setCreated(FIRST_RECORD.getCreated());
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setEnrichmentTrail("191919,870970");
        THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL.setCreated(THIRD_RECORD.getCreated());
    }

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFileWithDbcRecords;
    private File harvesterDataFileWithLocalRecords;
    private List<AddiMetaData> dbcRecordsAddiMetaDataExpectations;
    private List<AddiMetaData> localRecordsAddiMetaDataExpectations;
    private List<XmlExpectation> dbcRecordsExpectations;
    private List<XmlExpectation> localRecordsExpectations;

    private AddiMetaData.LibraryRules localLibraryRules = new AddiMetaData.LibraryRules()
            .withLibraryRule("rule1", true);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupMocks() throws SQLException, IOException, RawRepoException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, testFolder.toString());

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.dequeue(CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_JOB)
                .thenReturn(SECOND_QUEUE_JOB)
                .thenReturn(THIRD_QUEUE_JOB)
                .thenReturn(null);

        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterDataFileWithDbcRecords = tmpFolder.newFile();
        harvesterDataFileWithLocalRecords = tmpFolder.newFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithDbcRecords.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithLocalRecords.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        dbcRecordsAddiMetaDataExpectations = new ArrayList<>();
        localRecordsAddiMetaDataExpectations = new ArrayList<>();
        dbcRecordsExpectations = new ArrayList<>();
        localRecordsExpectations = new ArrayList<>();

        // mock OpenAgency calls for non-DBC libraries
        when(AGENCY_CONNECTION.getLibraryRules(LOCAL_LIBRARY, null)).thenReturn(localLibraryRules);
    }

    @Test
    public void harvest_multipleAgencyIdsHarvested_agencyIdsInSeparateJobs()
            throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException,
                   ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException, JSONBException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
                    put(FIRST_RECORD_SECTION_ID.getBibliographicRecordId(), FIRST_RECORD_SECTION);
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});

        when(RAW_REPO_CONNECTOR.fetchRecord(any(RecordId.class)))
                .thenReturn(FIRST_RECORD_WITHOUT_ENRICHMENT_TRAIL)
                .thenReturn(SECOND_RECORD)
                .thenReturn(THIRD_RECORD_WITHOUT_ENRICHMENT_TRAIL);

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_HEAD_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_SECTION_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation1);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation2);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(THIRD_RECORD.getCreated())
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        localRecordsExpectations.add(marcExchangeCollectionExpectation3);
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(SECOND_RECORD.getCreated())
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void harvest_recordCollectionContainsInvalidEntry_recordIsFailed()
            throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException, JSONBException {
        final MockedRecord invalidRecord = new MockedRecord(FIRST_RECORD_HEAD_ID, true);
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                    put("INVALID_RECORD_ID", invalidRecord);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});
        when(RAW_REPO_CONNECTOR.fetchRecord(any(RecordId.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        dbcRecordsExpectations.add(null);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId())
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format(
                        "Harvesting RawRepo %s failed: member data can not be parsed as marcXchange", FIRST_RECORD.getId())))
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        localRecordsExpectations.add(marcExchangeCollectionExpectation1);
        localRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getId().getAgencyId())
                .withFormat("katalog")
                .withCreationDate(SECOND_RECORD.getCreated())
                .withDeleted(false)
                .withLibraryRules(localLibraryRules));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        dbcRecordsExpectations.add(marcExchangeCollectionExpectation2);
        dbcRecordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withFormat("katalog")
                .withCreationDate(THIRD_RECORD.getCreated())
                .withEnrichmentTrail(THIRD_RECORD.getEnrichmentTrail())
                .withDeleted(false)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private HarvestOperation newHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent()
            .withConsumerId(CONSUMER_ID)
            .withFormat("katalog")
            .withFormatOverridesEntry(HarvestOperation.DBC_LIBRARY, "basis")
            .withIncludeRelations(true)
            .withIncludeLibraryRules(true);
        return new HarvestOperation(config, harvesterJobBuilderFactory, taskRepo, AGENCY_CONNECTION, RAW_REPO_CONNECTOR);
    }

    private void verifyHarvesterDataFiles() throws ParserConfigurationException, IOException, SAXException, JSONBException {
        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterDataFileWithDbcRecords, dbcRecordsAddiMetaDataExpectations, dbcRecordsExpectations);
        addiFileVerifier.verify(harvesterDataFileWithLocalRecords, localRecordsAddiMetaDataExpectations, localRecordsExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newHarvestOperation().getJobSpecificationTemplate(870970));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newHarvestOperation().getJobSpecificationTemplate(LOCAL_LIBRARY));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("yyyyMMdd").format(record.getCreated());
    }
}