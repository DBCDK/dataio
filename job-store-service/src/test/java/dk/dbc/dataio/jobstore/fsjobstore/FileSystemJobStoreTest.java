package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import dk.dbc.dataio.jobstore.types.ChunkCounter;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobState;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FileSystemJobStoreTest extends FileSystemJobStoreTestUtil {

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
        final Job job = instance.createJob(jobSpec, FLOWBINDER, FLOW, SINK);
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

}
