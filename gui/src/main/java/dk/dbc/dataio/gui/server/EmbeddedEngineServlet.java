package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
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
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

public class EmbeddedEngineServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedEngineServlet.class);
    private static final long serialVersionUID = 5701538885619048769L;
    private static final String jobStoreName = "dataio-job-store";
    private Path jobStorePath;
    private FileSystemJobStore jobStore;
    private WebResource webResource;
    private String localhostname;

    @Override
    public void init() throws ServletException {
        super.init();
        jobStorePath = FileSystems.getDefault().getPath(String.format("/tmp/%s", jobStoreName));
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
            localhostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (JobStoreException e) {
            throw new ServletException(e);
        } catch (UnknownHostException e) {
            throw new ServletException(e);
        }

        try {
            String flowStoreServiceEndpoint = ServletUtil.getFlowStoreServiceEndpoint();
            log.info("FlowStoreServiceEndpoint: " + flowStoreServiceEndpoint);
            webResource = setupWebResource(flowStoreServiceEndpoint);
        } catch (Exception ex) {
            log.error("Exception caught while initializing: ", ex);
        }
    }

    private WebResource setupWebResource(String flowStoreServiceEndpoint) {
        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        // force client to use Jackson JAX-RS provider (one in org.codehaus.jackson.jaxrs)
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        final Client httpClient = Client.create(clientConfig);

        return httpClient.resource(flowStoreServiceEndpoint);
    }

    public String getFlow(long id, long version) throws NullPointerException, IllegalStateException {
        try {
            final ClientResponse response = webResource.path("flows/" + id + "/" + version).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
//        if (response.getClientResponseStatus() != ClientResponse.Status.) {
//            throw new IllegalStateException(response.getEntity(String.class));
//        }
            log.info("Found something");
            return response.getEntity(String.class);
        } catch (Exception ex) {
            log.error("Caught Exception: ", ex);
            return "";
        }
        // return response.getEnt;
    }

    private String executeJob(String dataPath, String flow) throws Exception {
        final Engine engine = new Engine();
        final String sinkFileUrl;
        try {
            log.info("dataPath: " + dataPath);
            log.info("flow    : " + flow);
            final Job job = engine.insertIntoJobStore(FileSystems.getDefault().getPath(dataPath), flow, jobStore);
            engine.chunkify(job, jobStore);
            engine.process(job, jobStore);

            final String sinkFileName = String.format("%s.sink.txt", job.getId());
            final Path sinkPath = FileSystems.getDefault().getPath(jobStorePath.toString(), sinkFileName);
            engine.sendToSink(job, jobStore, sinkPath);

            sinkFileUrl = String.format("http://%s/%s/%s", localhostname, jobStoreName, sinkFileName);
        } catch (JobStoreException e) {
            throw new Exception(e);
        }
        return sinkFileUrl;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        File dataFile = null;

        // process only multipart requests
        if (ServletFileUpload.isMultipartContent(req)) {
            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                // Parse the request
                String flow = "";
                List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    if (EngineGUI.FORM_FIELD_DATA_FILE.equals(item.getFieldName())) {
                        dataFile = getItem(item);
                    } else if (EngineGUI.FORM_FIELD_FLOW_ID.equals(item.getFieldName())) {
                        String flowId = item.getString("UTF-8");
                        log.info("flow index [{}]", flowId);
                        FlowIdentifier flowIdentifier = new FlowIdentifier(flowId);
                        flow = getFlow(flowIdentifier.id, flowIdentifier.version);
                    }
                    resp.flushBuffer();
                }

                final String sinkFileUrl = executeJob(dataFile.getAbsolutePath(), flow);
                resp.setContentType("text/html");
                resp.getWriter().write(String.format("<a href='%s'>link to sink file</a>", sinkFileUrl));
            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            } finally {
                deleteFile(dataFile);
            }
        } else {
            String errMsg = "Request did not have multipart content";
            log.error(errMsg);
            throw new ServletException(errMsg);
        }
    }

    private class FlowIdentifier {

        public final long id;
        public final long version;

        public FlowIdentifier(String flowIdentifierAsString) {
            String[] flowIdComponents = flowIdentifierAsString.split("-");
            if (flowIdComponents.length != 2 || flowIdComponents[0].isEmpty() || flowIdComponents[1].isEmpty()) {
                String errMsg = "flowID is not legal: [" + flowIdentifierAsString + "]";
                log.error(errMsg);
                throw new IllegalArgumentException(errMsg);
            }
            this.id = Long.valueOf(flowIdComponents[0]);
            this.version = Long.valueOf(flowIdComponents[1]);
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
