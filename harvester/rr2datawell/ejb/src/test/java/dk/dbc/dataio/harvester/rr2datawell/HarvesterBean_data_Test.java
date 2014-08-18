package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBeanTestUtil;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.MockedJobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.MockedFileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.datafileverifier.HarvesterXmlDataFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
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
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBean_data_Test {
    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private final static RawRepoConnectorBean RAW_REPO_CONNECTOR_BEAN = mock(RawRepoConnectorBean.class);

    private final static RecordId FIRST_RECORD_ID = new RecordId("first", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String FIRST_RECORD_CONTENT = HarvesterBeanTest.asRcordContent(FIRST_RECORD_ID);
    private final static Record FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID, true);
    private final static QueueJob FIRST_QUEUE_JOB = HarvesterBeanTest.asQueueJob(FIRST_RECORD_ID);

    private final static RecordId FIRST_RECORD_HEAD_ID = new RecordId("first-head", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String FIRST_RECORD_HEAD_CONTENT = HarvesterBeanTest.asRcordContent(FIRST_RECORD_HEAD_ID);
    private final static Record FIRST_RECORD_HEAD = new MockedRecord(FIRST_RECORD_HEAD_ID, true);

    private final static RecordId FIRST_RECORD_SECTION_ID = new RecordId("first-section", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String FIRST_RECORD_SECTION_CONTENT = HarvesterBeanTest.asRcordContent(FIRST_RECORD_SECTION_ID);
    private final static Record FIRST_RECORD_SECTION = new MockedRecord(FIRST_RECORD_SECTION_ID, true);

    private final static RecordId SECOND_RECORD_ID = new RecordId("second", 123456);
    private final static String SECOND_RECORD_CONTENT = HarvesterBeanTest.asRcordContent(SECOND_RECORD_ID);
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID, true);
    private final static QueueJob SECOND_QUEUE_JOB = HarvesterBeanTest.asQueueJob(SECOND_RECORD_ID);

    private final static RecordId THIRD_RECORD_ID = new RecordId("third", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String THIRD_RECORD_CONTENT = HarvesterBeanTest.asRcordContent(THIRD_RECORD_ID);
    private final static Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID, true);
    private final static QueueJob THIRD_QUEUE_JOB = HarvesterBeanTest.asQueueJob(THIRD_RECORD_ID);

    static {
        FIRST_RECORD_HEAD.setContent(FIRST_RECORD_HEAD_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD_SECTION.setContent(FIRST_RECORD_SECTION_CONTENT.getBytes(StandardCharsets.UTF_8));
        FIRST_RECORD.setContent(FIRST_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        SECOND_RECORD.setContent(SECOND_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
        THIRD_RECORD.setContent(THIRD_RECORD_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Test
    public void harvest_multipleLibraryNumbersHarvested_allSkippedExcept870970Records() throws IOException, HarvesterException, SQLException, JobStoreServiceConnectorException, ParserConfigurationException, SAXException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, testFolder.toString());

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR_BEAN.dequeue(HarvesterBean.RAW_REPO_CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_JOB)
                .thenReturn(SECOND_QUEUE_JOB)
                .thenReturn(THIRD_QUEUE_JOB)
                .thenReturn(null);
        when(RAW_REPO_CONNECTOR_BEAN.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashSet<>(Arrays.asList(FIRST_RECORD_HEAD, FIRST_RECORD_SECTION, FIRST_RECORD)))
                .thenReturn(new HashSet<>(Arrays.asList(SECOND_RECORD)))
                .thenReturn(new HashSet<>(Arrays.asList(THIRD_RECORD)));

        // Intercept harvester data file with mocked FileStoreServiceConnectorBean
        final File harvesterDataFileWith870970 = tmpFolder.newFile();
        final MockedFileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean =
                new MockedFileStoreServiceConnectorBean();
        mockedFileStoreServiceConnectorBean.destinations.add(harvesterDataFileWith870970.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        final MockedJobStoreServiceConnectorBean mockedJobStoreServiceConnectorBean =
                new MockedJobStoreServiceConnectorBean();
        mockedJobStoreServiceConnectorBean.jobInfos.add(new JobInfoBuilder().build());

        // Execute harvest
        getHarvesterBean(mockedFileStoreServiceConnectorBean, mockedJobStoreServiceConnectorBean).harvest();

        // Setup harvester datafile content expectations
        MarcExchangeCollectionExpectation expectation1 = new MarcExchangeCollectionExpectation();
        expectation1.records.add(asMarcExchangeRecordExpectation(FIRST_RECORD_HEAD_ID));
        expectation1.records.add(asMarcExchangeRecordExpectation(FIRST_RECORD_SECTION_ID));
        expectation1.records.add(asMarcExchangeRecordExpectation(FIRST_RECORD_ID));
        MarcExchangeCollectionExpectation expectation2 = new MarcExchangeCollectionExpectation();
        expectation2.records.add(asMarcExchangeRecordExpectation(THIRD_RECORD_ID));

        // Verify harvester datafile content
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFileWith870970, Arrays.asList(expectation1, expectation2));

        verifyJobSpecification(mockedJobStoreServiceConnectorBean.jobSpecifications.remove());
    }

    @Test
    public void harvest_recordCollectionContainsInvalidEntry_recordIsSkipped() throws IOException, SQLException, HarvesterException, ParserConfigurationException, SAXException {
        // Enable JNDI lookup of base path for BinaryFileStoreBean
        final File testFolder = tmpFolder.newFolder();
        InMemoryInitialContextFactory.bind(BFS_BASE_PATH_JNDI_NAME, testFolder.toString());

        final MockedRecord invalidRecord = new MockedRecord(FIRST_RECORD_HEAD_ID, true);
        invalidRecord.setContent("not xml".getBytes(StandardCharsets.UTF_8));

        // Mock rawrepo return values
        when(RAW_REPO_CONNECTOR_BEAN.dequeue(HarvesterBean.RAW_REPO_CONSUMER_ID))
                .thenReturn(FIRST_QUEUE_JOB)
                .thenReturn(SECOND_QUEUE_JOB)
                .thenReturn(THIRD_QUEUE_JOB)
                .thenReturn(null);
        when(RAW_REPO_CONNECTOR_BEAN.fetchRecordCollection(any(RecordId.class)))
                .thenReturn(new HashSet<>(Arrays.asList(FIRST_RECORD, invalidRecord)))
                .thenReturn(new HashSet<>(Arrays.asList(SECOND_RECORD)))
                .thenReturn(new HashSet<>(Arrays.asList(THIRD_RECORD)));

        // Intercept harvester data file with mocked FileStoreServiceConnectorBean
        final File harvesterDataFileWith870970 = tmpFolder.newFile();
        final MockedFileStoreServiceConnectorBean mockedFileStoreServiceConnectorBean =
                new MockedFileStoreServiceConnectorBean();
        mockedFileStoreServiceConnectorBean.destinations.add(harvesterDataFileWith870970.toPath());

        // Intercept harvester job specifications with mocked JobStoreServiceConnectorBean
        final MockedJobStoreServiceConnectorBean mockedJobStoreServiceConnectorBean =
                new MockedJobStoreServiceConnectorBean();
        mockedJobStoreServiceConnectorBean.jobInfos.add(new JobInfoBuilder().build());

        // Execute harvest
        getHarvesterBean(mockedFileStoreServiceConnectorBean, mockedJobStoreServiceConnectorBean).harvest();

        // Setup harvester datafile content expectations
        MarcExchangeCollectionExpectation expectation1 = new MarcExchangeCollectionExpectation();
        expectation1.records.add(asMarcExchangeRecordExpectation(THIRD_RECORD_ID));

        // Verify harvester datafile content
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFileWith870970, Arrays.asList(expectation1));

        verifyJobSpecification(mockedJobStoreServiceConnectorBean.jobSpecifications.remove());
    }

    private HarvesterBean getHarvesterBean(FileStoreServiceConnectorBean fileStoreServiceConnectorBean,
            JobStoreServiceConnectorBean jobStoreServiceConnectorBean) {
        final HarvesterBean harvesterBean = new HarvesterBean();
        harvesterBean.init();
        harvesterBean.binaryFileStore = BinaryFileStoreBeanTestUtil.getBinaryFileStoreBean(BFS_BASE_PATH_JNDI_NAME);
        harvesterBean.fileStoreServiceConnector = fileStoreServiceConnectorBean;
        harvesterBean.jobStoreServiceConnector = jobStoreServiceConnectorBean;
        harvesterBean.rawRepoConnector = RAW_REPO_CONNECTOR_BEAN;
        return harvesterBean;
    }

    private void verifyJobSpecification(JobSpecification jobSpecification) {
        assertThat(jobSpecification.getPackaging(), is(HarvesterBean.PACKAGING));
        assertThat(jobSpecification.getFormat(), is(HarvesterBean.FORMAT));
        assertThat(jobSpecification.getCharset(), is(HarvesterBean.CHARSET));
        assertThat(jobSpecification.getDestination(), is(HarvesterBean.DESTINATION));
        assertThat(jobSpecification.getSubmitterId(), is((long)HarvesterBean.LIBRARY_NUMBER_870970));
    }

    private MarcExchangeRecordExpectation asMarcExchangeRecordExpectation(RecordId recordId) {
        return new MarcExchangeRecordExpectation(recordId.getId(), recordId.getLibrary());
    }
}