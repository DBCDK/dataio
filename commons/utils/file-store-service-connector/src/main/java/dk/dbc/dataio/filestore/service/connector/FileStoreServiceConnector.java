package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileStoreServiceConnector - dataIO file-store REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the file-store service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 * <p>
 * Be advised that in order to add files of sizes exceeding the heap size of the JVM a
 * web resources client connector able to use chunked encoding needs to be provided. Note
 * that the default jersey client connector does not adhere to the CHUNKED_ENCODING_SIZE
 * property. Consider using the Apache HttpClient connector instead.
 * </p>
 */
public class FileStoreServiceConnector {
    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public FileStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Adds content of given input stream as file in store
     * @param is input stream of bytes to be written
     * @return ID of generated file
     * @throws NullPointerException if given null-valued dataSource argument
     * @throws ProcessingException on general communication error
     * @throws FileStoreServiceConnectorException on failure to extract file ID from response
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public String addFile(final InputStream is)
            throws NullPointerException, ProcessingException, FileStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(is, "is");
        final Entity<InputStream> entity = Entity.entity(is, MediaType.APPLICATION_OCTET_STREAM);
        final Response response = HttpClient.doPost(httpClient, entity, baseUrl, FileStoreServiceConstants.FILES_COLLECTION);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
            final String fileId = getfileIdFromLocationHeader(response);
            if (fileId == null) {
                throw new FileStoreServiceConnectorException("Unable to extract file ID from Location header");
            }
            return fileId;
        } finally {
            response.close();
        }
    }

    /**
     * Retrieves file content as stream from store into given output stream.
     * <p>
     * Note that it is the responsibility of the caller to close the returned
     * stream to free web client resources.
     * </p>
     * @param fileId ID of file
     * @return file content input stream
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given empty-valued fileId argument
     * @throws ProcessingException on general communication error
     * @throws FileStoreServiceConnectorException on failure to extract input stream from response
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public InputStream getFile(final String fileId)
            throws NullPointerException, IllegalArgumentException, ProcessingException, FileStoreServiceConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
        final Map<String, String> pathVariables = new HashMap<>(1);
        pathVariables.put(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);
        final String path = HttpClient.interpolatePathVariables(FileStoreServiceConstants.FILE, pathVariables);
        final Response response = HttpClient.doGet(httpClient, baseUrl, path.split(HttpClient.URL_PATH_SEPARATOR));
        verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
        return readResponseInputStream(response);
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private InputStream readResponseInputStream(Response response) throws FileStoreServiceConnectorException {
        final InputStream inputStream = response.readEntity(InputStream.class);
        if (inputStream == null) {
            throw new FileStoreServiceConnectorException("file-store service returned with null-valued input stream");
        }
        return inputStream;
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus)
            throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new FileStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("file-store service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    private String getfileIdFromLocationHeader(Response response) {
        final List<Object> locationHeader = HttpClient.getHeader(response, "Location");
        if (locationHeader != null && !locationHeader.isEmpty()) {
            final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
            if (locationHeaderValueParts.length > 0) {
                return locationHeaderValueParts[locationHeaderValueParts.length - 1];
            }
        }
        return null;
    }
}
