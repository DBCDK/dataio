package dk.dbc.dataio.harvester.rr2fbs;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataContainerExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.DataFileExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.HarvesterXmlDataFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecord;
import dk.dbc.dataio.harvester.utils.jobstore.HarvesterJobBuilderFactoryBean;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
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

import javax.ejb.SessionContext;
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

public class HarvesterBean_data_Test {
    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private final static RawRepoConnectorBean RAW_REPO_CONNECTOR_BEAN = mock(RawRepoConnectorBean.class);
    private final static SessionContext SESSION_CONTEXT = mock(SessionContext.class);

    private final static RecordId FIRST_RECORD_ID = new RecordId("first",
            (int) HarvesterBean.JOB_SPECIFICATION_TEMPLATE.getSubmitterNumber());
    private final static String FIRST_RECORD_CONTENT = HarvesterBeanTest.asRecordContent(FIRST_RECORD_ID);
    private final static Record FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID, true);
    private final static QueueJob FIRST_QUEUE_JOB = HarvesterBeanTest.asQueueJob(FIRST_RECORD_ID);

    private final static RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head",
            (int) HarvesterBean.JOB_SPECIFICATION_TEMPLATE.getSubmitterNumber());
    private final static Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID, true);

    private final static RecordId SECOND_RECORD_ID = new RecordId("second",
            (int) HarvesterBean.JOB_SPECIFICATION_TEMPLATE.getSubmitterNumber());
    private final static String SECOND_RECORD_CONTENT = HarvesterBeanTest.asRecordContent(SECOND_RECORD_ID);
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID, true);
    private final static QueueJob SECOND_QUEUE_JOB = HarvesterBeanTest.asQueueJob(SECOND_RECORD_ID);

    private final static RecordId THIRD_RECORD_ID = new RecordId("third",
            (int) HarvesterBean.JOB_SPECIFICATION_TEMPLATE.getSubmitterNumber());
    private final static String THIRD_RECORD_CONTENT = HarvesterBeanTest.asRecordContent(THIRD_RECORD_ID);
    private final static Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID, true);
    private final static QueueJob THIRD_QUEUE_JOB = HarvesterBeanTest.asQueueJob(THIRD_RECORD_ID);

    static {
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private FileStoreServiceConnectorBean fileStoreServiceConnectorBean = mock(FileStoreServiceConnectorBean.class);
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
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
        when(RAW_REPO_CONNECTOR_BEAN.dequeue(HarvesterBean.RAW_REPO_CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_JOB)
                .thenReturn(SECOND_QUEUE_JOB)
                .thenReturn(THIRD_QUEUE_JOB)
                .thenReturn(null);

        // Intercept harvester data files with mocked FileStoreServiceConnector
        harvesterDataFile = tmpFolder.newFile();
        mockedFileStoreServiceConnector = new MockedFileStoreServiceConnector();
        mockedFileStoreServiceConnector.destinations.add(harvesterDataFile.toPath());
        when(fileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);

        // Intercept harvester job specifications with mocked JobStoreServiceConnector
        mockedJobStoreServiceConnector = new MockedJobStoreServiceConnector();
        mockedJobStoreServiceConnector.jobInfos.add(new JobInfoBuilder().build());
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(mockedJobStoreServiceConnector);

        harvesterDataFileExpectations = new ArrayList<>();
    }

    @Test
    public void harvest_multipleRecordsHarvested_dataFileContainsMarcExchangeCollections()
            throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR_BEAN.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
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
        marcExchangeCollectionExpectation1.records.add(asMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(asMarcExchangeRecord(SECOND_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation2 = new DataContainerExpectation();
        dataContainerExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(SECOND_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation2);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation3 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation3.records.add(asMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation3 = new DataContainerExpectation();
        dataContainerExpectation3.dataExpectation = marcExchangeCollectionExpectation3;
        dataContainerExpectation3.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation3);

        // Execute harvest
        getHarvesterBean().harvest();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    @Test
    public void harvest_recordIsInvalid_recordIsSkipped()
            throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException, RawRepoException, MarcXMergerException {
        final MockedRecord invalidRecord = new MockedRecord(SECOND_RECORD_ID, true);
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR_BEAN.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashMap<String, Record>(){{
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
        marcExchangeCollectionExpectation1.records.add(asMarcExchangeRecord(FIRST_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation1 = new DataContainerExpectation();
        dataContainerExpectation1.dataExpectation = marcExchangeCollectionExpectation1;
        dataContainerExpectation1.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(FIRST_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation1);

        final MarcExchangeCollectionExpectation marcExchangeCollectionExpectation2 = new MarcExchangeCollectionExpectation();
        marcExchangeCollectionExpectation2.records.add(asMarcExchangeRecord(THIRD_RECORD_ID));
        final DataContainerExpectation dataContainerExpectation2 = new DataContainerExpectation();
        dataContainerExpectation2.dataExpectation = marcExchangeCollectionExpectation2;
        dataContainerExpectation2.supplementaryDataExpectation.put("creationDate", getRecordCreationDate(THIRD_RECORD));
        harvesterDataFileExpectations.add(dataContainerExpectation2);

        // Execute harvest
        getHarvesterBean().harvest();

        verifyHarvesterDataFiles();
        verifyJobSpecifications();
    }

    private HarvesterBean getHarvesterBean() {
        final HarvesterJobBuilderFactoryBean harvesterJobBuilderFactoryBean = new HarvesterJobBuilderFactoryBean();
        harvesterJobBuilderFactoryBean.binaryFileStore = BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME);
        harvesterJobBuilderFactoryBean.fileStoreServiceConnectorBean = fileStoreServiceConnectorBean;
        harvesterJobBuilderFactoryBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        final HarvesterBean harvesterBean = new HarvesterBean();
        harvesterBean.init();
        harvesterBean.harvesterJobBuilderFactoryBean = harvesterJobBuilderFactoryBean;
        harvesterBean.rawRepoConnector = RAW_REPO_CONNECTOR_BEAN;
        harvesterBean.sessionContext = SESSION_CONTEXT;
        when(SESSION_CONTEXT.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        return harvesterBean;
    }

    private void verifyHarvesterDataFiles() throws ParserConfigurationException, IOException, SAXException {
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFile, harvesterDataFileExpectations);
    }

    private void verifyJobSpecifications() {
        verifyJobSpecification(mockedJobStoreServiceConnector.jobSpecifications.remove(),
                HarvesterBean.JOB_SPECIFICATION_TEMPLATE);
    }

    private void verifyJobSpecification(JobSpecification jobSpecification, JobSpecification jobSpecificationTemplate) {
        assertThat(jobSpecification.getPackaging(), is(jobSpecificationTemplate.getPackaging()));
        assertThat(jobSpecification.getFormat(), is(jobSpecificationTemplate.getFormat()));
        assertThat(jobSpecification.getCharset(), is(jobSpecificationTemplate.getCharset()));
        assertThat(jobSpecification.getDestination(), is(jobSpecificationTemplate.getDestination()));
        assertThat(jobSpecification.getSubmitterNumber(), is(jobSpecificationTemplate.getSubmitterNumber()));
    }

    private MarcExchangeRecord asMarcExchangeRecord(RecordId recordId) {
        return new MarcExchangeRecord(recordId.getBibliographicRecordId(), recordId.getAgencyId());
    }

    private String getRecordCreationDate(Record record) {
        return new SimpleDateFormat("YYYYMMdd").format(record.getCreated());
    }
}