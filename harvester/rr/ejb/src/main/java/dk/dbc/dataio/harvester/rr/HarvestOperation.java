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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.harvester.types.DataContainer;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
        rawRepoConnector = getRawRepoConnector(config);
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
                "placeholder", "placeholder", "placeholder", "placeholder", config.getType());
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
        try {
            final Map<String, Record> records;
            final String trackingId;
            try {
                records = rawRepoConnector.fetchRecordCollection(recordId);
                trackingId = getTrackingId(recordId, records);
                MDC.put(Constants.DBC_TRACKING_ID, trackingId);
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
            dataContainer.setEnrichmentTrail(getRecordEnrichmentTrail(recordId, records));
            dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());
            dataContainer.setTrackingId(trackingId);
            return dataContainer;
        } finally {
            MDC.remove(Constants.DBC_TRACKING_ID);
        }
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

    private String getTrackingId(RecordId recordId, Map<String, Record> records) {
        if (records.containsKey(recordId.getBibliographicRecordId())) {
            return records.get(recordId.getBibliographicRecordId()).getTrackingId();
        } else {
            LOGGER.info("Record {} was not found in returned collection. Tracking id could not be extracted", recordId.toString());
            return null;
        }
    }

    private String getRecordEnrichmentTrail(RecordId recordId, Map<String, Record> records) {
        return records.get(recordId.getBibliographicRecordId()).getEnrichmentTrail();
    }

    private void markAsFailure(QueueJob queuedItem, String errorMessage) throws HarvesterException {
        try {
            rawRepoConnector.queueFail(queuedItem, errorMessage);
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterException("Unable to mark queue item "+ queuedItem.toString() +" as failure", e);
        }
    }

    /* Stand-alone methods to enable easy override during testing */

    RawRepoConnector getRawRepoConnector(RawRepoHarvesterConfig.Entry config)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        final OpenAgencyTarget openAgencyTarget = config.getOpenAgencyTarget();
        if (openAgencyTarget == null) {
            throw new IllegalArgumentException("No OpenAgency target configured");
        }
        final OpenAgencyServiceFromURL openAgencyService;
        if (openAgencyTarget.getUser() == null && openAgencyTarget.getGroup() == null) {
            openAgencyService = OpenAgencyServiceFromURL.builder().build(openAgencyTarget.getUrl().toString());
        } else {
            openAgencyService = OpenAgencyServiceFromURL.builder()
                                    .authentication(
                                            openAgencyTarget.getUser(),
                                            openAgencyTarget.getGroup(),
                                            openAgencyTarget.getPassword())
                                    .build(openAgencyTarget.getUrl().toString());
        }
        return new RawRepoConnector(config.getResource(), openAgencyService);
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
