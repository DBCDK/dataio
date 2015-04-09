package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.newjobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
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

public class HarvesterBean_2datawell_Test {
    private static final String CONSUMER_ID = "consumerId";
    private static final int COMMUNITY_ID = 870970;
    private static final int LOCAL_ID = 700000;

    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private final static RawRepoConnector RAW_REPO_CONNECTOR = mock(RawRepoConnector.class);

    private final static RecordId FIRST_RECORD_ID = new RecordId("first", COMMUNITY_ID);
    private final static String FIRST_RECORD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_ID);
    private final static Record FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID, true);
    private final static QueueJob FIRST_QUEUE_JOB = HarvestOperationTest.getQueueJob(FIRST_RECORD_ID);

    private final static RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", COMMUNITY_ID);
    private final static String FIRST_RECORD_HEAD_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_HEAD_ID);
    private final static Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID, true);

    private final static RecordId FIRST_RECORD_SECTION_ID = new RecordId("first-section", COMMUNITY_ID);
    private final static String FIRST_RECORD_SECTION_CONTENT = HarvestOperationTest.getRecordContent(FIRST_RECORD_SECTION_ID);
    private final static Record FIRST_RECORD_SECTION = new MockedRecord(FIRST_RECORD_SECTION_ID, true);

    private final static RecordId SECOND_RECORD_ID = new RecordId("second", LOCAL_ID);
    private final static String SECOND_RECORD_CONTENT = HarvestOperationTest.getRecordContent(SECOND_RECORD_ID);
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID, true);
    private final static QueueJob SECOND_QUEUE_JOB = HarvestOperationTest.getQueueJob(SECOND_RECORD_ID);

    private final static RecordId THIRD_RECORD_ID = new RecordId("third", COMMUNITY_ID);
    private final static String THIRD_RECORD_CONTENT = HarvestOperationTest.getRecordContent(THIRD_RECORD_ID);
    private final static Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID, true);
    private final static QueueJob THIRD_QUEUE_JOB = HarvestOperationTest.getQueueJob(THIRD_RECORD_ID);

    static {
        FIRST_RECORD_HEAD.setContent(FIRST_RECORD_HEAD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD_SECTION.setContent(FIRST_RECORD_SECTION_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private MockedJobStoreServiceConnector mockedJobStoreServiceConnector;
    private MockedFileStoreServiceConnector mockedFileStoreServiceConnector;
    private File harvesterDataFileWithCommunityRecords;
    private File harvesterDataFileWithLocalRecords;
    private List<DataFileExpectation> harvesterDataFileWithCommunityRecordsExpectations;
    private List<DataFileExpectation> harvesterDataFileWithLocalRecordsExpectations;

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
        harvesterDataFileWithCommunityRecords = tmpFolder.newFile();
        harvesterDataFileWithLocalRecords = tmpFolder.newFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithCommunityRecords.toPath());
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFileWithLocalRecords.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());
        mockedJobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshotBuilder().build());

        harvesterDataFileWithCommunityRecordsExpectations = new ArrayList<>();
        harvesterDataFileWithLocalRecordsExpectations = new ArrayList<>();
    }

    @Test
    public void harvest_multipleLibraryNumbersHarvested_CommunityAndLocalRecordsInSeparateJobs()
            throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
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

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_HEAD_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_SECTION_ID));
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation communityExpectation1 = new DataContainerExpectation();
        communityExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        communityExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        harvesterDataFileWithCommunityRecordsExpectations.add(communityExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation communityExpectation2 = new DataContainerExpectation();
        communityExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        communityExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileWithCommunityRecordsExpectations.add(communityExpectation2);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation localExpectation1 = new DataContainerExpectation();
        localExpectation1.dataExpectation = marcExchangeCollectionExpectation3;
        localExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        harvesterDataFileWithLocalRecordsExpectations.add(localExpectation1);

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void harvest_recordCollectionContainsInvalidEntry_recordIsSkipped()
            throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
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

        // Setup harvester datafile content expectations
        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation1 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation1.records.add(getMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation localExpectation1 = new DataContainerExpectation();
        localExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        localExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        harvesterDataFileWithLocalRecordsExpectations.add(localExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(getMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation communityExpectation1 = new DataContainerExpectation();
        communityExpectation1.dataExpectation = marcExchangeCollectionExpectation2;
        communityExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileWithCommunityRecordsExpectations.add(communityExpectation1);

        final HarvestOperation harvestOperation = getHarvestOperation();
        harvestOperation.execute();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private HarvestOperation getHarvestOperation() {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(
                BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME), mockedFileStoreServiceConnector, mockedJobStoreServiceConnector);
        final RawRepoHarvesterConfig.Entry config = HarvesterTestUtil.getHarvestOperationConfigEntry();
        config.setConsumerId(CONSUMER_ID);
        config.setFormat("katalog");
        config.setFormatOverride(COMMUNITY_ID, "basis");
        config.setIncludeRelations(true);
        return new ClassUnderTest(config, harvesterJobBuilderFactory);
    }


    private void verifyHarvesterDataFiles() throws ParserConfigurationException, IOException, SAXException {
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFileWithCommunityRecords, harvesterDataFileWithCommunityRecordsExpectations);
        harvesterXmlDataFileVerifier.verify(harvesterDataFileWithLocalRecords, harvesterDataFileWithLocalRecordsExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                getHarvestOperation().getJobSpecificationTemplate(COMMUNITY_ID));
        verifyJobSpecification(mockedJobStoreServiceConnector.jobInputStreams.remove().getJobSpecification(),
                getHarvestOperation().getJobSpecificationTemplate(LOCAL_ID));
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterId(), is(jobSpecificationTemplate.getSubmitterId()));
    }

    private MarcExchangeRecord getMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecord(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("YYYYMMdd").format(record.getCreated());
    }

    private class ClassUnderTest extends HarvestOperation {
        public ClassUnderTest(RawRepoHarvesterConfig.Entry config, HarvesterJobBuilderFactory harvesterJobBuilderFactory) {
            super(config, harvesterJobBuilderFactory);
        }
        RawRepoConnector getRawRepoConnector(String dataSourceName) {
            return RAW_REPO_CONNECTOR;
        }
    }
}