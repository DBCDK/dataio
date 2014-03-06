package dk.dbc.dataio.jobstore.fsjobstore;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.types.JobTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FileSystemJobStoreTestUtil {
    static final FlowBinder FLOWBINDER = JobTest.createDefaultFlowBinder();
    static final Flow FLOW = JobTest.createDefaultFlow();
    static final Sink SINK = JobTest.createDefaultSink();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    Path getJobStorePath() throws IOException {
        final File root = tmpFolder.newFolder();
        final String jobStoreName = "dataio-job-store";
        return Paths.get(root.getPath(), jobStoreName);
    }

    String readFileIntoString(Path file) throws IOException {
        return new String(Files.readAllBytes(file), FileSystemJobStore.LOCAL_CHARSET);
    }

    Path getDataFile() throws IOException {
        final Path dataFile = tmpFolder.newFile().toPath();
        Files.write(dataFile, "<data><record>Content</record></data>".getBytes());
        return dataFile;
    }

    JobSpecification createJobSpecification(Path p) {
        return new JobSpecification("packaging", "format", "utf8", "destination", 42L, "verify@dbc.dk", "processing@dbc.dk", "abc", p.toString());
    }
}
