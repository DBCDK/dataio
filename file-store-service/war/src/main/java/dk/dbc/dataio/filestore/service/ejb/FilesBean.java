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

package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import java.util.List;

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
     * @param uriInfo application and request URI information
     * @param dataStream binary data to be written to file
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
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
     * Retrieves content of file contained in file-store as binary data stream
     * @param id ID of file
     * @return a HTTP 200 OK response with file data as binary stream
     *         a HTTP 404 NOT_FOUND response in case the id could not be found
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @Path(FileStoreServiceConstants.FILE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Stopwatch
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
     * Adds metadata to an existing file
     *
     * @param id id of file
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
     * Retrieves the size of a file contained within the file attributes belonging to a file
     * @param id ID of file
     * @return a HTTP 200 OK response with byte size as entity
     *         a HTTP 400 BAD_REQUEST response in case the file id is not a number
     *         a HTTP 404 NOT_FOUND response in case the file attributes could not be found
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     */
    @GET
    @Path(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
    @Stopwatch
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
