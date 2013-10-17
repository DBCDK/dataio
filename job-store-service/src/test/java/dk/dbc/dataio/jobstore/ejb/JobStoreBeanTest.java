package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.IllegalDataException;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;
import java.io.File;
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
        Path f = tmpFolder.newFile().toPath();
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String someXML = "<data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        Job job = jsb.createJob(jobSpec, createDefaultFlow());
        assertThat(jsb.getNumberOfChunksInJob(job), is(1L));
        Chunk chunk = jsb.getChunk(job, 1);
        assertThat(chunk.getRecords().size(), is(1));
        assertThat(base64decode(chunk.getRecords().get(0)), is(xmlHeader + someXML));
    }

    @Test(expected = JobStoreException.class)
    public void gettingChunkFromUnknownJob_throwsException() throws JobStoreException, IOException {
        final Job job = new Job(1, tmpFolder.newFile().toPath(), createDefaultFlow());
        jsb.getChunk(job, 1);
    }

    @Test(expected = JobStoreException.class)
    public void gettingUnknownChunk_throwsException() throws JobStoreException, IOException {
        Path f = tmpFolder.newFile().toPath();
        String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        Job job = jsb.createJob(jobSpec, createDefaultFlow());
        jsb.getChunk(job, jsb.getNumberOfChunksInJob(job) + 1);
    }

    @Test(expected = IllegalDataException.class)
    public void xmlWithoutClosingOuterTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        jsb.createJob(jobSpec, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlMissingClosingRecordTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</data>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        jsb.createJob(jobSpec, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlWithoutClosingRecordAndToplevelTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        jsb.createJob(jobSpec, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlMismatchedClosingOuterTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></wrong>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        JobSpecification jobSpec = createJobSpecification(f);
        jsb.createJob(jobSpec, createDefaultFlow());
    }

    /*
     * Private helper methods:
     */
    private Flow createDefaultFlow() {
        return new Flow(1, 1, new FlowContent("name", "description", new ArrayList<FlowComponent>()));
    }

    private JobSpecification createJobSpecification(Path f) {
        return new JobSpecification("packaging", "format", "charset", "destination", 42L, "verify@dbc.dk", "processing@dbc.dk", "abc", f.toString(), 0L);
    }

}