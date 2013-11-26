package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.transfile.TransFile;
import dk.dbc.dataio.commons.utils.transfile.TransFileData;
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
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class EmbeddedEngineServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedEngineServlet.class);
    private static final long serialVersionUID = 5701538885619048769L;
    private String localhostname;
    private transient Client client;

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

    private JobInfo executeJob(String dataPath, TransFileData transFileData) throws Exception {
        final String primaryEmailAddress = transFileData.getPrimaryEmailAddress() != null ? transFileData.getPrimaryEmailAddress() : "";
        final String secondaryEmailAddress = transFileData.getSecondaryEmailAddress() != null ? transFileData.getSecondaryEmailAddress() : "";
        final String initials = transFileData.getInitials() != null ? transFileData.getInitials() : "";
        final JobSpecification jobSpecification = new JobSpecification(
                transFileData.getTechnicalProtocol(),
                transFileData.getLibraryFormat(),
                transFileData.getCharacterSet(),
                transFileData.getBaseName(),
                transFileData.getSubmitterNumber(),
                primaryEmailAddress,
                secondaryEmailAddress,
                initials,
                dataPath);
        final Response response = HttpClient.doPostWithJson(client, jobSpecification,
                ServletUtil.getJobStoreServiceEndpoint(), JobStoreServiceEntryPoint.JOBS);

        try {
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (status == Response.Status.PRECONDITION_FAILED) {
                throw new IllegalArgumentException("Unable to resolve flow binder from job specification");
            }
            if (status != Response.Status.CREATED) {
                throw new ServletException(String.format("job-store service returned with unexpected status code: %s", status));
            }
            return response.readEntity(JobInfo.class);
        } finally {
            response.close();
        }
    }

    private TransFileData validateTransFile(File transFile, String dataFileName) throws IllegalArgumentException, IOException {
        final List<TransFileData> transFileEntries = TransFile.process(Files.newInputStream(transFile.toPath()));
        if (transFileEntries.size() > 1) {
            throw new IllegalArgumentException("Håndtering af multiple indgange i transfil er ikke implementeret endnu");
        }
        if (transFileEntries.isEmpty()) {
            throw new IllegalArgumentException("transfilen indeholder ingen indgange");
        }
        final TransFileData transFileData = transFileEntries.get(0);
        if (!dataFileName.equals(transFileData.getFileName())) {
            throw new IllegalArgumentException(String.format("Kunne ikke finde datafilen %s", transFileData.getFileName()));
        }
        return transFileData;
    }

    private String buildStatusFromJobInfo(JobInfo jobInfo, TransFileData transFileData, String transFileName) {
        String status;
        switch (jobInfo.getJobErrorCode()) {
            case DATA_FILE_ENCODING_MISMATCH:
                status = String.format("<div>Validering af tegnsæt: Filen %s indeholder ikke samme tegnsæt som angivet i transfilen.</div>", transFileData.getFileName());
                break;
            case DATA_FILE_INVALID:
                status = String.format("<div>Validering af rammeformat: Filen %s indeholder ikke well-formed xml.</div>", transFileData.getFileName());
                break;
            case NO_ERROR:
                final Path sinkFile = Paths.get(jobInfo.getJobResultDataFile());
                status = String.format("<div>%s - Status: OK. Filen %s med %d poster er modtaget. <a href='%s'>Link til sink fil</a></div>", transFileName, transFileData.getFileName(), jobInfo.getJobRecordCount(),
                    String.format("http://%s/%s/%s", localhostname, sinkFile.getParent().getFileName(), sinkFile.getFileName()));
                break;
            default:
                status = String.format("<div>Ukendt job fejlkode: %s</div>", jobInfo.getJobErrorCode().toString());
                break;
        }
        return status;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        File dataFile = null;
        File transFile = null;
        String dataFileOriginalName = "";
        String transFileOriginalName = "";

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
                        dataFileOriginalName = item.getName();
                    } else if (EngineGUI.FORM_FIELD_TRANS_FILE.equals(item.getFieldName())) {
                        transFile = getItem(item);
                        transFileOriginalName = item.getName();
                    }
                    resp.flushBuffer();
                }
                if(dataFile == null || transFile == null) {
                    throw new IllegalArgumentException("dataFile or transFile was not initialized.");
                }

                log.info("data file {}", dataFile.getAbsolutePath());
                log.info("trans file {}", transFile.getAbsolutePath());

                resp.setContentType("text/html");

                TransFileData transFileData;
                try {
                    transFileData = validateTransFile(transFile, dataFileOriginalName);
                } catch (IllegalArgumentException e) {
                    resp.getWriter().write(String.format("<div>%s - not OK. %s</div>", transFileOriginalName, e.getMessage()));
                    return;
                }

                JobInfo jobInfo;
                try {
                    jobInfo = executeJob(dataFile.getAbsolutePath(), transFileData);
                } catch (IllegalArgumentException e) {
                    resp.getWriter().write(String.format("<div>%s Der blev ikke fundet en matchende flowbinder.</div>", transFileData));
                    return;
                }

                resp.getWriter().write(buildStatusFromJobInfo(jobInfo, transFileData, transFileOriginalName));
            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            } finally {
                deleteFile(dataFile);
                deleteFile(transFile);
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
            if(!uploadedFile.delete()) {
                log.info("Could not delete uploaded file {}", uploadedFile.getAbsolutePath());
            }
        }
    }

}

