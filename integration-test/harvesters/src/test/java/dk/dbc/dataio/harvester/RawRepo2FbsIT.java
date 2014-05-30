package dk.dbc.dataio.harvester;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.opencataloging.update.rawrepo.RawRepoConnector;
import dk.dbc.opencataloging.update.ws.commons.utils.dom.DomUtil;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RawRepo2FbsIT {
    private static final String RAWREPO_PROVIDER = "opencataloging-update";
    private static final String JOB_DESTINATION = "fbs";
    private static final String QUEUE_NAME = "fbs-sync";
    private static final String MARC_EXCHANGE_NAMESPACE = "info:lc/xmlns/marcxchange-v1";
    private final String COLLECTION_ELEMENT_NAME = "collection";
    private final String RECORD_ELEMENT_NAME = "record";
    private static Client client;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws SQLException, ClassNotFoundException {
        RawRepoUtil.setupRawRepo();
        client = HttpClient.newClient(new ClientConfig()
                .register(new Jackson2xFeature()));
    }

    @After
    public void clearRawRepo() throws SQLException, ClassNotFoundException {
        RawRepoUtil.clearRawRepo();
    }

    @After
    public void clearFileStore() {
        ITUtil.clearFileStore();
    }

    /**
     * Given: a rawrepo with two records queued <br/>
     * When: a scheduled harvest is executed <br/>
     * Then: the rawrepo queue is emptied <br/>
     * And: a new job is created in the job-store <br/>
     * And: the data file referenced by this job contains two marc
     * exchange collections each with one record <br/>
     */
    @Test
    public void harvest_ok() throws Exception {
        // Given...

        populateRawRepoWithRecordsFromResources(
                "/marcxchange/marcXchange-test-record-wrapped-in-collection-utf8.xml",
                "/marcxchange/marcXchange-test-record-wrapped-in-collection-utf8-014-relation.xml");

        // When...

        // Then...

        RawRepoUtil.awaitQueueSize(QUEUE_NAME, 0, 35000);

        // And...

        final Document dataFileAsDocument = assertJobCreated();

        // And...

        final Element documentElement = dataFileAsDocument.getDocumentElement();
        final NodeList childNodes = documentElement.getChildNodes();
        assertThat(childNodes, is(notNullValue()));
        assertThat(childNodes.getLength(), is(2));
        for (int i = 0; i < childNodes.getLength(); i++) {
            assertMarcExchangeCollection(childNodes.item(i), 1);
        }
    }

    private static void populateRawRepoWithRecordsFromResources(String... resourceNames)
            throws SQLException, ClassNotFoundException, IOException, SAXException, ParserConfigurationException, TransformerException {
        try (final Connection connection = RawRepoUtil.newRawRepoConnection()) {
            final RawRepoConnector rawRepoConnector = new RawRepoConnector();
            for (final String resource : resourceNames) {
                rawRepoConnector.addRecord(connection, RAWREPO_PROVIDER,
                        RawRepoUtil.createRecordBinding(getDocumentElementFromResource(resource)));
            }
        }
    }

    private static Element getDocumentElementFromResource(String resourceName) throws ParserConfigurationException, IOException, SAXException {
        return DomUtil.getDocumentElementFromInputStream(
                RawRepoUtil.class.getResourceAsStream(resourceName));
    }

    private Document assertJobCreated() throws URISyntaxException, FileStoreServiceConnectorException, IOException, ParserConfigurationException, SAXException {
        final JobInfo mostRecentJob = getMostRecentJob();
        assertThat(mostRecentJob.getJobSpecification().getDestination(), is(JOB_DESTINATION));
        final FileStoreUrn fileStoreUrn = new FileStoreUrn(mostRecentJob.getJobSpecification().getDataFile());
        return parseDataFileAsXml(getDataFileFromFileStore(fileStoreUrn.getFileId()));
    }

    private JobInfo getMostRecentJob() {
        // ToDo; add getJobs() capability to job-store connector
        final Response response = HttpClient.doGet(client, ITUtil.JOB_STORE_BASE_URL, JobStoreServiceConstants.JOB_COLLECTION);
        final List<JobInfo> jobInfos = response.readEntity(new GenericType<List<JobInfo>>() { });
        return jobInfos.get(jobInfos.size() - 1);
    }

    private File getDataFileFromFileStore(String fileId) throws FileStoreServiceConnectorException, IOException {
        final FileStoreServiceConnector fileStoreServiceConnector = new FileStoreServiceConnector(client, ITUtil.FILE_STORE_BASE_URL);
        final File dataFile = testFolder.newFile();
        try (final InputStream data = fileStoreServiceConnector.getFile(fileId)) {
            IOUtils.copy(data, new FileOutputStream(dataFile));
        }
        return dataFile;
    }

    private Document parseDataFileAsXml(File dataFile) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(dataFile);
    }

    private void assertMarcExchangeCollection(Node marcExchangeCollectionNode, int expectedRecordCount) {
        assertThat(marcExchangeCollectionNode.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(marcExchangeCollectionNode.getLocalName(), is(COLLECTION_ELEMENT_NAME));
        assertThat(marcExchangeCollectionNode.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));
        final NodeList recordNodes = marcExchangeCollectionNode.getChildNodes();
        assertThat(recordNodes, is(notNullValue()));
        assertThat(recordNodes.getLength(), is(expectedRecordCount));
        for (int i = 0; i < recordNodes.getLength(); i++) {
            final Node recordNode = recordNodes.item(i);
            assertThat(recordNode.getNodeType(), is(Node.ELEMENT_NODE));
            assertThat(recordNode.getLocalName(), is(RECORD_ELEMENT_NAME));
            assertThat(recordNode.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));
        }
    }
}
