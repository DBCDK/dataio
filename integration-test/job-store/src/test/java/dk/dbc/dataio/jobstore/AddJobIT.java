package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

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

public class AddJobIT extends AbstractJobStoreTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public TestName test = new TestName();

    /**
     * Given: an empty job-store <br/>
     * When: a job specification is posted, which references a harvester marcXchange data file
     * containing a number of records exceeding the capacity of a single chunk <br/>
     * Then: a new job is created without error <br/>
     * And: the proper number of chunks is created <br/>
     */
    @Test
    public void createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated()
            throws IOException, JobStoreServiceConnectorException, URISyntaxException {
        final int recordCount = 15;
        final String fileId = createMarcxchangeHarvesterDataFile(tmpFolder.newFile(), recordCount);
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                    .setPackaging("xml")
                    .setFormat("katalog")
                    .setCharset("utf8")
                    .setDestination(test.getMethodName())
                    .setSubmitterId(700000)
                    .setDataFile(FileStoreUrn.create(fileId).toString())
                    .build();
        createFlowStoreEnvironmentMatchingJobSpecification(jobSpecification);

        // When...

        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(getJobInputStream(jobSpecification));

        // Then...

        final State jobState = jobInfoSnapshot.getState();
        assertThat("Partitioning phase complete", jobState.phaseIsDone(State.Phase.PARTITIONING), is(true));
        assertThat("Partitioning phase failures", jobState.getPhase(State.Phase.PARTITIONING).getFailed(), is(0));

        // And...

        assertThat("Number of items", jobInfoSnapshot.getNumberOfItems(), is(recordCount));
        assertThat("Number of chunks", jobInfoSnapshot.getNumberOfChunks(), is(2));
    }

    public static String createMarcxchangeHarvesterDataFile(File datafile, int numberOfRecords) {
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
            try (final InputStream is = new FileInputStream(datafile)) {
                return fileStoreServiceConnector.addFile(is);
            }
        } catch (IOException | HarvesterException | FileStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createFlowStoreEnvironmentMatchingJobSpecification(JobSpecification jobSpecification) {
        try {
            final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder().build());
            final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                    .setName(jobSpecification.getDestination())
                    .build());
            final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                    .setNumber(jobSpecification.getSubmitterId())
                    .build());
            flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                    .setPackaging(jobSpecification.getPackaging())
                    .setFormat(jobSpecification.getFormat())
                    .setCharset(jobSpecification.getCharset())
                    .setDestination(jobSpecification.getDestination())
                    .setSubmitterIds(Arrays.asList(submitter.getId()))
                    .setFlowId(flow.getId())
                    .setSinkId(sink.getId())
                    .build());
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JobInputStream getJobInputStream(JobSpecification jobSpecification) {
        return new JobInputStream(jobSpecification, true, 0);
    }
}
