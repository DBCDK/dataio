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

package dk.dbc.dataio.harvester.utils.harvesterjobbuilder;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractHarvesterJobBuilder implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHarvesterJobBuilder.class);
    protected final JobSpecification jobSpecificationTemplate;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final BinaryFile tmpFile;
    private final OutputStream tmpFileOutputStream;
    protected int recordsAdded = 0;

    /**
     * Class constructor
     * @param binaryFileStore binaryFileStore implementation for tmp file writing
     * @param fileStoreServiceConnector file-store service connector for datafile uploads
     * @param jobStoreServiceConnector job-store service connector for job creation
     * @param jobSpecificationTemplate job specification template
     * @throws NullPointerException if given null-valued argument
     * @throws HarvesterException on failure to create harvester data file
     * backed by temporary binary file
     */
    public AbstractHarvesterJobBuilder(BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector, JobStoreServiceConnector jobStoreServiceConnector, JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.jobSpecificationTemplate = InvariantUtil.checkNotNullOrThrow(jobSpecificationTemplate, "jobSpecificationTemplate");
        this.tmpFile = createTmpFile();
        this.tmpFileOutputStream = openForWriting(tmpFile);
    }

    /**
     * Uploads harvester data file, if non-empty, to the file-store
     * and creates job in the job-store referencing the uploaded file
     * @return Optional containing job info snapshot for created job, or empty if no job was created
     * @throws HarvesterException on failure to upload to file-store or on failure to create job in job-store
     */
    public Optional<JobInfoSnapshot> build() throws HarvesterException {
        closeTmpFile();
        if (recordsAdded > 0) {
            final Optional<String> uploadedFileId = uploadToFileStore();
            if (uploadedFileId.isPresent()) {
                return Optional.of(createInJobStore(uploadedFileId.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * Adds given Addi record to harvester data file
     * @param record Addi record to add
     * @throws HarvesterException if unable to add record
     */
    public void addRecord(AddiRecord record) throws HarvesterException {
        try {
            tmpFileOutputStream.write(record.getBytes());
        } catch (IOException e) {
            throw new HarvesterException("Error writing harvester record to tmp", e);
        }
        recordsAdded++;
    }

    /** @return number of records added */
    public int getRecordsAdded() {
        return recordsAdded;
    }

    /**
     * Closes and deletes temporary file
     * @throws HarvesterException if unable to close temporary file
     */
    @Override
    public void close() throws HarvesterException {
        closeTmpFile();
        deleteTmpFile();
    }

    /* Creates harvester specific job specification for given file id */
    protected abstract JobSpecification createJobSpecification(String fileId) throws HarvesterException;

    /*
     * private methods
     */

    private void removeFromFileStore(String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    /* Returns temporary binary file for harvested data */
    private BinaryFile createTmpFile() throws HarvesterException {
        try {
            return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
        } catch (RuntimeException e) {
            throw new HarvesterException("Error creating tmp file", e);
        }
    }

    private void deleteTmpFile() {
        try {
            tmpFile.delete();
        } catch (IllegalStateException e) {
            // We don't want to rollback the entire transaction, just
            // because we can't remove the temporary file.
            LOGGER.warn("Unable to delete temporary file {}", tmpFile.getPath(), e);
        }
    }

    private void closeTmpFile() throws HarvesterException {
        try {
            tmpFileOutputStream.close();
        } catch (IOException e) {
            throw new HarvesterException("Error while closing tmp file", e);
        }
    }

    private OutputStream openForWriting(BinaryFile binaryFile) throws HarvesterException {
        try {
            return binaryFile.openOutputStream();
        } catch (RuntimeException e) {
            throw new HarvesterException("Error opening tmp file for writing", e);
        }
    }

    /* Uploads harvester data file to the file-store */
    private Optional<String> uploadToFileStore() throws HarvesterException {
        String fileId = null;
        try (InputStream is = tmpFile.openInputStream()) {
            fileId = fileStoreServiceConnector.addFile(is);
            LOGGER.info("Added file with ID {} to file-store", fileId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            throw new HarvesterException("Unable to add file to file-store", e);
        } catch (IOException e) {
            LOGGER.warn("Unable to close tmp file InputStream");
        }
        return Optional.ofNullable(fileId);
    }

    /* Creates new job in the job-store */
    private JobInfoSnapshot createInJobStore(String fileId) throws HarvesterException {
        final JobSpecification jobSpecification = createJobSpecification(fileId);
        try {
            final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpecification, true, 0));
            LOGGER.info("Created job in job-store with ID {}", jobInfoSnapshot.getJobId());
            return jobInfoSnapshot;
        } catch (JobStoreServiceConnectorException | RuntimeException e) {
            boolean doRemoveFromFileStore = true;
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                JobStoreServiceConnectorUnexpectedStatusCodeException statusCodeException
                        = (JobStoreServiceConnectorUnexpectedStatusCodeException) e;
                final JobError jobError = statusCodeException.getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
                if (statusCodeException.getStatusCode() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                    doRemoveFromFileStore = false;
                }
            }
            if (doRemoveFromFileStore) {
                removeFromFileStore(fileId);
            }
            throw new HarvesterException("Unable to create job in job-store", e);
        }
    }
}
