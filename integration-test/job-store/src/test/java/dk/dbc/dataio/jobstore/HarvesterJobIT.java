package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SinkContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.SubmitterContentJsonBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.rawrepo.HarvesterException;
import dk.dbc.dataio.harvester.rawrepo.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.rawrepo.MarcExchangeCollection;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.client.Client;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HarvesterJobIT extends AbstractJobStoreTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    /**
     * Given: an empty job-store <br/>
     * When: a job specification is posted, which references a harvester data file
     * containing a number of records exceeding the capacity of a single chunk <br/>
     * Then: a new job is created without error <br/>
     * And: the proper number of chunks is created <br/>
     */
    @Test
    public void createJob_jobSpecificationReferencesHarvesterXmlDataFile_newJobIsCreated()
            throws IOException, HarvesterException, FileStoreServiceConnectorException, URISyntaxException, JobStoreServiceConnectorException {

        // When...

        final int recordCount = 15;
        final String fileId = createMarcExchangeHarvesterDataFile(restClient, tmpFolder.newFile(), recordCount);
        final JobSpecification jobSpecification = getJobSpecification(restClient, fileId);
        final JobInfo jobInfo = createJob(restClient, jobSpecification);

        // Then...

        assertThat(jobInfo.getJobErrorCode(), is(JobErrorCode.NO_ERROR));

        // And...

        final Chunk chunk1 = getChunk(restClient, jobInfo.getJobId(), 1L);
        assertThat(chunk1.getItems().size(), is(Constants.CHUNK_RECORD_COUNT_UPPER_BOUND));
        final Chunk chunk2 = getChunk(restClient, jobInfo.getJobId(), 2L);
        assertThat(chunk2.getItems().size(), is(recordCount - Constants.CHUNK_RECORD_COUNT_UPPER_BOUND));
    }

    private static String createMarcExchangeHarvesterDataFile(Client client, File datafile, int numberOfRecords)
            throws IOException, HarvesterException, FileStoreServiceConnectorException {
        try (final OutputStream os = new FileOutputStream(datafile)) {
            try (final HarvesterXmlDataFile harvesterXmlDataFile = new HarvesterXmlDataFile(StandardCharsets.UTF_8, os)) {
                while (numberOfRecords-- > 0) {
                    final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection();
                    marcExchangeCollection.addMember(
                            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\"/>".getBytes(StandardCharsets.UTF_8)
                    );
                    harvesterXmlDataFile.addRecord(marcExchangeCollection);
                }
            }
        }
        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);
        try (final InputStream is = new FileInputStream(datafile)) {
            return fileStoreServiceConnector.addFile(is);
        }
    }

    private static JobSpecification getJobSpecification(Client restClient, String fileId) throws URISyntaxException {
        final String packaging = "xml";
        final String format = "katalog";
        final String charset = "utf8";
        final String destination = "fbs";
        final long submitterNumber = 42;
        final long flowId = ITUtil.createFlow(restClient, ITUtil.FLOW_STORE_BASE_URL, new FlowContentJsonBuilder().build());
        final long sinkId = ITUtil.createSink(restClient, ITUtil.FLOW_STORE_BASE_URL, new SinkContentJsonBuilder().build());
        final long submitterId = ITUtil.createSubmitter(restClient, ITUtil.FLOW_STORE_BASE_URL,
                new SubmitterContentJsonBuilder()
                        .setNumber(submitterNumber)
                        .build());
        ITUtil.createFlowBinder(restClient, ITUtil.FLOW_STORE_BASE_URL,
                new FlowBinderContentJsonBuilder()
                        .setPackaging(packaging)
                        .setFormat(format)
                        .setCharset(charset)
                        .setDestination(destination)
                        .setRecordSplitter("DefaultXMLRecordSplitter")
                        .setFlowId(flowId)
                        .setSinkId(sinkId)
                        .setSubmitterIds(Arrays.asList(submitterId))
                        .build());

        return new JobSpecification(packaging, format, charset, destination, submitterNumber, "", "", "",
                FileStoreUrn.create(fileId).toString());
    }
}
