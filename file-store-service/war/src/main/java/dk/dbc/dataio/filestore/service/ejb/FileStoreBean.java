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

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * This stateless Enterprise Java Bean (EJB) class handles storing and retrieval of
 * file data
 */
@Stateless
public class FileStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileStoreBean.class);

    @PersistenceContext
    EntityManager entityManager;

    @EJB
    BinaryFileStoreBean binaryFileStore;

    /**
     * Adds content of given input stream as file in store
     * @param dataSource input stream of bytes to be written
     * @return ID of generated file
     * @throws NullPointerException if given null-valued dataSource argument
     * @throws IllegalStateException on general failure to write data
     */
    @Stopwatch
    public String addFile(InputStream dataSource) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(dataSource, "dataSource");
        final Path location = getCurrentLocation();
        FileAttributes fileAttributes = new FileAttributes(new Date(), location);
        fileAttributes = entityManager.merge(fileAttributes);
        entityManager.flush();

        final ByteCountingInputStream wrappedDataSource = new ByteCountingInputStream(dataSource);
        final Path path = location.resolve(String.valueOf(fileAttributes.getId()));
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(path);
        if (binaryFile.exists()) {
            // FileAttributes could be created, but a dangling file with the
            // same id already exists in the file system.
            LOGGER.warn("Deleted dangling file {}", binaryFile.getPath());
            binaryFile.delete();
        }
        binaryFile.write(wrappedDataSource);

        // Set the number of bytes read on file attributes
        fileAttributes.setByteSize(wrappedDataSource.getBytesRead());

        LOGGER.info("Wrote file {}", path.toString());
        return String.valueOf(fileAttributes.getId());
    }

    /**
     * Appends content to existing file in store
     * @param id id of existing file
     * @param bytes bytes to be appended
     * @throws IllegalStateException on general failure to append data
     */
    public void appendToFile(String id, byte[] bytes) {
        if (bytes != null) {
            final FileAttributes fileAttributes = getFileAttributesOrThrow(id);
            binaryFileStore.getBinaryFile(fileAttributes.getLocation().resolve(id)).append(bytes);
            fileAttributes.setByteSize(fileAttributes.getByteSize() + bytes.length);
        }
    }

    /**
     * Adds metadata to an existing file
     *
     * @param id id of file
     * @param metadata json structure containing metadata
     * @return updated file attributes
     * @throws NullPointerException if given null-valued id argument
     * @throws EJBException if no file attributes can be found for given file ID
     */
    @Stopwatch
    public FileAttributes addMetaData(String id, String metadata)
            throws NullPointerException, EJBException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(id, "id");
        final FileAttributes fileAttributes = getFileAttributesOrThrow(id);
        fileAttributes.setMetadata(metadata);
        return fileAttributes;
    }

    /**
     * Retrieves file content from store into given output stream
     * @param fileId ID of file
     * @param dataDestination output stream to which bytes are written
     * @throws NullPointerException if given null-valued fileId or dataDestination argument
     * @throws IllegalArgumentException if given invalid formatted fileId argument
     * @throws IllegalStateException on general failure to read data
     * @throws EJBException if no file attributes can be found for given file ID
     */
    @Stopwatch
    public void getFile(String fileId, OutputStream dataDestination)
            throws NullPointerException, IllegalArgumentException, IllegalStateException, EJBException {
        InvariantUtil.checkNotNullOrThrow(dataDestination, "dataDestination");
        final FileAttributes fileAttributes = getFileAttributesOrThrow(fileId);
        final BinaryFile binaryFile = getBinaryFile(fileAttributes);
        binaryFile.read(dataDestination);
    }

    /**
     * Retrieves a list of file attributes based on a postgresql json operator select
     * @param metadata metadata to select with
     * @return list of file attributes
     */
    @Stopwatch
    public List<FileAttributes> getFilesFromMetadata(final String metadata) {
        final TypedQuery<FileAttributes> query = entityManager
            .createNamedQuery(FileAttributes.GET_FILES_FROM_METADATA,
            FileAttributes.class)
            .setParameter(1, metadata);
        return query.getResultList();
    }

    /**
     * Deletes file content and attributes from store
     * @param fileId ID of file
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given invalid formatted fileId argument
     * @throws IllegalStateException on general failure to delete binary file
     * @throws EJBException if no file attributes can be found for given file ID
     */
    @Stopwatch
    public void deleteFile(String fileId) {
        LOGGER.info("Deleting file with ID '{}'", fileId);
        final FileAttributes fileAttributes = getFileAttributesOrThrow(fileId);
        final BinaryFile binaryFile = getBinaryFile(fileAttributes);
        binaryFile.delete();
        entityManager.remove(fileAttributes);
    }

    /**
     * Returns file attributes for specific file
     * @param fileId ID of file
     * @return file attributes, empty if file does not exist
     */
    @Stopwatch
    public Optional<FileAttributes> getFileAttributes(String fileId)
            throws NullPointerException, IllegalArgumentException {
        final long id = fileIdToLong(fileId);
        return Optional.ofNullable(entityManager.find(FileAttributes.class, id));
    }

    /**
     * Retrieves the byte size of a file specified through the file id given as input
     * @param fileId ID of file
     * @return the byte size of the file
     *
     * @throws NullPointerException if given null-valued fileId argument
     * @throws IllegalArgumentException if given invalid formatted fileId argument
     * @throws EJBException if no file attributes can be found for given file ID
     */
    @Stopwatch
    public long getByteSize(String fileId) throws NullPointerException, IllegalArgumentException, EJBException {
        return getFileAttributesOrThrow(fileId).getByteSize();
    }

    /**
     * Tests existence in store of file identified by given ID
     * @param fileId ID of file
     * @return true if file exists, false if not
     * @throws NullPointerException if given null-valued fileId argument
     */
    @Stopwatch
    public boolean fileExists(String fileId) throws NullPointerException {
        try {
            return getFileAttributes(fileId).isPresent();
        } catch (IllegalArgumentException e) {
            LOGGER.warn(e.getMessage());
            return false;
        }
    }

    private Path getCurrentLocation() {
        final Calendar now = Calendar.getInstance();
        final int year = now.get(Calendar.YEAR);
        final int month = now.get(Calendar.MONTH); // Note: zero based!
        final int day = now.get(Calendar.DAY_OF_MONTH);
        return Paths.get(String.format("%d/%02d/%02d", year, month + 1, day));
    }

    private FileAttributes getFileAttributesOrThrow(String fileId) throws NullPointerException, IllegalArgumentException, EJBException {
        final Optional<FileAttributes> fileAttributes = getFileAttributes(fileId);
        if (!fileAttributes.isPresent()) {
            throw new EJBException(String.format("Trying to retrieve attributes for non-existing file with ID '%s'", fileId));
        }
        return fileAttributes.get();
    }

    private long fileIdToLong(String fileId) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
        try {
            return Long.parseLong(fileId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid file ID '%s'", fileId));
        }
    }

    private BinaryFile getBinaryFile(FileAttributes fileAttributes) {
        final Path path = fileAttributes.getLocation().resolve(String.valueOf(fileAttributes.getId()));
        return binaryFileStore.getBinaryFile(path);
    }
}
