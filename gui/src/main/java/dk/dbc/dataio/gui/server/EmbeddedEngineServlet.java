package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.engine.Engine;
import dk.dbc.dataio.engine.FileSystemJobStore;
import dk.dbc.dataio.engine.Job;
import dk.dbc.dataio.engine.JobStoreException;
import dk.dbc.dataio.gui.client.proxies.EmbeddedEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class EmbeddedEngineServlet extends RemoteServiceServlet implements EmbeddedEngine {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedEngineServlet.class);
    private static final long serialVersionUID = -1690694115239676102L;

    private FileSystemJobStore jobStore;

    @Override
    public void init() throws ServletException {
        super.init();
        final Path jobStorePath = FileSystems.getDefault().getPath("/tmp/dataio-job-store");
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch (JobStoreException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void executeJob(String dataPath, String flow) throws Exception {
        flow = "{\"id\": \"0\", \"content\": {\"name\":\"unknown\", \"description\":\"Beskrivelse\", \"components\" : [ {\"id\" : \"0\", \"content\" : {\"javascript\": \"function convertToUpperCase(record) { return record.toUpperCase(); }\", \"invocationMethod\" : \"convertToUpperCase\"} } ] } }";
        
        final Engine engine = new Engine();
        try {
            log.info("dataPath: " + dataPath);
            log.info("flow    : " + flow);
            final Job job = engine.insertIntoJobStore(FileSystems.getDefault().getPath(dataPath), flow, jobStore);
            engine.chunkify(job, jobStore);
            engine.process(job, jobStore);
        } catch (JobStoreException e) {
            throw new Exception(e);
        }
    }
}
