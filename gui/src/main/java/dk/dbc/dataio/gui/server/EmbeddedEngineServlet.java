package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.gui.client.tmpengine.EngineGUI;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class EmbeddedEngineServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedEngineServlet.class);
    private static final long serialVersionUID = 5701538885619048769L;
    private String localhostname;
    private Client client;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            localhostname = java.net.InetAddress.getLocalHost().getHostName();
            client = setupHttpClient();
        } catch (UnknownHostException e) {
            throw new ServletException(e);
        }
    }

    private Client setupHttpClient() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        return HttpClient.newClient(clientConfig);
    }

    private String executeJob(String dataPath, long flowId) throws Exception {
        final JobSpecification jobSpecification = new JobSpecification("packaging", "format", "charset", "destination", 42L, dataPath, flowId);
        final Response response = HttpClient.doPostWithJson(client, jobSpecification,
                    ServletUtil.getJobStoreServiceEndpoint(), JobStoreServiceEntryPoint.JOBS);

        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != Response.Status.CREATED) {
            throw new ServletException(String.format("job-store service returned with unexpected status code: %s", status));
        }

        final JobInfo jobInfo = response.readEntity(JobInfo.class);
        final Path sinkFile = Paths.get(jobInfo.getJobResultDataFile());
        return String.format("http://%s/%s/%s", localhostname, sinkFile.getParent().getFileName(), sinkFile.getFileName());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        File dataFile = null;
        long flowId = 0;

        // process only multipart requests
        if (ServletFileUpload.isMultipartContent(req)) {
            // Create a factory for disk-based file items
            final FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            final ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                // Parse the request
                final List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    if (EngineGUI.FORM_FIELD_DATA_FILE.equals(item.getFieldName())) {
                        dataFile = getItem(item);
                    } else if (EngineGUI.FORM_FIELD_FLOW_ID.equals(item.getFieldName())) {
                        flowId = Long.valueOf(item.getString("UTF-8"));
                        log.info("flow ID [{}]", flowId);
                    }
                    resp.flushBuffer();
                }

                final String sinkFileUrl = executeJob(dataFile.getAbsolutePath(), flowId);
                resp.setContentType("text/html");
                resp.getWriter().write(String.format("<a href='%s'>link to sink file</a>", sinkFileUrl));
            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            } finally {
                deleteFile(dataFile);
            }
        } else {
            final String errMsg = "Request did not have multipart content";
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
}
