package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.ChunkCounter;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.jobstore.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.ChunkResultBuilder;
import dk.dbc.dataio.jobstore.types.SinkChunkResultBuilder;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FileSystemJobStoreTest extends FileSystemJobStoreTestUtil {
    private final SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator();

    @Test(expected = NullPointerException.class)
    public void constructor_storePathArgIsNull_throws() throws Exception {
        new FileSystemJobStore(null);
    }

    @Test(expected = JobStoreException.class)
    public void constructor_storePathArgPointsToExistingFile_throws() throws Exception {
        final File existingFile = tmpFolder.newFile();
        new FileSystemJobStore(existingFile.toPath());
    }

    @Test(expected = JobStoreException.class)
    public void constructor_storePathCanNotBeCreated_throws() throws Exception {
        new FileSystemJobStore(Paths.get("/dataio-test"));
    }

    @Test
    public void constructor_storePathDoesNotExist_storePathIsCreated() throws Exception {
        final Path path = getJobStorePath();
        assertThat(Files.exists(path), is(false));
        new FileSystemJobStore(path);
        assertThat(Files.exists(path), is(true));
    }

    @Test
    public void addProcessorResult_processorResultIsAdded_writesResultFileAndIncrementsProcessorCounterAndUpdatesState() throws IOException, JobStoreException, JsonException {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        final ChunkResult processorResult = new ChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addProcessorResult(processorResult);
        final Path processorResultFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                String.format(FileSystemJobStore.PROCESSOR_RESULT_FILENAME_PATTERN, processorResult.getChunkId()));
        assertThat(Files.exists(processorResultFile), is(true));
        final Path processorCounterFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                FileSystemJobStore.PROCESSOR_COUNTER_FILE);
        assertThat(Files.exists(processorCounterFile), is(true));
        final ChunkCounter chunkCounter = JsonUtil.fromJson(readFileIntoString(processorCounterFile), ChunkCounter.class);
        assertThat(chunkCounter.getTotal(), is(1L));
        assertThat(chunkCounter.getItemResultCounter().getTotal(), is(1L));
        assertThat(chunkCounter.getItemResultCounter().getSuccess(), is(1L));
        final Path stateFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                FileSystemJobStore.JOBSTATE_FILE);
        final JobState jobState = JsonUtil.fromJson(readFileIntoString(stateFile), JobState.class, MixIns.getMixIns());
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.DONE));
    }

    @Test
    public void addSinkResult_sinkResultIsAdded_writesResultFileAndIncrementsProcessorCounterAndUpdatesState() throws IOException, JobStoreException, JsonException {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;

        // Create job in file system
        try (InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));

        // Create a new chunk and add as Processor Result
        final ChunkResult processorChunkResult = new ChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addProcessorResult(processorChunkResult);

        // Create a new chunk and add as Sink Result
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addSinkResult(sinkChunkResult);

        // Read and assert Sink Result from file system
        final Path sinkResultFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                String.format(FileSystemJobStore.SINK_RESULT_FILENAME_PATTERN, sinkChunkResult.getChunkId()));
        assertThat(Files.exists(sinkResultFile), is(true));

        // Read and assert Sink Counter file
        final Path sinkCounterFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                FileSystemJobStore.SINK_COUNTER_FILE);
        assertThat(Files.exists(sinkCounterFile), is(true));
        final ChunkCounter sinkChunkCounter = JsonUtil.fromJson(readFileIntoString(sinkCounterFile), ChunkCounter.class);
        assertThat(sinkChunkCounter.getTotal(), is(1L));
        assertThat(sinkChunkCounter.getItemResultCounter().getTotal(), is(1L));
        assertThat(sinkChunkCounter.getItemResultCounter().getSuccess(), is(1L));

    }

    @Test
    public void addSinkResult_sinkResultIsAdded_writesResultFileAndIncrementsProcessorCounterAndUpdatesJobInfo() throws IOException, JobStoreException, JsonException {

        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;

        // Create job in file system
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));

        // Create a new chunk and add as Processor Result
        final ChunkResult processorChunkResult = new ChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addProcessorResult(processorChunkResult);

        // Create a new chunk and add as Sink Result
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addSinkResult(sinkChunkResult);

        // Read and assert JobInfo file
        final Path jobInfoPath = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                FileSystemJobStore.JOBINFO_FILE);
        final JobInfo jobInfo = JsonUtil.fromJson(readFileIntoString(jobInfoPath), JobInfo.class);
        assertThat(jobInfo, is(notNullValue()));

        ChunkCounter chunkifyingCounter = jobInfo.getChunkifyingChunkCounter();
        assertThat(chunkifyingCounter, is(notNullValue()));
        assertThat(chunkifyingCounter.getItemResultCounter().getFailure(), is(0L));
        assertThat(chunkifyingCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(chunkifyingCounter.getItemResultCounter().getSuccess(), is(1L));

        ChunkCounter processingCounter = jobInfo.getProcessingChunkCounter();
        assertThat(jobInfo.getProcessingChunkCounter(), is(notNullValue()));
        assertThat(processingCounter.getItemResultCounter().getFailure(), is(0L));
        assertThat(processingCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(processingCounter.getItemResultCounter().getSuccess(), is(1L));

        ChunkCounter deliveringCounter = jobInfo.getDeliveringChunkCounter();
        assertThat(jobInfo.getDeliveringChunkCounter(), is(notNullValue()));
        assertThat(deliveringCounter.getItemResultCounter().getFailure(), is(0L));
        assertThat(deliveringCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(deliveringCounter.getItemResultCounter().getSuccess(), is(1L));

        assertThat(chunkifyingCounter.getTotal(), is(1L));
    }

    @Test
    public void addSinkResult_sinkResultIsAddedWithFailure_writesResultFileAndIncrementsProcessorCounterAndUpdatesJobInfoWithFailure() throws IOException, JobStoreException, JsonException {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;

        // Create job in file system
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));

        List<ChunkItem> items = new ArrayList<>(Arrays.asList(new ChunkItemBuilder().setStatus(ChunkItem.Status.FAILURE).build()));

        // Create a new chunk and add as Processor Result
        final ChunkResult processorChunkResult = new ChunkResultBuilder()
                .setJobId(job.getId())
                .setItems(items) //Set item with failure
                .build();
        instance.addProcessorResult(processorChunkResult);

        // Create a new chunk and add as Sink Result
        final SinkChunkResult sinkChunkResult = new SinkChunkResultBuilder()
                .setJobId(job.getId())
                .build();
        instance.addSinkResult(sinkChunkResult);

        // Read and assert Sink Result from file system
        final Path sinkResultFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                String.format(FileSystemJobStore.SINK_RESULT_FILENAME_PATTERN, sinkChunkResult.getChunkId()));
        assertThat(Files.exists(sinkResultFile), is(true));

        // Read and assert JobInfo file
        final Path jobInfoPath = Paths.get(jobStorePath.toString(), Long.toString(job.getId()),
                FileSystemJobStore.JOBINFO_FILE);
        final JobInfo jobInfo = JsonUtil.fromJson(readFileIntoString(jobInfoPath), JobInfo.class);
        assertThat(jobInfo, is(notNullValue()));

        ChunkCounter chunkifyingCounter = jobInfo.getChunkifyingChunkCounter();
        assertThat(chunkifyingCounter, is(notNullValue()));
        assertThat(chunkifyingCounter.getItemResultCounter().getFailure(), is(0L));
        assertThat(chunkifyingCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(chunkifyingCounter.getItemResultCounter().getSuccess(), is(1L));

        ChunkCounter processingCounter = jobInfo.getProcessingChunkCounter();
        assertThat(jobInfo.getProcessingChunkCounter(), is(notNullValue()));
        assertThat(processingCounter.getItemResultCounter().getFailure(), is(1L));
        assertThat(processingCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(processingCounter.getItemResultCounter().getSuccess(), is(0L));

        ChunkCounter deliveringCounter = jobInfo.getDeliveringChunkCounter();
        assertThat(jobInfo.getDeliveringChunkCounter(), is(notNullValue()));
        assertThat(deliveringCounter.getItemResultCounter().getFailure(), is(0L));
        assertThat(deliveringCounter.getItemResultCounter().getIgnore(), is(0L));
        assertThat(deliveringCounter.getItemResultCounter().getSuccess(), is(1L));

        assertThat(chunkifyingCounter.getTotal(), is(1L));
    }

    @Test
    public void test() throws IOException, JobStoreException {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job;

        // Create job in file system
        try(InputStream is = Files.newInputStream(getDataFile())) {
            job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK, is, sequenceAnalyserKeyGenerator);
        }
        assertThat(job, is(notNullValue()));
        assertThat(instance.getSink(job.getId()).getId(), is(SINK.getId()));
    }
}
