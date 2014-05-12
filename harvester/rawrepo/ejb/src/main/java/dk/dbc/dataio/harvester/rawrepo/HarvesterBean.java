package dk.dbc.dataio.harvester.rawrepo;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
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

    private static final String rawRepoConsumerId = "fbs-sync";
    private static final long submitterNumber = 42;

    @EJB
    RawRepoConnectorBean rawRepoConnector;

    @EJB
    BinaryFileStoreBean binaryFileStore;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnector;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void harvest() throws HarvesterException {
        final String fileId = createHarvesterDataFile();
        if (fileId != null) {
            createJob(fileId);
        }
    }

    QueueJob getNextQueuedItem() throws HarvesterException {
        try {
            return rawRepoConnector.dequeue(rawRepoConsumerId);
        } catch (SQLException e) {
            throw new HarvesterException(e);
        }
    }

    String createHarvesterDataFile() throws HarvesterException {
        String fileId = null;
        final BinaryFile tmpFile = getTmpFile();
        try {
            final long recordsHarvested = addRecordsToHarvesterDataFile(tmpFile);
            LOGGER.info("Harvested {} records from {} queue", recordsHarvested, rawRepoConsumerId);
            if (recordsHarvested > 0) {
                try (final InputStream is = tmpFile.openInputStream()) {
                    fileId = fileStoreServiceConnector.addFile(is);
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
                LOGGER.warn("Unable to delete temporary file {}", tmpFile.getPath(), e);
            }
        }
        return fileId;
    }

    long addRecordsToHarvesterDataFile(BinaryFile tmpFile) throws HarvesterException {
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

    HarvesterXmlRecord getHarvesterRecordForQueuedItem(QueueJob queueJob) throws SQLException, HarvesterException {
        final Record record = rawRepoConnector.fetchRecord(queueJob.getJob());
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection();
        try {
            harvesterRecord.addMember(record.getContent());
        } catch (HarvesterInvalidRecordException e) {
            LOGGER.error("Invalid record {}", record, e);
        }
        return harvesterRecord;
    }

    String createJob(String fileId) {
        String jobId = null;
        return jobId;
    }

    BinaryFile getTmpFile() {
        return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
    }
}
