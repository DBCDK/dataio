package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.engine.Chunk;
import dk.dbc.dataio.engine.IllegalDataException;
import dk.dbc.dataio.engine.Engine;
import dk.dbc.dataio.engine.Job;
import dk.dbc.dataio.engine.JobStoreException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Before;

public class JobStoreBeanTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private JobStoreBean jsb = null;

    @Before
    public void setUp() {
        jsb = new JobStoreBean();
        jsb.setupJobStore();
    }

    @Test
    public void oneRecordToOneChunk_withoutXMLHeaderInInput() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String someXML = "<data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        Job job = jsb.createJob(f, createDefaultFlow());
        assertThat(jsb.getNumberOfChunksInJob(job), is(1L));
        Chunk chunk = jsb.getChunk(job, 0);
        assertThat(chunk.getRecords().size(), is(1));
        assertThat(Engine.base64decode(chunk.getRecords().get(0)), is(xmlHeader + someXML));
    }

    @Test(expected = JobStoreException.class)
    public void gettingChunkFromUnknownJob_throwsException() throws JobStoreException {
        Job job = new Job(1, null, null);
        jsb.getChunk(job, 1);
    }

    @Test(expected = JobStoreException.class)
    public void gettingUnknownChunk_throwsException() throws JobStoreException, IOException {
        Path f = tmpFolder.newFile().toPath();
        String someXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        Job job = jsb.createJob(f, createDefaultFlow());
        jsb.getChunk(job, jsb.getNumberOfChunksInJob(job));
    }

    @Test(expected = IllegalDataException.class)
    public void xmlWithoutClosingOuterTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        jsb.createJob(f, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlMissingClosingRecordTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</data>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        jsb.createJob(f, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlWithoutClosingRecordAndToplevelTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        jsb.createJob(f, createDefaultFlow());
    }

    @Test(expected = IllegalDataException.class)
    public void xmlMismatchedClosingOuterTag_throwsException() throws IOException, JobStoreException {
        Path f = tmpFolder.newFile().toPath();
        String xmlWithoutClosingOuterTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><data><record>Content</record></wrong>";
        Files.write(f, xmlWithoutClosingOuterTag.getBytes());

        jsb.createJob(f, createDefaultFlow());
    }

    /*
     * Private helper methods:
     */
    private Flow createDefaultFlow() {
        return new Flow(1, 1, new FlowContent("name", "description", new ArrayList<FlowComponent>()));
    }

}