package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.engine.Chunk;
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
    }

    @Test
    public void oneRecordToOneChunk() throws IOException, JobStoreException {
        jsb.setupJobStore();
        Path f = tmpFolder.newFile().toPath();
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String someXML = "<data><record>Content</record></data>";
        Files.write(f, someXML.getBytes());

        Job job = jsb.createJob(f, new Flow(1, 1, new FlowContent("name", "description", new ArrayList<FlowComponent>())));
        assertThat(jsb.getNumberOfChunksInJob(job), is(1L));
        Chunk chunk = jsb.getChunk(job, 0);
        assertThat(chunk.getRecords().size(), is(1));
        assertThat(Engine.base64decode(chunk.getRecords().get(0)), is(xmlHeader+someXML));
    }
}