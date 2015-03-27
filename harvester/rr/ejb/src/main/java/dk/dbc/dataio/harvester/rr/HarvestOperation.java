package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.harvester.types.DataContainer;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HarvestOperation {
    public static final int COMMUNITY_LIBRARY_NUMBER = 191919;

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    private final RawRepoHarvesterConfig.Entry config;
    private final HarvesterJobBuilderFactory harvesterJobBuilderFactory;
    private final Map<Integer, HarvesterJobBuilder> harvesterJobBuilders = new HashMap<>();
    private final DocumentBuilder documentBuilder;
    private final Transformer transformer;
    private final RawRepoConnector rawRepoConnector;

    public HarvestOperation(RawRepoHarvesterConfig.Entry config, HarvesterJobBuilderFactory harvesterJobBuilderFactory)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.harvesterJobBuilderFactory = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilderFactory, "harvesterJobBuilderFactory");
        documentBuilder = getDocumentBuilder();
        transformer = getTransformer();
        rawRepoConnector = getRawRepoConnector(config.getResource());
    }

    public int execute() throws HarvesterException {
        int itemsHarvested = 0;
        QueueJob nextQueuedItem = getNextQueuedItem();
        while (nextQueuedItem != null) {
            LOGGER.info("{} ready for harvesting", nextQueuedItem);
            final RecordId queuedRecordId = nextQueuedItem.getJob();
            if (queuedRecordId.getAgencyId() != COMMUNITY_LIBRARY_NUMBER) {
                try {
                    final HarvesterXmlRecord harvesterRecord = getHarvesterRecordForQueuedRecord(queuedRecordId);
                    getHarvesterJobBuilder(queuedRecordId.getAgencyId()).addHarvesterRecord(harvesterRecord);
                } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                    LOGGER.error("Marking queue item {} as failure", nextQueuedItem, e);
                    markAsFailure(nextQueuedItem, e.getMessage());
                }

                if (++itemsHarvested == config.getBatchSize()) {
                    break;
                }
            } else {
                LOGGER.debug("Skipped {}", queuedRecordId);
            }
            nextQueuedItem = getNextQueuedItem();
        }
        flushHarvesterJobBuilders();
        LOGGER.info("Harvested {} items from {} queue", itemsHarvested, config.getConsumerId());
        return itemsHarvested;
    }

    JobSpecification getJobSpecificationTemplate(int agencyId) {
        return new JobSpecification("xml", config.getFormat(agencyId), "utf8", config.getDestination(), agencyId,
                "placeholder", "placeholder", "placeholder", "placeholder");
    }

    private HarvesterJobBuilder getHarvesterJobBuilder(int agencyId) throws HarvesterException {
        if (!harvesterJobBuilders.containsKey(agencyId)) {
            harvesterJobBuilders.put(agencyId, harvesterJobBuilderFactory
                    .newHarvesterJobBuilder(getJobSpecificationTemplate(agencyId)));
        }
        return harvesterJobBuilders.get(agencyId);
    }

    private void flushHarvesterJobBuilders() throws HarvesterException {
        try {
            for (Map.Entry<Integer, HarvesterJobBuilder> entry : harvesterJobBuilders.entrySet()) {
                try {
                    entry.getValue().build();
                } catch (HarvesterException e) {
                    LOGGER.error("Failed to flush HarvesterJobBuilder for {}", entry.getKey(), e);
                    throw e;
                }
            }
        } finally {
            closeHarvesterJobBuilders();
        }
    }

    private void closeHarvesterJobBuilders() {
        for (Map.Entry<Integer, HarvesterJobBuilder> entry : harvesterJobBuilders.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                LOGGER.warn("Unable to close HarvesterJobBuilder for {}", entry.getKey());
            }
        }
        harvesterJobBuilders.clear();
    }

    /* Returns next rawrepo queue item (or null if queue is empty)
     */
    private QueueJob getNextQueuedItem() throws HarvesterException {
        try {
            return rawRepoConnector.dequeue(config.getConsumerId());
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException(e);
        }
    }

    /* Fetches rawrepo record associated with given record ID and adds
       its content to a new MARC exchange collection.
       Returns data container harvester record containing MARC exchange collection as data
       and record creation date as supplementary data.
     */
    private HarvesterXmlRecord getHarvesterRecordForQueuedRecord(RecordId recordId) throws HarvesterException {
        final Map<String, Record> records;
        try {
            records = rawRepoConnector.fetchRecordCollection(recordId);
        } catch (SQLException | RawRepoException | MarcXMergerException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " + recordId.toString(), e);
        }
        LOGGER.debug("Fetched rawrepo collection<{}> for {}", records.values(), recordId);
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty rawrepo collection returned for " + recordId.toString());
        }
        if (!records.containsKey(recordId.getBibliographicRecordId())) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s was not found in returned collection", recordId.toString()));
        }

        final MarcExchangeCollection marcExchangeCollection = getMarcExchangeCollection(recordId, records);
        final DataContainer dataContainer = new DataContainer(documentBuilder, transformer);
        dataContainer.setCreationDate(getRecordCreationDate(recordId, records));
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());
        return dataContainer;
    }

    private MarcExchangeCollection getMarcExchangeCollection(RecordId recordId, Map<String, Record> records)
            throws HarvesterException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(documentBuilder, transformer);
        if (config.includeRelations()) {
            for (Record record : records.values()) {
                LOGGER.debug("Adding {} member to {} marc exchange collection", record.getId(), recordId);
                marcExchangeCollection.addMember(getRecordContent(record));
            }
        } else {
            marcExchangeCollection.addMember(getRecordContent(recordId, records));
        }
        return marcExchangeCollection;
    }

    private byte[] getRecordContent(RecordId recordId, Map<String, Record> records) throws HarvesterInvalidRecordException {
        try {
            return records.get(recordId.getBibliographicRecordId()).getContent();
        } catch (NullPointerException e) {
             throw new HarvesterInvalidRecordException("Record content is null");
        }
    }

    private byte[] getRecordContent(Record record) throws HarvesterInvalidRecordException {
        try {
            return record.getContent();
        } catch (NullPointerException e) {
            throw new HarvesterInvalidRecordException("Record content is null");
        }
    }

    private Date getRecordCreationDate(RecordId recordId, Map<String, Record> records) throws HarvesterInvalidRecordException {
        try {
            final Date created = records.get(recordId.getBibliographicRecordId()).getCreated();
            if (created == null) {
                throw new HarvesterInvalidRecordException("Record creation date is null");
            }
            return created;
        } catch (NullPointerException e) {
            throw new HarvesterInvalidRecordException("Record creation date is null");
        }
    }

    private void markAsFailure(QueueJob queuedItem, String errorMessage) throws HarvesterException {
        try {
            rawRepoConnector.queueFail(queuedItem, errorMessage);
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as failure", e);
        }
    }

    /* Stand-alone methods to enable easy override during testing */

    RawRepoConnector getRawRepoConnector(String dataSourceResourceName)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        return new RawRepoConnector(dataSourceResourceName);
    }

    private DocumentBuilder getDocumentBuilder() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Transformer getTransformer() {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            return transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
