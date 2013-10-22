package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.integrationtest.ITUtil;
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
        assertThat(Files.exists(path), is(false));
        new FileSystemJobStore(path);
        assertThat(Files.exists(path), is(true));
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
        JobSpecification jobSpec = createJobSpecification(dataFile.toPath());
        instance.createJob(jobSpec, null);
    }

    @Test
    public void createJob_newJobIsCreated_jobFolderIsAddedToJobStore() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        assertThat(Files.exists(Paths.get(jobStorePath.toString(), Long.toString(job.getId()))), is(true));
    }

    @Test
    public void createJob_newJobIsCreated_flowFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path flowFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.FLOW_FILE);
        assertThat(Files.exists(flowFile), is(true));
        assertThat(readFileIntoString(flowFile), is(JsonUtil.toJson(FLOW)));
    }

    @Test
    public void createJob_newJobIsCreated_chunkCounterFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path chunkCounterFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.CHUNK_COUNTER_FILE);
        assertThat(Files.exists(chunkCounterFile), is(true));
        assertThat(readFileIntoString(chunkCounterFile), is("1"));
    }

    @Test
    public void createJob_newJobIsCreated_jobSpecificationFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path jobSpecificationFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.JOBSPECIFICATION_FILE);
        assertThat(Files.exists(jobSpecificationFile), is(true));
    }

    @Test
    public void createJob_newJobIsCreated_jobInfoFileIsAddedToJobFolder() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(getDataFile());
        final Job job = instance.createJob(jobSpec, FLOW);
        assertThat(job, is(notNullValue()));
        final Path jobInfoFile = Paths.get(jobStorePath.toString(), Long.toString(job.getId()), FileSystemJobStore.JOBINFO_FILE);
        assertThat(Files.exists(jobInfoFile), is(true));
        final JobInfo jobInfo = JsonUtil.fromJson(readFileIntoString(jobInfoFile), JobInfo.class, MixIns.getMixIns());
        assertThat(jobInfo.getJobState(), is(JobState.CREATED));
    }

    @Test(expected = JobStoreException.class)
    public void createJob_dataFileDoesNotExist_throws() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final JobSpecification jobSpec = createJobSpecification(Paths.get("no-such-file"));
        instance.createJob(jobSpec, FLOW);
    }

    @Test
    public void createJob_mismatchBetweenSpecifiedAndActualEncoding_returnsJobInFailedState() throws Exception {
        final Path jobStorePath = getJobStorePath();
        final FileSystemJobStore instance = new FileSystemJobStore(jobStorePath);
        final Path f = tmpFolder.newFile().toPath();
        final String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());
        final String jobSpecificationData = new ITUtil.JobSpecificationJsonBuilder()
                .setCharset("no-such-charset")
                .setDataFile(f.toString())
                .build();
        final JobSpecification jobSpecification = JsonUtil.fromJson(jobSpecificationData, JobSpecification.class, MixIns.getMixIns());
        final Job job = instance.createJob(jobSpecification, FLOW);
        assertThat(job.getJobInfo().getJobState(), is(JobState.FAILED_DURING_CREATION));
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

    private JobSpecification createJobSpecification(Path p) {
        return new JobSpecification("packaging", "format", "utf8", "destination", 42L, "verify@dbc.dk", "processing@dbc.dk", "abc", p.toString(), 0L);
    }
}
