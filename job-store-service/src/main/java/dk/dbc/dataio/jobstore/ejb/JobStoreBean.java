package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.engine.JobStore;
import dk.dbc.dataio.engine.FileSystemJobStore;
import dk.dbc.dataio.engine.JobStoreException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

@Singleton
public class JobStoreBean {

    private static final String jobStoreName = "dataio-job-store";
    private Path jobStorePath = FileSystems.getDefault().getPath(String.format("/tmp/%s", jobStoreName));
    private JobStore jobStore;


    @PostConstruct
    public void setupJobStore() {
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch(JobStoreException ex) {

        }
    }

}
