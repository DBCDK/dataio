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
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.MarcExchangeRecordBinding;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayInputStream;
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
    public static final String RAW_REPO_CONSUMER_ID = "broend-sync";
    public static final int LIBRARY_NUMBER_870970 = 870970;
    public static final String PACKAGING = "xml";
    public static final String FORMAT = "basis";
    public static final String CHARSET = "utf8";
    public static final String DESTINATION = "broend3";

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB
    RawRepoConnectorBean rawRepoConnector;

    @EJB
    BinaryFileStoreBean binaryFileStore;

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnector;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;

    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    @PostConstruct
    @PostActivate
    public void init() throws EJBException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new EJBException(e);
        }
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new EJBException(e);
        }
    }

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
            LOGGER.info("Harvested {} records from {} queue", recordsHarvested, RAW_REPO_CONSUMER_ID);
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
                    final MarcExchangeCollection marcExchangeCollection = getHarvesterRecordForQueuedItem(nextQueuedItem);
                    if (marcExchangeCollection.getMemberCount() > 0) {
                        harvesterDataFile.addRecord(marcExchangeCollection);
                        recordsAdded++;
                    }
                    nextQueuedItem = getNextQueuedItem();
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
            return rawRepoConnector.dequeue(RAW_REPO_CONSUMER_ID);
        } catch (SQLException e) {
            throw new HarvesterException(e);
        }
    }

    /* Fetches rawrepo record associated with given queue item and adds
       its content to a new MARC Exchange collection harvester.
       Returns harvester record.
     */
    private MarcExchangeCollection getHarvesterRecordForQueuedItem(QueueJob queueJob) throws SQLException, HarvesterException {
        final Record record = rawRepoConnector.fetchRecord(queueJob.getJob());
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection(documentBuilder, transformer);
        try {
            final Document recordDoc = asDocument(record.getContent());
            final MarcExchangeRecordBinding recordBinding = new MarcExchangeRecordBinding(recordDoc);
            if (recordBinding.getLibrary() == LIBRARY_NUMBER_870970) {
                harvesterRecord.addMember(recordDoc);
            } else {
                LOGGER.warn("Skipped record with library number {}", recordBinding.getLibrary());
            }
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
        final FileStoreUrn fileStoreUrn;
        try {
            fileStoreUrn = FileStoreUrn.create(fileId);
        } catch (URISyntaxException e) {
            throw new HarvesterException("Unable to create FileStoreUrn", e);
        }
        return new JobSpecification(PACKAGING, FORMAT, CHARSET, DESTINATION, LIBRARY_NUMBER_870970, "", "", "", fileStoreUrn.toString());
    }

    /* Returns temporary binary file for harvested data
     */
    private BinaryFile getTmpFile() {
        return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
    }

    /* Parses given data as XML returning parsed document
     */
    private Document asDocument(byte[] data) throws HarvesterInvalidRecordException {
        try (final InputStream is = new ByteArrayInputStream(data)) {
            return documentBuilder.parse(is);
        } catch (IOException | SAXException e) {
            throw new HarvesterInvalidRecordException("Unable to parse record data as XML", e);
        } finally {
            documentBuilder.reset();
        }
    }
}
