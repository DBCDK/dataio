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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String executeJob(String dataPath, TransfileData transfileData, long flowId) throws Exception {
        final JobSpecification jobSpecification = new JobSpecification(
                transfileData.packaging,
                transfileData.format,
                transfileData.charset,
                transfileData.destination,
                transfileData.submitterId,
                transfileData.verificationMail,
                transfileData.processingMail,
                transfileData.resultMailInitials,
                dataPath);
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
        Map<String, String> transfileFieldsMap = new HashMap<String, String>();

        log.info("TESTING");
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
                    } else {
                        // transfiledata input fields:
                        String fieldName = item.getFieldName();
                        String itemString = item.getString("UTF-8");
                        transfileFieldsMap.put(fieldName, itemString);
                    }
                    resp.flushBuffer();
                }
                TransfileData transfileData = new TransfileData(transfileFieldsMap);
                log.info("transfile: \n{}", transfileData.toString());

                final String sinkFileUrl = executeJob(dataFile.getAbsolutePath(), transfileData, flowId);
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

    private class TransfileData {
        public final Long submitterId;
        public final String filename;
        public final String format;
        public final String packaging;
        public final String charset;
        public final String destination;
        public final String verificationMail;
        public final String processingMail;
        public final String resultMailInitials;

        public TransfileData(Map<String, String> fieldsAndValues) {
            filename = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_FILENAME);
            format = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_FORMAT);
            packaging = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_PACKAGING);
            charset = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_CHARSET);
            destination = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_DESTINATION);
            verificationMail = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_VERIFICATION_MAIL);
            processingMail = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_PROCESSING_MAIL);
            resultMailInitials = fieldsAndValues.get(EngineGUI.FORM_FIELD_TRANSFILE_RESULT_MAIL_INITIALS);
            submitterId = Long.valueOf(filename.substring(0, filename.indexOf(".")));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("filename : " + filename + "\n");
            sb.append("Sumbitter String: " + filename.substring(0, filename.indexOf(".")) + "\n");
            sb.append("submitter : " + submitterId + "\n");
            sb.append("format : " + format + "\n");
            sb.append("packaging : " + packaging + "\n");
            sb.append("charset : " + charset + "\n");
            sb.append("destination : " + destination + "\n");
            sb.append("verificationMail : " + verificationMail + "\n");
            sb.append("processingMail : " + processingMail + "\n");
            sb.append("resultMailInitials : " + resultMailInitials + "\n");
            return sb.toString();
        }
    }
}

