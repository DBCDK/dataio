package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.DataContainer;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.utils.jobstore.HarvesterJobBuilder;
import dk.dbc.dataio.harvester.utils.jobstore.HarvesterJobBuilderFactoryBean;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnectorBean;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This stateless Enterprise Java Bean (EJB) handles an actual RawRepo-to-datawell harvest
 */
@Singleton
public class HarvesterBean {
    public static final String RAW_REPO_CONSUMER_ID = "broend-sync";
    public static final int COMMUNITY_LIBRARY_NUMBER = 870970;
    public static final JobSpecification COMMUNITY_RECORDS_JOB_SPECIFICATION_TEMPLATE =
            new JobSpecification("xml", "basis", "utf8", "broend3", COMMUNITY_LIBRARY_NUMBER, "placeholder", "placeholder", "placeholder", "placeholder");
    public static final JobSpecification LOCAL_RECORDS_JOB_SPECIFICATION_TEMPLATE =
            new JobSpecification("xml", "katalog", "utf8", "broend3", 700000, "placeholder", "placeholder", "placeholder", "placeholder");

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);
    static final int HARVEST_BATCH_SIZE = 10000;

    @EJB
    RawRepoConnectorBean rawRepoConnector;

    @EJB
    HarvesterJobBuilderFactoryBean harvesterJobBuilderFactoryBean;

    @Resource
    SessionContext sessionContext;

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
     * Executes harvest operation in batches (each batch in its own transactional
     * scope to avoid tearing down any controlling timers) creating the
     * corresponding jobs in the job-store if data is retrieved. Community records and
     * non-community records are handled as separate jobs.
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void harvest() throws IllegalStateException, HarvesterException {
        while (sessionContext.getBusinessObject(HarvesterBean.class).harvestBatch() == HARVEST_BATCH_SIZE)
            continue;
    }

    /**
     * Executes harvest batch operation
     * @return number of items harvested
     * @throws IllegalStateException on low-level binary file operation failure
     * @throws HarvesterException on failure to complete harvest operation
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int harvestBatch() throws IllegalStateException, HarvesterException {
        int itemsHarvested = 0;
        try (
            final HarvesterJobBuilder communityRecordsJobBuilder = harvesterJobBuilderFactoryBean.newHarvesterJobBuilder(COMMUNITY_RECORDS_JOB_SPECIFICATION_TEMPLATE);
            final HarvesterJobBuilder localRecordsJobBuilder = harvesterJobBuilderFactoryBean.newHarvesterJobBuilder(LOCAL_RECORDS_JOB_SPECIFICATION_TEMPLATE)
        ) {
            QueueJob nextQueuedItem = getNextQueuedItem();
            while (nextQueuedItem != null) {
                LOGGER.info("{} ready for harvesting", nextQueuedItem);
                try {
                    final RecordId queuedRecordId = nextQueuedItem.getJob();
                    final HarvesterXmlRecord harvesterRecord = getHarvesterRecordForQueuedRecord(queuedRecordId);
                    switch (queuedRecordId.getAgencyId()) {
                        case COMMUNITY_LIBRARY_NUMBER:
                            communityRecordsJobBuilder.addHarvesterRecord(harvesterRecord);
                            break;
                        default:
                            localRecordsJobBuilder.addHarvesterRecord(harvesterRecord);
                            break;
                    }
                    markAsSuccess(nextQueuedItem);
                } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                    LOGGER.error("Marking queue item {} as failure", nextQueuedItem, e);
                    markAsFailure(nextQueuedItem, e.getMessage());
                }

                if (++itemsHarvested == HARVEST_BATCH_SIZE) {
                    nextQueuedItem = null;
                } else {
                    nextQueuedItem = getNextQueuedItem();
                }
            }
            communityRecordsJobBuilder.build();
            localRecordsJobBuilder.build();

            LOGGER.info("Harvested {} records from {} queue",
                    communityRecordsJobBuilder.getRecordsAdded() + localRecordsJobBuilder.getRecordsAdded(), RAW_REPO_CONSUMER_ID);
        }
        return itemsHarvested;
    }

    /* Returns next rawrepo queue item (or null if queue is empty)
     */
    private QueueJob getNextQueuedItem() throws HarvesterException {
        try {
            return rawRepoConnector.dequeue(RAW_REPO_CONSUMER_ID);
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException(e);
        }
    }

    /* Fetches rawrepo record collection associated with given record ID and adds
       its content to a new MARC exchange collection.
       Returns data container harvester record containing MARC exchange collection as data
       and record creation date as supplementary data.
     */
    private HarvesterXmlRecord getHarvesterRecordForQueuedRecord(RecordId recordId) throws HarvesterException {
        final Set<Record> records;
        try {
            records = asSet(rawRepoConnector.fetchRecordCollection(recordId));
        } catch (SQLException | RawRepoException | MarcXMergerException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " + recordId.toString(), e);
        }
        LOGGER.debug("Fetched rawrepo collection<{}> for {}", records, recordId);
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(documentBuilder, transformer);
        for (Record record : records) {
            LOGGER.debug("Adding {} member to {} marc exchange collection", record.getId(), recordId);
            marcExchangeCollection.addMember(getRecordContent(record));
        }
        final DataContainer dataContainer = new DataContainer(documentBuilder, transformer);
        dataContainer.setCreationDate(getRecordCreationDate(recordId, records));
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());
        return dataContainer;
    }

    private Set<Record> asSet(Map<String, Record> recordMap) {
        final Set<Record> records = new HashSet<>(recordMap.size());
        for (Record record : recordMap.values()) {
            records.add(record);
        }
        return records;
    }

    private byte[] getRecordContent(Record record) throws HarvesterInvalidRecordException {
        try {
            return record.getContent();
        } catch (NullPointerException e) {
             throw new HarvesterInvalidRecordException("Record content is null");
        }
    }

    private Date getRecordCreationDate(RecordId recordId, Set<Record> records) {
        for (Record record : records) {
            if (record.getId().equals(recordId)) {
                return record.getCreated();
            }
        }
        return null;
    }

    private void markAsSuccess(QueueJob queuedItem) throws HarvesterException {
        try {
            rawRepoConnector.queueSuccess(queuedItem);
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as success", e);
        }
    }

    private void markAsFailure(QueueJob queuedItem, String errorMessage) throws HarvesterException {
        try {
            rawRepoConnector.queueFail(queuedItem, errorMessage);
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as failure", e);
        }
    }
}
