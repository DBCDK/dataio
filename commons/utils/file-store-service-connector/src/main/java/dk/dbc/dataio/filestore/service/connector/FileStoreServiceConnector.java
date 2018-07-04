/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private static final Logger log = LoggerFactory.getLogger(FileStoreServiceConnector.class);

    private static final RetryPolicy RETRY_POLICY = new RetryPolicy()
            .retryOn(Collections.singletonList(ProcessingException.class))
            .retryIf((Response response) -> response.getStatus() == 404 || response.getStatus() == 500 || response.getStatus() == 502)
            .withDelay(10, TimeUnit.SECONDS)
            .withMaxRetries(6);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public FileStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    FileStoreServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
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
        log.trace("addFile()");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(is, "is");
            final Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(new String[] {FileStoreServiceConstants.FILES_COLLECTION})
                    .withData(is, MediaType.APPLICATION_OCTET_STREAM)
                    .execute();
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
        } finally {
            log.info("addFile took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Appends content to existing file in store
     * @param fileId ID of existing file
     * @param bytes data to be appended
     * @throws NullPointerException if given null-valued fileId argument
     * @throws ProcessingException on general communication error
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public void appendToFile(final String fileId, final byte[] bytes)
            throws NullPointerException, ProcessingException, FileStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
            if (bytes != null) {
                final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                        .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);
                final Response response = new HttpPost(failSafeHttpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(path.build())
                        .withData(bytes, MediaType.APPLICATION_OCTET_STREAM)
                        .execute();
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            }
        } finally {
            log.info("appendToFile({}) took {} milliseconds", fileId, stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves file content as stream from store
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
        log.trace("getFile({})", fileId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
            final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                    .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);
            final Response response = new HttpGet(failSafeHttpClient)
                    .withHeader("Accept-Encoding", "") // null does not clear, even though is should
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, InputStream.class);
        } finally {
            log.info("getFile({}) took {} milliseconds", fileId, stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes file from store
     * @param fileId ID of file
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given empty-valued fileId argument
     * @throws FileStoreServiceConnectorException on general communication error
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public void deleteFile(final String fileId)
            throws NullPointerException, IllegalArgumentException, FileStoreServiceConnectorException {
        log.trace("deleteFile({})", fileId);
        final StopWatch stopWatch = new StopWatch();
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);

        final Response response = doDelete(path);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
        } finally {
            response.close();
            log.info("deleteFile({}) took {} milliseconds", fileId, stopWatch.getElapsedTime());
        }
    }

    public void deleteFile(FileStoreUrn urn)
            throws NullPointerException, IllegalArgumentException, FileStoreServiceConnectorException {
        deleteFile(InvariantUtil.checkNotNullOrThrow(urn, "urn").getFileId());
    }

    /**
     * Retrieves size of a file in bytes.
     * @param fileId ID of file
     * @return byte size of file
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given empty-valued fileId argument
     * @throws FileStoreServiceConnectorException on failure to extract byte size from response
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public long getByteSize(final String fileId) throws NullPointerException, IllegalArgumentException, FileStoreServiceConnectorException {
        log.trace("getByteSize({})", fileId);
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
            final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
                    .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);
            final Response response = new HttpGet(failSafeHttpClient)
                    .withHeader("Accept-Encoding", "") // null does not clear, even though is should
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Long.class);
        } finally {
            log.info("getByteSize({}) took {} milliseconds", fileId, stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds metadata for an existing file overwriting any metadata already present
     * @param fileId ID of existing file
     * @param metadata metadata to be added
     * @throws NullPointerException if given null-valued fileId argument
     * @throws ProcessingException on general communication error
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public void addMetadata(final String fileId, final Object metadata)
            throws NullPointerException, ProcessingException,
                   FileStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
            if (metadata != null) {
                final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                        .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId);
                final Response response = new HttpPost(failSafeHttpClient)
                        .withBaseUrl(baseUrl)
                        .withPathElements(path.build())
                        .withData(metadata, MediaType.APPLICATION_JSON)
                        .execute();
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            }
        } finally {
            log.info("addMetadata({}) took {} milliseconds", fileId, stopWatch.getElapsedTime());
        }
    }

    /**
     * Lists files matching given metadata
     * @param metadata metadata selector
     * @param tClass class of result entities
     * @param <T> type parameter
     * @return list of result entities for matching files
     * @throws ProcessingException on general communication error
     * @throws FileStoreServiceConnectorException on failure to read result entities from response
     * @throws FileStoreServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public <T> List<T> searchByMetadata(final Object metadata, Class<T> tClass)
            throws ProcessingException, FileStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            if (metadata == null) {
                return Collections.emptyList();
            }
            final Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FileStoreServiceConstants.FILES_COLLECTION)
                    .withData(metadata, MediaType.APPLICATION_JSON)
                    .execute();
            return readResponseEntity(response, new GenericType<>(createGenericListType(tClass)));
        } finally {
            log.info("searchByMetadata took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public Client getClient() {
        return failSafeHttpClient.getClient();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws FileStoreServiceConnectorException {
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new FileStoreServiceConnectorException (
                    String.format("file-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }

     private <T> T readResponseEntity(Response response, GenericType<T> genericType)
             throws FileStoreServiceConnectorException {
        response.bufferEntity();
        final T entity = response.readEntity(genericType);
        if (entity == null) {
            throw new FileStoreServiceConnectorException(
                    String.format("file-store service returned with null-valued List<%s> entity",
                            genericType.getRawType().getName()));
        }
        return entity;
    }

    private <T> ParameterizedType createGenericListType(final Class<T> tClass) {
        return new ParameterizedType() {
            private final Type[] actualType = {tClass};
            public Type[] getActualTypeArguments() {
                return actualType;
            }
            public Type getRawType() {
                return List.class;
            }
            public Type getOwnerType() {
                return null;
            }
        };
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws FileStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new FileStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("file-store service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    private String getfileIdFromLocationHeader(Response response) {
        final List<Object> locationHeader = response.getHeaders().get("Location");
        if (locationHeader != null && !locationHeader.isEmpty()) {
            final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
            if (locationHeaderValueParts.length > 0) {
                return locationHeaderValueParts[locationHeaderValueParts.length - 1];
            }
        }
        return null;
    }

    private Response doDelete(PathBuilder path) throws FileStoreServiceConnectorException {
        try {
            return new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute();
        } catch (ProcessingException e) {
            throw new FileStoreServiceConnectorException("file-store communication error", e);
        }
    }
}
