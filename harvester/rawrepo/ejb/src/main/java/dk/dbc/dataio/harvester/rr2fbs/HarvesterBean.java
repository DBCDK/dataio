package dk.dbc.dataio.harvester.rr2fbs;

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
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

/**
 * This stateless Enterprise Java Bean (EJB) handles an actual RawRepo-to-FBS harvest
 */
@Singleton
public class HarvesterBean {
    public static final String RAW_REPO_CONSUMER_ID = "fbs-sync";
    public static final JobSpecification JOB_SPECIFICATION_TEMPLATE =
            new JobSpecification("xml", "katalog", "utf8", "fbs", 42, "placeholder", "placeholder", "placeholder", "placeholder");

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
     * tearing down any controlling timers) creating the corresponding jobs
     * in the job-store if data is retrieved.
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void harvest() throws IllegalStateException, HarvesterException {
        try (
            final HarvesterJobBuilder harvesterJobBuilder = new HarvesterJobBuilder(JOB_SPECIFICATION_TEMPLATE);
        ) {
            QueueJob nextQueuedItem = getNextQueuedItem();
            while (nextQueuedItem != null) {
                LOGGER.info("{} ready for harvesting", nextQueuedItem);
                try {
                    final RecordId queuedRecordId = nextQueuedItem.getJob();
                    final MarcExchangeCollection harvesterRecord = getHarvesterRecordForQueuedRecord(queuedRecordId);
                    harvesterJobBuilder.addHarvesterRecord(harvesterRecord);
                    markAsSuccess(nextQueuedItem);
                } catch (HarvesterInvalidRecordException e) {
                    LOGGER.error("Marking queue item {} as failure", nextQueuedItem, e);
                    markAsFailure(nextQueuedItem, e.getMessage());
                }
                nextQueuedItem = getNextQueuedItem();
            }
            harvesterJobBuilder.build();

            LOGGER.info("Harvested {} records from {} queue", harvesterJobBuilder.recordsAdded, RAW_REPO_CONSUMER_ID);
        }
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

    /* Fetches rawrepo record associated with given record ID and adds
       its content to a new MARC exchange collection.
       Returns harvester record.
     */
    private MarcExchangeCollection getHarvesterRecordForQueuedRecord(RecordId recordId) throws HarvesterException {
        final Record record;
        try {
            record = rawRepoConnector.fetchRecord(recordId);
            LOGGER.debug("Fetched rawrepo record<{}>", record);
        } catch (SQLException e) {
            throw new HarvesterException("Unable to fetch record for " + recordId.toString(), e);
        }
        final MarcExchangeCollection harvesterRecord = new MarcExchangeCollection(documentBuilder, transformer);
        harvesterRecord.addMember(record.getContent());
        return harvesterRecord;
    }

    private void markAsSuccess(QueueJob queuedItem) throws HarvesterException {
        try {
            rawRepoConnector.queueSuccess(queuedItem);
        } catch (SQLException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as success", e);
        }
    }

    private void markAsFailure(QueueJob queuedItem, String errorMessage) throws HarvesterException {
        try {
            rawRepoConnector.queueFail(queuedItem, errorMessage);
        } catch (SQLException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as failure", e);
        }
    }

    /**
     * Helper class for job-store job creation
     */
    public class HarvesterJobBuilder implements AutoCloseable {
        private final BinaryFile tmpFile;
        private final OutputStream tmpFileOutputStream;
        private final HarvesterXmlDataFile dataFile;
        private final JobSpecification jobSpecificationTemplate;
        private int recordsAdded = 0;

        /**
         * Class constructor
         * @param jobSpecificationTemplate job specification template
         * @throws HarvesterException on failure to create harvester data file
         * backed by temporary binary file
         */
        public HarvesterJobBuilder(JobSpecification jobSpecificationTemplate) throws HarvesterException {
            this.tmpFile = getTmpFile();
            this.tmpFileOutputStream = tmpFile.openOutputStream();
            this.dataFile = new HarvesterXmlDataFile(StandardCharsets.UTF_8, tmpFileOutputStream);
            this.jobSpecificationTemplate = jobSpecificationTemplate;
        }

        /**
         * Adds given MarcExchangeCollection to harvester data file provided that
         * the collection in question contains record members
         * @param marcExchangeCollection harvester record as MarcExchangeCollection
         * @throws HarvesterException if unable to add harvester record
         */
        public void addHarvesterRecord(MarcExchangeCollection marcExchangeCollection) throws HarvesterException {
            dataFile.addRecord(marcExchangeCollection);
            recordsAdded++;
        }

        /**
         * Uploads harvester data file, as long as the data file contains
         * any records, to the file-store and creates job in the job-store
         * referencing the uploaded file
         * @return job info for created job, or null if no job was created
         * @throws HarvesterException on failure to upload to file-store or on
         * failure to create job in job-store
         */
        public JobInfo build() throws HarvesterException {
            dataFile.close();
            try {
                closeTmpFileOutputStream();
            } catch (IllegalStateException e) {
                throw new HarvesterException(e);
            }
            if (recordsAdded > 0) {
                final String fileId = uploadToFileStore();
                if (fileId != null) {
                    return createJobInJobStore(fileId);
                }
            }
            return null;
        }

        /**
         * Closes associated file resources and deletes temporary file
         * @throws IllegalStateException if unable to close temporary file output stream
         */
        @Override
        public void close() throws IllegalStateException {
            closeTmpFileOutputStream();
            deleteTmpFile();
        }

        private void closeTmpFileOutputStream() throws IllegalStateException {
            try {
                tmpFileOutputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
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

        /* Returns temporary binary file for harvested data
        */
        private BinaryFile getTmpFile() {
            return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
        }

        /* Uploads harvester data file to the file-store
         */
        private String uploadToFileStore() throws HarvesterException {
            String fileId = null;
            try (final InputStream is = tmpFile.openInputStream()) {
                fileId = fileStoreServiceConnector.addFile(is);
                LOGGER.info("Added file with ID {} to file-store", fileId);
            } catch (FileStoreServiceConnectorException e) {
                throw new HarvesterException("Unable to add file to file-store", e);
            } catch (IOException e) {
                LOGGER.warn("Unable to close tmp file InputStream");
            }
            return fileId;
        }

        /* Creates new job in the job-store
        */
        private JobInfo createJobInJobStore(String fileId) throws HarvesterException {
            final JobSpecification jobSpecification = createJobSpecification(fileId);
            try {
                final JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpecification);
                LOGGER.info("Created job in job-store with ID {}", jobInfo.getJobId());
                return jobInfo;
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
            return new JobSpecification(
                    jobSpecificationTemplate.getPackaging(),
                    jobSpecificationTemplate.getFormat(),
                    jobSpecificationTemplate.getCharset(),
                    jobSpecificationTemplate.getDestination(),
                    jobSpecificationTemplate.getSubmitterId(),
                    "", "", "", fileStoreUrn.toString());
        }
    }
}
