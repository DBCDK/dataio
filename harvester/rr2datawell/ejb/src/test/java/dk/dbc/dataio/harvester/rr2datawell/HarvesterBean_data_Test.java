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
import dk.dbc.rawrepo.MockedQueueJob;
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
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBean_data_Test {
    private final static String BFS_BASE_PATH_JNDI_NAME = "bfs/home";
    private final static RawRepoConnectorBean RAW_REPO_CONNECTOR_BEAN = mock(RawRepoConnectorBean.class);
    private final static QueueJob QUEUE_JOB = new MockedQueueJob("id", 42, "worker", new Timestamp(new Date().getTime()));

    private final static RecordId FIRST_RECORD_ID = new RecordId("first", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String FIRST_RECORD_CONTENT =
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + FIRST_RECORD_ID.getId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">" + FIRST_RECORD_ID.getLibrary() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    private final static Record FIRST_RECORD = new MockedRecord(FIRST_RECORD_ID, true);

    private final static RecordId SECOND_RECORD_ID = new RecordId("second", 123456);
    private final static String SECOND_RECORD_CONTENT =
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + SECOND_RECORD_ID.getId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">" + SECOND_RECORD_ID.getLibrary() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    private final static Record SECOND_RECORD = new MockedRecord(SECOND_RECORD_ID, true);


    private final static RecordId THIRD_RECORD_ID = new RecordId("third", HarvesterBean.LIBRARY_NUMBER_870970);
    private final static String THIRD_RECORD_CONTENT =
        "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
            "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "<marcx:subfield code=\"a\">" + THIRD_RECORD_ID.getId() + "</marcx:subfield>" +
                    "<marcx:subfield code=\"b\">"+ THIRD_RECORD_ID.getLibrary() + "</marcx:subfield>" +
                "</marcx:datafield>" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield>" +
            "</marcx:record>" +
        "</marcx:collection>";
    private final static Record THIRD_RECORD = new MockedRecord(THIRD_RECORD_ID, true);

    static {
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
                .thenReturn(QUEUE_JOB)
                .thenReturn(QUEUE_JOB)
                .thenReturn(QUEUE_JOB)
                .thenReturn(null);
        when(RAW_REPO_CONNECTOR_BEAN.fetchRecord(any(RecordId.class)))
                .thenReturn(FIRST_RECORD)
                .thenReturn(SECOND_RECORD)
                .thenReturn(THIRD_RECORD);

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
        expectation1.records.add(asMarcExchangeRecordExpectation(FIRST_RECORD_ID));
        MarcExchangeCollectionExpectation expectation2 = new MarcExchangeCollectionExpectation();
        expectation2.records.add(asMarcExchangeRecordExpectation(THIRD_RECORD_ID));

        // Verify harvester datafile content
        final HarvesterXmlDataFileVerifier harvesterXmlDataFileVerifier = new HarvesterXmlDataFileVerifier();
        harvesterXmlDataFileVerifier.verify(harvesterDataFileWith870970, Arrays.asList(expectation1, expectation2));

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