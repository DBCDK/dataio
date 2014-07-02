package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JobStoreBeanTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private JobStoreBean jobStoreBean = null;
    private JobSchedulerBean jobSchedulerBean = mock(JobSchedulerBean.class);

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setUp() throws IOException, NamingException {
        InMemoryInitialContextFactory.bind(JobStoreBean.PATH_RESOURCE_JOB_STORE_HOME, tmpFolder.newFolder().toString());
        jobStoreBean = new JobStoreBean();
        jobStoreBean.setupJobStore();
        jobStoreBean.jobScheduler = jobSchedulerBean;
    }

    @Test
    public void oneRecordToOneChunk_withoutXMLHeaderInInput() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        final String someXML = "<data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        final JobSpecification jobSpec = createJobSpecification(f);
        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(jobSpec, createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(1L));
        assertThat(jobStoreBean.getJobStore().getNumberOfChunksInJob(job.getId()), is(1L));
        final Chunk chunk = jobStoreBean.getJobStore().getChunk(job.getId(), 1);
        assertThat(chunk.getItems().size(), is(1));
        assertThat(base64decode(chunk.getItems().get(0).getData()), is(xmlHeader + someXML));
    }

    @Test
    public void gettingChunkFromUnknownJob_returnsNull() throws JobStoreException, IOException, JsonException {
        final String jobInfoData = new JobInfoJsonBuilder().build();
        final Job job = new Job(JsonUtil.fromJson(jobInfoData, JobInfo.class, MixIns.getMixIns()), new JobState(),
                createDefaultFlow());
        assertThat(jobStoreBean.getJobStore().getChunk(job.getId(), 1), is(nullValue()));
    }

    @Test
    public void gettingUnknownChunk_returnsNull() throws JobStoreException, IOException {
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(jobStoreBean.getJobStore().getChunk(job.getId(), jobStoreBean.getJobStore().getNumberOfChunksInJob(job.getId()) + 1), is(nullValue()));
    }

    @Test
    public void xmlWithoutClosingOuterTag_returnsJobInfailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlMissingClosingRecordTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</data>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlWithoutClosingRecordAndToplevelTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void xmlMismatchedClosingOuterTag_returnsJobInFailedState() throws IOException, JobStoreException {
        final Path f = tmpFolder.newFile().toPath();
        final String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></wrong>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Ignore("Ignored while JobsBean handles the creation of inputstream")
    @Test
    public void createJob_dataFileDoesNotExist_returnsJobInFailedState() throws JobStoreException, IOException {
        final Path f = Paths.get("no-such-file");
        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(createJobSpecification(f), createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
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
        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(jobSpecification, createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
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
        final Job job;
        try(InputStream is = Files.newInputStream(f)) {
            job = jobStoreBean.createAndScheduleJob(jobSpecification, createDefaultFlowBinder(), createDefaultFlow(), createDefaultSink(), is);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(2L));
    }

    /*
     * Private helper methods:
     */
    private FlowBinder createDefaultFlowBinder() {
        return new FlowBinderBuilder().build();
    }

    private Flow createDefaultFlow() {
        return new FlowBuilder().build();
    }

    private Sink createDefaultSink() {
        return new SinkBuilder().build();
    }

    private JobSpecification createJobSpecification(Path f) {
        return new JobSpecificationBuilder()
                .setCharset("utf8")
                .setDataFile(f.toString())
                .build();
    }
}