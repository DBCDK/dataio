package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkCounter;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FileSystemJobStore_CreateJobTest extends FileSystemJobStoreTestUtil {
    private final SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator();

    @Test(expected = NullPointerException.class)
    public void createJob_dataObjectPathArg_isNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        try(InputStream is = Files.newInputStream(dataFile.toPath())) {
            instance.createJob(null, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
    }

    @Test(expected = NullPointerException.class)
    public void createJob_flowBinderArgisNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        JobSpecification jobSpec = createJobSpecification(dataFile.toPath());
        try(InputStream is = Files.newInputStream(dataFile.toPath())) {
            instance.createJob(jobSpec, null, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
    }

    @Test(expected = NullPointerException.class)
    public void createJob_flowArgisNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        JobSpecification jobSpec = createJobSpecification(dataFile.toPath());
        try(InputStream is = Files.newInputStream(dataFile.toPath())) {
            instance.createJob(jobSpec, FLOWBINDER, null, SINK, is, sequenceAnalyserKeyGenerator);
        }
    }

    @Test(expected = NullPointerException.class)
    public void createJob_sinkArgisNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        JobSpecification jobSpec = createJobSpecification(dataFile.toPath());
        try(InputStream is = Files.newInputStream(dataFile.toPath())) {
            instance.createJob(jobSpec, FLOWBINDER, FLOW, null, is, sequenceAnalyserKeyGenerator);
        }
    }

    @Test(expected = NullPointerException.class)
    public void createJob_jobInputStreamArgisNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        JobSpecification jobSpec = createJobSpecification(dataFile.toPath());
        instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, null, sequenceAnalyserKeyGenerator);
    }

    @Test
    public void createJob_newJobIsCreated_jobFolderIsAddedToJobStore() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        assertThat(Files.exists(Paths.get(jobStorePath.toString(), Long.toString(job.getId()))), is(true));
    }

    private class TestJob {

        private Path jobStorePath;
        private FileSystemJobStore instance;
        private JobSpecification jobSpec;
        private FlowBinder flowBinder;
        private Flow flow;
        private Sink sink;
        private Job job;

        public TestJob setJobStorePath(Path jobStorePath) {
            this.jobStorePath = jobStorePath;
            return this;
        }

        public TestJob setJobSpec(JobSpecification jobSpec) {
            this.jobSpec = jobSpec;
            return this;
        }

        public TestJob setFlowBinder(FlowBinder flowBinder) {
            this.flowBinder = flowBinder;
            return this;
        }

        public TestJob setFlow(Flow flow) {
            this.flow = flow;
            return this;
        }

        public TestJob setSink(Sink sink) {
            this.sink = sink;
            return this;
        }

        public TestJob create() throws JobStoreException, IOException {
            if (jobStorePath == null) {
                jobStorePath = getJobStorePath();
            }
            if (jobSpec == null) {
                jobSpec = createJobSpecification(getDataFile());
            }
            if (flowBinder == null) {
                flowBinder = FLOWBINDER;
            }
            if (sink == null) {
                sink = SINK;
            }
            if (flow == null) {
                flow = FLOW;
            }
            instance = new FileSystemJobStore(jobStorePath);

            try(InputStream is = Files.newInputStream(Paths.get(jobSpec.getDataFile()))) {
                job = instance.createJob(jobSpec, flowBinder, flow, sink, is, sequenceAnalyserKeyGenerator);
            }
            return this;
        }

        public Path getTheJobStorePath() throws TestJobException {
            if (jobStorePath == null) {
                throw new TestJobException("jobStorePath is not initialized");
            }
            return jobStorePath;
        }

        public Job getJob() throws TestJobException {
            if (job == null) {
                throw new TestJobException("Job is not initialized!");
            }
            return job;
        }

        public class TestJobException extends Exception {

            public TestJobException(String message) {
                super(message);
            }
        }
    }

    @Test
    public void createJob_newJobIsCreated_chunkFileWithGeneratedKeysIsAddedToJobFolder() throws Exception {
        final TestJob testJob = new TestJob().create();
        final FileSystemJobStore jobStore = new FileSystemJobStore(testJob.getTheJobStorePath());
        final Chunk chunk = jobStore.getChunk(testJob.job.getId(), 1);
        assertThat(chunk, is(notNullValue()));
        assertThat(chunk.getKeys().size(), is(1));
        assertThat(chunk.getKeys().contains(testJob.sink.getContent().getName()), is(true));
    }

    @Test
    public void createJob_newJobIsCreated_flowFileIsAddedToJobFolder() throws Exception {
        TestJob testJob = new TestJob().create();
        final Path flowFile = Paths.get(testJob.getTheJobStorePath().toString(), Long.toString(testJob.getJob().getId()), FileSystemJobStore.FLOW_FILE);

        assertThat(Files.exists(flowFile), is(true));
        assertThat(readFileIntoString(flowFile), is(JsonUtil.toJson(FLOW)));
    }

    // todo: Add test for flowbinder added to jobstore
    // todo: Add test for sink added to jobstore

    @Test
    public void createJob_newJobIsCreated_chunkCounterFileIsAddedToJobFolder() throws Exception {
        TestJob testJob = new TestJob().create();
        final Path chunkCounterFile = Paths.get(testJob.getTheJobStorePath().toString(), Long.toString(testJob.getJob().getId()), FileSystemJobStore.CHUNK_COUNTER_FILE);

        assertThat(Files.exists(chunkCounterFile), is(true));
        final ChunkCounter chunkCounter = JsonUtil.fromJson(readFileIntoString(chunkCounterFile), ChunkCounter.class);
        assertThat(chunkCounter.getTotal(), is(1L));
        assertThat(chunkCounter.getItemResultCounter().getTotal(), is(1L));
        assertThat(chunkCounter.getItemResultCounter().getSuccess(), is(1L));
    }

    @Test
    public void createJob_newJobIsCreated_processorCounterFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        final Path processorCounterFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.PROCESSOR_COUNTER_FILE);
        assertThat(Files.exists(processorCounterFile), is(true));
        final ChunkCounter chunkCounter = JsonUtil.fromJson(readFileIntoString(processorCounterFile), ChunkCounter.class);
        assertThat(chunkCounter.getTotal(), is(0L));
        assertThat(chunkCounter.getItemResultCounter().getTotal(), is(0L));
    }

    @Test
    public void createJob_newJobIsCreated_jobSpecificationFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        final Path jobSpecificationFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.JOBSPECIFICATION_FILE);
        assertThat(Files.exists(jobSpecificationFile), is(true));
    }

    @Test
    public void createJob_newJobIsCreated_jobInfoFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        final Path jobInfoFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.JOBINFO_FILE);
        assertThat(Files.exists(jobInfoFile), is(true));
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(1L));
    }

    @Test
    public void createJob_newJobIsCreated_jobStateFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        final Path jobStateFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.JOBSTATE_FILE);
        assertThat(Files.exists(jobStateFile), is(true));
        final JobState jobState = JsonUtil.fromJson(readFileIntoString(jobStateFile), JobState.class);
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
    }

    @Ignore("Ignored while JobsBean handles the creation of inputstream")
    @Test
    public void createJob_dataFileDoesNotExist_returnsJobInFailedState() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(Paths.get("no-such-file"));
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_NOT_FOUND));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_mismatchBetweenSpecifiedAndActualEncoding_returnsJobInFailedState() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes("UTF-8"));
        final String jobSpecificationData = new JobSpecificationJsonBuilder()
                .setCharset("no-such-charset")
                .setDataFile(f.toString())
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpecification, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_ENCODING_MISMATCH));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_invalidDataFile_returnsJobInFailedState() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</drocer></data>";
        Files.write(f, someXML.getBytes("UTF-8"));
        final String jobSpecificationData = new JobSpecificationJsonBuilder()
                .setDataFile(f.toString())
                .setCharset("UTF-8")
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job;
        try(InputStream is = Files.newInputStream(Paths.get(jobSpecification.getDataFile()))) {
            job = instance.createJob(jobSpecification, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
        assertThat(job.getJobInfo().getJobRecordCount(), is(0L));
    }

    @Test
    public void createJob_dataFileContainsMultipleRecords_recordCountIsCorrect() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record><record>Content</record></data>";
        Files.write(f, someXML.getBytes("UTF-8"));
        final String jobSpecificationData = new JobSpecificationJsonBuilder()
                .setDataFile(f.toString())
                .setCharset("UTF-8")
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job;
        try(InputStream is = Files.newInputStream(Paths.get(jobSpecification.getDataFile()))) {
            job = instance.createJob(jobSpecification, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job.getJobState().getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.DONE));
        assertThat(job.getJobInfo().getJobErrorCode(), is(JobErrorCode.NO_ERROR));
        assertThat(job.getJobInfo().getJobRecordCount(), is(2L));
    }

}
