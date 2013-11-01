package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobStoreBeanTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private JobStoreBean jsb = null;

    @Before
    public void setUp() throws IOException {
        jsb = new JobStoreBean();
        // to avoid folder leak in java.io.tmpdir
        jsb.jobStorePath = tmpFolder.newFolder().toPath();
        jsb.setupJobStore();
    }

    @Test
    public void oneRecordToOneChunk_withoutXMLHeaderInInput() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        final String someXML = "<data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        final JobSpecification jobSpec = createJobSpecification(f);
        final Job job = jsb.createJob(jobSpec, createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.CREATED));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(1L));
        assertThat(jsb.getNumberOfChunksInJob(job), is(1L));
        final Chunk chunk = jsb.getChunk(job, 1);
        assertThat(chunk.getRecords().size(), is(1));
        assertThat(base64decode(chunk.getRecords().get(0)), is(xmlHeader + someXML));
    }

    @Test(expected = JobStoreException.class)
    public void gettingChunkFromUnknownJob_throwsException() throws JobStoreException, IOException, JsonException {
        final String jobInfoData = new JobInfoJsonBuilder().build();
        final Job job = new Job(JsonUtil.fromJson(jobInfoData, JobInfo.class, MixIns.getMixIns()),
                createDefaultFlow());
        jsb.getChunk(job, 1);
    }

    @Test(expected = JobStoreException.class)
    public void gettingUnknownChunk_throwsException() throws JobStoreException, IOException {
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        jsb.getChunk(job, jsb.getNumberOfChunksInJob(job) + 1);
    }

    @Test
    public void xmlWithoutClosingOuterTag_returnsJobInfailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlMissingClosingRecordTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</data>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlWithoutClosingRecordAndToplevelTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlMismatchedClosingOuterTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></wrong>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_dataFileDoesNotExist_returnsJobInFailedState() throws JobStoreException {
        final Path f = Paths.get("no-such-file");
        final Job job = jsb.createJob(createJobSpecification(f), createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_NOT_FOUND));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_mismatchBetweenSpecifiedAndActualEncoding_returnsJobInFailedState() throws JsonException, JobStoreException, IOException {
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());
        final String jobSpecificationData = new JobSpecificationJsonBuilder()
                .setCharset("no-such-charset")
                .setDataFile(f.toString())
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job = jsb.createJob(jobSpecification, createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_ENCODING_MISMATCH));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_dataFileContainsMultipleRecords_recordCountIsCorrect() throws JsonException, JobStoreException, IOException {
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record><record>Content</record></data>";
        Files.write(f, someXML.getBytes());
        final String jobSpecificationData = new JobSpecificationJsonBuilder()
                .setCharset("utf8")
                .setDataFile(f.toString())
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job = jsb.createJob(jobSpecification, createDefaultFlow());
        assertThat(job.getJobInfo().getJobState(), is(JobState.CREATED));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(2L));
    }

    /*
     * Private helper methods:
     */
    private Flow createDefaultFlow() {
        return new Flow(1, 1, new FlowContent("name", "description", new ArrayList<FlowComponent>()));
    }

    private JobSpecification createJobSpecification(Path f) {
        return new JobSpecification("packaging", "format", "utf8", "destination", 42L, "verify@dbc.dk", "processing@dbc.dk", "abc", f.toString());
    }

}