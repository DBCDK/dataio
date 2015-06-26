package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@value dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants#FILES_COLLECTION}' entry point
 */
@Stateless
@javax.ws.rs.Path("/")
public class FilesBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesBean.class);

    @EJB
    FileStoreBean fileStore;

    /**
     * Creates new file in file-store containing data from the given data stream
     *
     * @param uriInfo application and request URI information
     * @param dataStream binary data to be written to file
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws IOException if an I/O error occurs.
     */
    @POST
    @javax.ws.rs.Path(FileStoreServiceConstants.FILES_COLLECTION)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
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
     * Retrieves content of file contained in file-store as binary data stream
     * @param id ID of file
     * @return a HTTP 200 OK response with file data as binary stream
     *         a HTTP 404 NOT_FOUND response in case the id could not be found
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @javax.ws.rs.Path(FileStoreServiceConstants.FILE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@PathParam("id") final String id) {
        LOGGER.trace("getFile() method called with file ID {}", id);

        if (!fileStore.fileExists(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) {
                fileStore.getFile(id, os);
            }
        };

        return Response.ok(stream).build();
    }

    /**
     * Retrieves the size of a file contained within the file attributes belonging to a file
     * @param id ID of file
     * @return a HTTP 200 OK response with byte size as entity
     *         a HTTP 400 BAD_REQUEST response in case the file id is not a number
     *         a HTTP 404 NOT_FOUND response in case the file attributes could not be found
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @javax.ws.rs.Path(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
    public Response getByteSize(@PathParam("id") final String id) {
        LOGGER.trace("getFileAttributes() method called with file ID {}", id);
        try {
            final long byteSize = fileStore.getByteSize(id);
            return Response.ok().entity(byteSize).build();
        } catch (EJBException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private URI getUri(UriInfo uriInfo, String fileId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(fileId).build();
    }
}
