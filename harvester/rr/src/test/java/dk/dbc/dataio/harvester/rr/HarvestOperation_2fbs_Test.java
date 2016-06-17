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
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataContainerExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataFileExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.HarvesterXmlDataFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecord;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
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
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestOperation_2fbs_Test {
    private static final String CONSUMER_ID = "consumerId";
    private static final int AGENCY_ID = 123456;

    private static final String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private static final RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);

    private static final RecordId FIRST_RECORD_ID = new RecordId("first", AGENCY_ID);
    private static final String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private static final MockedRecord FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID, true);
    private static final QueueJob FIRST_QUEUE_JOB = HarvestOperationTest.getQueueJob(FIRST_RECORD_ID);

    private static final RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", AGENCY_ID);
    private static final Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID, true);

    private static final RecordId SECOND_RECORD_ID = new RecordId("second", AGENCY_ID);
    private static final String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private static final Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID, true);
    private static final QueueJob SECOND_QUEUE_JOB = HarvestOperationTest.getQueueJob(SECOND_RECORD_ID);

    private static final RecordId THIRD_RECORD_ID = new RecordId("third", AGENCY_ID);
    private static final String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private static final Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID, true);
    private static final QueueJob THIRD_QUEUE_JOB = HarvestOperationTest.getQueueJob(THIRD_RECORD_ID);

    static {
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setEnrichmentTrail("trail");
        FIRST_RECORD.setTrackingId("tracking id");
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setTrackingId(null);
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFile;
    private List<DataFileExpectation> harvesterDataFileExpectations;

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

        harvesterDataFileExpectations = new ArrayList<>();
    }

    @Test
    public void execute_multipleRecordsHarvested_dataFileContainsMarcExchangeCollections()
            throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
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

        // Setup harvester datafile content expectations

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation1.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation1.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        harvesterDataFileExpectations.add(dataContainerExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation2 = new DataContainerExpectation();
        dataContainerExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation2);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation3 = new DataContainerExpectation();
        dataContainerExpectation3.dataExpectation = marcExchangeCollectionExpectation3;
        dataContainerExpectation3.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation3);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void execute_recordIsInvalid_recordIsSkipped()
            throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
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

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        dataContainerExpectation1.supplementaryDataExpectation.put("enrichmentTrail", FIRST_RECORD.getEnrichmentTrail());
        dataContainerExpectation1.supplementaryDataExpectation.put("trackingId", FIRST_RECORD.getTrackingId());
        harvesterDataFileExpectations.add(dataContainerExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation2 = new DataContainerExpectation();
        dataContainerExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation2);

        final HarvestOperation harvestOperation = newHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private void verifyHarvesterDataFiles() throws ParserConfigurationException, IOException, SAXException {
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFile, harvesterDataFileExpectations);
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

    private MarcExchangeRecord getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecord(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("yyyyMMdd").format(record.getCreated());
    }

    private HarvestOperation newHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        final RRHarvesterConfig config = HarvesterTestUtil.getRRHarvesterConfig();
        config.getContent().withConsumerId(CONSUMER_ID);
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