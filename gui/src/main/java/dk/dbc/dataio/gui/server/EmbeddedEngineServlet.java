package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.engine.Engine;
import dk.dbc.dataio.engine.FileSystemJobStore;
import dk.dbc.dataio.engine.Job;
import dk.dbc.dataio.engine.JobStoreException;
import dk.dbc.dataio.gui.client.tmpengine.EngineGUI;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class EmbeddedEngineServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedEngineServlet.class);
    private static final long serialVersionUID = 5701538885619048769L;

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

    private void executeJob(String dataPath, String flow) throws Exception {
        //flow = "{\"id\": \"0\", \"content\": {\"name\":\"unknown\", \"description\":\"Beskrivelse\", \"components\" : [ {\"id\" : \"0\", \"content\" : {\"javascript\": \"function convertToUpperCase(record) { return record.toUpperCase(); }\", \"invocationMethod\" : \"convertToUpperCase\"} } ] } }";
        
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        File dataFile = null;
        File flowFile = null;

        // process only multipart requests
        if (ServletFileUpload.isMultipartContent(req)) {
            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                // Parse the request
                List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    if (EngineGUI.FORM_FIELD_DATA_FILE.equals(item.getFieldName())) {
                       dataFile = getItem(item);
                    } else if (EngineGUI.FORM_FIELD_FLOW_FILE.equals(item.getFieldName())) {
                        flowFile = getItem(item);
                    }
                    resp.flushBuffer();
                }

                executeJob(dataFile.getAbsolutePath(), getFileContentAsString(flowFile));
            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            } finally {
                deleteFile(dataFile);
                deleteFile(flowFile);
            }
        } else {
            String errMsg = "Request did not have multipart content";
            log.error(errMsg);
            throw new ServletException(errMsg);
        }
     }

    private File getItem(FileItem item) throws Exception {
        log.info("Accessing multi part item {} from field {}", item.getName(), item.getFieldName());
        final String fileName = item.getName();
        final File uploadedFile = File.createTempFile(FilenameUtils.getName(fileName), null);
        item.write(uploadedFile);
        log.info("Uploaded file to {}", uploadedFile.getAbsolutePath());
        return uploadedFile;
    }

    private static void deleteFile(File uploadedFile) {
        if (uploadedFile != null) {
            log.info("Removing uploaded file {}", uploadedFile.getAbsolutePath());
            uploadedFile.delete();
        }
    }

    private static String getFileContentAsString(File uploadedFile) throws IOException {
         return new String(Files.readAllBytes(uploadedFile.toPath()), Charset.forName("UTF-8"));
    }
}
