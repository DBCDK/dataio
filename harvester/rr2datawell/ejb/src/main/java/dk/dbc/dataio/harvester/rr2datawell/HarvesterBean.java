package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

/**
 * This stateless Enterprise Java Bean (EJB) handles an actual RawRepo harvest
 */
@Stateless
public class HarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    private static final String rawRepoConsumerId = "broend-sync";

    @EJB
    RawRepoConnectorBean rawRepoConnector;

    @EJB
    BinaryFileStoreBean binaryFileStore;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnector;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;

    /**
     * Executes harvest operation (in its own transactional scope to avoid
     * tearing down any controlling timers) creating the corresponding job
     * in the job-store if data is retrieved.
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to complete harvest operation
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void harvest() throws IllegalStateException, HarvesterException {
        final String fileId = createHarvesterDataFile();
        if (fileId != null) {
            final JobInfo job = createJob(fileId);
            LOGGER.info("Created job in job-store with ID {}", job.getJobId());
        }
    }

    /* Creates new HarvesterXmlDataFile backed by temporary binary file. If any rawrepo
       records can be added to the harvester data file, it is subsequently stored in the
       file-store.
       Returns ID of generated file-store file.
     */
    private String createHarvesterDataFile() throws HarvesterException {
        String fileId = null;
        final BinaryFile tmpFile = getTmpFile();
        try {
            final long recordsHarvested = addRecordsToHarvesterDataFile(tmpFile);
            LOGGER.info("Harvested {} records from {} queue", recordsHarvested, rawRepoConsumerId);
            if (recordsHarvested > 0) {
                try (final InputStream is = tmpFile.openInputStream()) {
                    fileId = fileStoreServiceConnector.addFile(is);
                    LOGGER.info("Added file with ID {} to file-store", fileId);
                } catch (FileStoreServiceConnectorException e) {
                    throw new HarvesterException("Unable to add file to file-store", e);
                } catch (IOException e) {
                    LOGGER.warn("Unable to close tmp file InputStream");
                }
            }
        } finally {
            try {
                tmpFile.delete();
            } catch (IllegalStateException e) {
                // We don't want to rollback the entire transaction, just
                // because we can't remove the temporary file.
                LOGGER.warn("Unable to delete temporary file {}", tmpFile.getPath(), e);
            }
        }
        return fileId;
    }

    /* Creates new HarvesterXmlDataFile backed by given temporary binary file if rawrepo queue is non-empty.
       All queued rawrepo records are subsequently added to the harvester data file.
       Returns number of rawrepo records added.
     */
    private long addRecordsToHarvesterDataFile(BinaryFile tmpFile) throws HarvesterException {
        long recordsAdded = 0;
        QueueJob nextQueuedItem = getNextQueuedItem();
        if (nextQueuedItem != null) {
            try (
                    final OutputStream os = tmpFile.openOutputStream();
                    final HarvesterXmlDataFile harvesterDataFile = new HarvesterXmlDataFile(StandardCharsets.UTF_8, os)) {
                while (nextQueuedItem != null) {
                    LOGGER.debug("{} ready for harvesting", nextQueuedItem);
                    rawRepoConnector.queueSuccess(nextQueuedItem);
                    harvesterDataFile.addRecord(getHarvesterRecordForQueuedItem(nextQueuedItem));
                    recordsAdded++;

                    nextQueuedItem = rawRepoConnector.dequeue(rawRepoConsumerId);
                }
            } catch (IOException e) {
                throw new HarvesterException("Unable to close tmp file OutputStream", e);
            } catch (SQLException e) {
                throw new HarvesterException("Unable to retrieve item from queue", e);
            }
        }
        return recordsAdded;
    }

    /* Returns next rawrepo queue item (or null if queue is empty)
     */
    private QueueJob getNextQueuedItem() throws HarvesterException {
        try {
            return rawRepoConnector.dequeue(rawRepoConsumerId);
        } catch (SQLException e) {
            throw new HarvesterException(e);
        }
    }

    /* Fetches rawrepo record associated with given queue item and adds
       its content to a new MARC Exchange collection harvester.
       Returns harvester record.
     */
    private HarvesterXmlRecord getHarvesterRecordForQueuedItem(QueueJob queueJob) throws SQLException, HarvesterException {
        final Record record = rawRepoConnector.fetchRecord(queueJob.getJob());
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(record.getContent());
        } catch (HarvesterInvalidRecordException e) {
            LOGGER.error("Invalid record {}", record, e);
        }
        return harvesterRecord;
    }

    /* Creates new job in the job-store
     */
    private JobInfo createJob(String fileId) throws HarvesterException {
        final JobSpecification jobSpecification = createJobSpecification(fileId);
        try {
            return jobStoreServiceConnector.createJob(jobSpecification);
        } catch (JobStoreServiceConnectorException e) {
            throw new HarvesterException("Unable to create job in job-store", e);
        }
    }

    /* Returns job specification for given file ID
     */
    private JobSpecification createJobSpecification(String fileId) throws HarvesterException {
        final String packaging = "xml";
        final String format = "basis";
        final String charset = "utf8";
        final String destination = "broend3";
        final long submitterId = 870970;
        final FileStoreUrn fileStoreUrn;
        try {
            fileStoreUrn = FileStoreUrn.create(fileId);
        } catch (URISyntaxException e) {
            throw new HarvesterException("Unable to create FileStoreUrn", e);
        }
        return new JobSpecification(packaging, format, charset, destination, submitterId, "", "", "", fileStoreUrn.toString());
    }

    /* Returns temporary binary file for harvested data
     */
    private BinaryFile getTmpFile() {
        return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
    }
}
