package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@value dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants#FILES_COLLECTION}' entry point
 */
@Stateless
@Path("/")
public class FilesBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesBean.class);
    private static final JSONBContext jsonbContext = new JSONBContext();

    @EJB
    FileStoreBean fileStore;

    /**
     * Creates new file in file-store containing data from the given data stream
     *
     * @param uriInfo    application and request URI information
     * @param dataStream binary data to be written to file
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     * @throws IOException if an I/O error occurs.
     */
    @POST
    @Path(FileStoreServiceConstants.FILES_COLLECTION)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Stopwatch
    public Response addFile(@Context UriInfo uriInfo, InputStream dataStream) throws IOException {
        LOGGER.trace("addFile() method called");
        try {
            final String fileId = fileStore.addFile(dataStream);
            LOGGER.info("Added data to file with ID {}", fileId);
            return Response.created(getUri(uriInfo, fileId)).build();
        } finally {
            dataStream.close();
        }
    }

    /**
     * Appends content to existing file.
     * <p>
     * Be advised that no attempt to synchronize multiple concurrent requests
     * appending to the same file is being made, so in such a scenario it is
     * entirely up to the client to guarantee a well-defined result.
     *
     * @param id    id of existing file
     * @param is InputStream to be appended from
     * @return HTTP 200 OK
     * a HTTP 404 NOT_FOUND response in case the id could not be found
     */
    @POST
    @Path(FileStoreServiceConstants.FILE)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Stopwatch
    public Response appendToFile(@PathParam("id") String id, InputStream is) {
        if (!fileStore.fileExists(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        fileStore.appendToFile(id, is);
        return Response.ok().build();
    }

    /**
     * Retrieves content of file contained in file-store as binary data stream.
     * <p>
     * If HTTP header Accept-Encoding contains bzip2 or gzip and the binary file is
     * formatted using the corresponding compression algorithm, its content is
     * returned in its compressed form, otherwise it will be automatically decompressed.
     *
     * @param id             ID of file
     * @param acceptEncoding value of Accept-Encoding header
     * @return a HTTP 200 OK response with file data as binary stream
     * a HTTP 404 NOT_FOUND response in case the id could not be found
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @Path(FileStoreServiceConstants.FILE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Stopwatch
    public Response getFile(@HeaderParam("Accept-Encoding") String acceptEncoding,
                            @PathParam("id") final String id) {
        if (!fileStore.fileExists(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final boolean decompress = acceptEncoding == null ||
                !(acceptEncoding.contains("bzip2") || acceptEncoding.contains("gzip"));

        final StreamingOutput stream = os -> fileStore.getFile(id, os, decompress);
        return Response.ok(stream).build();
    }

    /**
     * Adds metadata to an existing file
     *
     * @param id       id of file
     * @param metadata json structure containing metadata
     * @return a http 200 ok response
     */
    @POST
    @Path(FileStoreServiceConstants.FILE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Stopwatch
    public Response addMetadata(@PathParam("id") final String id,
                                String metadata) {
        fileStore.addMetaData(id, metadata);
        return Response.ok().build();
    }

    /**
     * Retrieves a list of file attributes
     *
     * @param metadata metadata to select with
     * @return a http 200 ok containing a json list with file attributes
     * @throws JSONBException on error deserializing the file attributes list
     */
    @POST
    @Path(FileStoreServiceConstants.FILES_COLLECTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Stopwatch
    public Response getFilesFromMetadata(final String metadata)
            throws JSONBException {
        List<FileAttributes> fileAttributesList = fileStore
                .getFilesFromMetadata(metadata);
        return Response.ok(jsonbContext.marshall(fileAttributesList))
                .build();
    }

    @DELETE
    @Path(FileStoreServiceConstants.FILE)
    @Stopwatch
    public Response deleteFile(@PathParam("id") final String id) {
        if (!fileStore.fileExists(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        fileStore.deleteFile(id);
        return Response.ok().build();
    }

    /**
     * Retrieves file attributes
     *
     * @param id ID of file
     * @return a HTTP 200 OK response with file attributes as JSON entity,
     * a HTTP 400 BAD_REQUEST response in case the file id is not a number,
     * a HTTP 404 NOT_FOUND response in case the file attributes could not be found,
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     * @throws JSONBException on error while marshalling the file attributes
     */
    @GET
    @Path(FileStoreServiceConstants.FILE_ATTRIBUTES)
    @Produces(MediaType.APPLICATION_JSON)
    @Stopwatch
    public Response getAttributes(@PathParam("id") final String id) throws JSONBException {
        try {
            final Optional<FileAttributes> fileAttributes = fileStore.getFileAttributes(id);
            if (fileAttributes.isPresent()) {
                return Response.ok().entity(jsonbContext.marshall(fileAttributes.get())).build();
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Retrieves the size in bytes of a file contained within the file-store
     * <p>
     * If HTTP header Accept-Encoding contains bzip2 or gzip and the binary file is
     * formatted using the corresponding compression algorithm, the size of its
     * compressed form is returned, otherwise the decompressed size is returned.
     *
     * @param id             ID of file
     * @param acceptEncoding value of Accept-Encoding header
     * @return a HTTP 200 OK response with byte size as entity
     * a HTTP 400 BAD_REQUEST response in case the file id is not a number
     * a HTTP 404 NOT_FOUND response in case the file attributes could not be found
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @Path(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
    @Stopwatch
    public Response getByteSize(@HeaderParam("Accept-Encoding") String acceptEncoding,
                                @PathParam("id") final String id) {
        try {
            final boolean decompressed = acceptEncoding == null ||
                    !(acceptEncoding.contains("bzip2") || acceptEncoding.contains("gzip"));
            final long byteSize = fileStore.getByteSize(id, decompressed);
            return Response.ok().entity(byteSize).build();
        } catch (EJBException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Cleans up up the filestore by purging deprecated files
     *
     * @return a HTTP 200 OK response
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @DELETE
    @Path(FileStoreServiceConstants.FILES_COLLECTION)
    @Stopwatch
    public Response clean() {
        fileStore.purge();
        return Response.ok().build();
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private URI getUri(UriInfo uriInfo, String fileId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(fileId).build();
    }
}
