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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataContainerExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.XmlExpectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
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

public class HarvestOperation_2fbs_Test {
    private static final Date QUEUED_TIME = new Date(1467277697583L); // 2016-06-30 11:08:17.583
    private static final String CONSUMER_ID = "consumerId";
    private static final int AGENCY_ID = 123456;

    private static final String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private static final RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);

    private static final RecordId FIRST_RECORD_ID = new RecordId("first", AGENCY_ID);
    private static final String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private static final MockedRecord FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID);
    private static final QueueJob FIRST_QUEUE_JOB = HarvestOperationTest.getQueueJob(FIRST_RECORD_ID, QUEUED_TIME);

    private static final RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", AGENCY_ID);
    private static final Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID);

    private static final RecordId SECOND_RECORD_ID = new RecordId("second", AGENCY_ID);
    private static final String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private static final Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID);
    private static final QueueJob SECOND_QUEUE_JOB = HarvestOperationTest.getQueueJob(SECOND_RECORD_ID, QUEUED_TIME);

    private static final RecordId THIRD_RECORD_ID = new RecordId("third", AGENCY_ID);
    private static final String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private static final Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID);
    private static final QueueJob THIRD_QUEUE_JOB = HarvestOperationTest.getQueueJob(THIRD_RECORD_ID, QUEUED_TIME);

    static {
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("trail");
        FIRST_RECORD.setTrackingId("tracking id");
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setTrackingId(null);
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private final EntityManager entityManager = mock(EntityManager.class);

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFile;
    private List<AddiMetaData> recordsAddiMetaDataExpectations;
    private List<XmlExpectation> recordsExpectations;

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

        // Intercept harvester data files with mocked FileStoreServiceConnector
        harvesterDataFile = tmpFolder.newFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFile.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnector
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());

        recordsAddiMetaDataExpectations = new ArrayList<>();
        recordsExpectations = new ArrayList<>();
    }

    @Test
    public void execute_multipleRecordsHarvested_dataFileContainsMarcExchangeCollections()
            throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException, JSONBException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_HEAD_ID.getBibliographicRecordId(), FIRST_RECORD_HEAD);
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(SECOND_RECORD_ID.getBibliographicRecordId(), SECOND_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>() {{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});

        when(RAW_REPO_CONNECTOR.fetchRecord(any(RecordId.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation1.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation1.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        recordsExpectations.add(dataContainerExpectation1);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(FIRST_RECORD.getId().getAgencyId())
                .withFormat("format")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation2 = new DataContainerExpectation();
        dataContainerExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        recordsExpectations.add(dataContainerExpectation2);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getId().getAgencyId())
                .withFormat("format")
                .withCreationDate(SECOND_RECORD.getCreated()));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation3 = new DataContainerExpectation();
        dataContainerExpectation3.dataExpectation = marcExchangeCollectionExpectation3;
        dataContainerExpectation3.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        recordsExpectations.add(dataContainerExpectation3);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(THIRD_RECORD.getId().getAgencyId())
                .withFormat("format")
                .withCreationDate(THIRD_RECORD.getCreated()));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void execute_recordIsInvalid_recordIsFailed()
            throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException, JSONBException {
        final MockedRecord invalidRecord = new MockedRecord(SECOND_RECORD_ID, true);
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>() {{
                    put(FIRST_RECORD_ID.getBibliographicRecordId(), FIRST_RECORD);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(invalidRecord.getId().toString(), invalidRecord);
                }})
                .thenReturn(new HashMap<String, Record>(){{
                    put(THIRD_RECORD_ID.getBibliographicRecordId(), THIRD_RECORD);
                }});

        when(RAW_REPO_CONNECTOR.fetchRecord(any(RecordId.class))).thenReturn(FIRST_RECORD).thenReturn(SECOND_RECORD).thenReturn(THIRD_RECORD);

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation1.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation1.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        recordsExpectations.add(dataContainerExpectation1);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(FIRST_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(FIRST_RECORD.getId().getAgencyId())
                .withFormat("format")
                .withCreationDate(FIRST_RECORD.getCreated())
                .withEnrichmentTrail(FIRST_RECORD.getEnrichmentTrail())
                .withTrackingId(FIRST_RECORD.getTrackingId()));

        final DataContainerExpectation dataContainerExpectation2 = null;
        recordsExpectations.add(dataContainerExpectation2);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(SECOND_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(SECOND_RECORD.getId().getAgencyId())
                .withCreationDate(SECOND_RECORD.getCreated())
                .withEnrichmentTrail(SECOND_RECORD.getEnrichmentTrail())
                .withTrackingId(SECOND_RECORD.getTrackingId())
                .withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, String.format(
                        "Harvesting RawRepo %s failed: Record %s was not found in returned collection",
                        SECOND_RECORD.getId(), SECOND_RECORD.getId()))));

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation3 = new DataContainerExpectation();
        dataContainerExpectation3.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation3.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        recordsExpectations.add(dataContainerExpectation3);
        recordsAddiMetaDataExpectations.add(new AddiMetaData()
                .withBibliographicRecordId(THIRD_RECORD.getId().getBibliographicRecordId())
                .withSubmitterNumber(THIRD_RECORD.getId().getAgencyId())
                .withFormat("format")
                .withCreationDate(THIRD_RECORD.getCreated()));

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute(entityManager);

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private void verifyHarvesterDataFiles() throws ParserConfigurationException, IOException, SAXException, JSONBException {
        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterDataFile, recordsAddiMetaDataExpectations, recordsExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                newHarvestOperation().getJobSpecificationTemplate(AGENCY_ID));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat("JobSpecification.packaging", jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat("JobSpecification.format", jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat("JobSpecification.charset", jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat("JobSpecification.destination", jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat("JobSpecification.submitterId", jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private MarcExchangeRecordExpectation getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecordExpectation(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("yyyyMMdd").format(record.getCreated());
    }

    public String dateToString(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date);
    }

    private HarvestOperation newHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent()
                .withFormat("format")
                .withConsumerId(CONSUMER_ID);
        return new ClassUnderTest(config, harvesterJobBuilderFactory);
    }

    private class ClassUnderTest extends HarvestOperation {
        public ClassUnderTest(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory) {
            super(config, harvesterJobBuilderFactory);
        }
        @Override
        RawRepoConnector getRawRepoConnector(RRHarvesterConfig config) {
            return RAW_REPO_CONNECTOR;
        }
    }
}