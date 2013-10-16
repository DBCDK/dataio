package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.JobTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class FileSystemJobStoreTest {
    private static final Flow FLOW = JobTest.createDefaultFlow();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

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
        assertFalse(Files.exists(path));
        new FileSystemJobStore(path);
        assertTrue(Files.exists(path));
    }

    @Test(expected = NullPointerException.class)
    public void createJob_dataObjectPathArg_isNull_throws() throws Exception {
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        instance.createJob(null, FLOW);
    }

    @Test(expected = NullPointerException.class)
    public void createJob_flowArgisNull_throws() throws Exception {
        final File dataFile = tmpFolder.newFile();
        final FileSystemJobStore instance = new FileSystemJobStore(getJobStorePath());
        JobSpecification jobSpec = createJobSpecfication(dataFile.toPath());
        instance.createJob(jobSpec, null);
    }

    @Test
    public void createJob_newJobIsCreated_jobFolderIsAddedToJobStore() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecfication(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        assertTrue(Files.exists(Paths.get(jobStorePath.toString(), Long.toString(job.getId()))));
    }

    @Test
    public void createJob_newJobIsCreated_flowFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecfication(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path flowFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.FLOW_FILE);
        assertTrue(Files.exists(flowFile));
        assertThat(readFileIntoString(flowFile), is(JsonUtil.toJson(FLOW)));
    }

    @Test
    public void createJob_newJobIsCreated_chunkCounterFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecfication(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path chunkCounterFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.CHUNK_COUNTER_FILE);
        assertTrue(Files.exists(chunkCounterFile));
        assertThat(readFileIntoString(chunkCounterFile), is("1"));
    }

    private Path getJobStorePath() throws IOException {
        final File root = tmpFolder.newFolder();
        final String jobStoreName = "dataio-job-store";
        return Paths.get(root.getPath(), jobStoreName);
    }

    private String readFileIntoString(Path file) throws IOException {
        return new String(Files.readAllBytes(file), FileSystemJobStore.LOCAL_CHARSET);
    }

    private Path getDataFile() throws IOException {
        final Path dataFile = tmpFolder.newFile().toPath();
        Files.write(dataFile, "<data><record>Content</record></data>".getBytes());
        return dataFile;
    }

    private JobSpecification createJobSpecfication(Path p) {
        return new JobSpecification("packaging", "format", "charset", "destination", 42L, p.toString(), 0L);
    }
}
