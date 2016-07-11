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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.harvester.types.DataContainer;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.rawrepo.AgencySearchOrder;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RelationHints;
import dk.dbc.rawrepo.RelationHintsOpenAgency;
import dk.dbc.rawrepo.showorder.AgencySearchOrderFromShowOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HarvestOperation {
    public static final int DBC_LIBRARY_NUMBER = 191919;

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    /* Currently only 87097x agency id's should be added to excludes or else execute functionality will break */
    private static final Set<Integer> AGENCY_ID_EXCLUDES = Stream.of(
            870970, 870971, 870972, 870973, 870974, 870975, 870976, 870977, 870978, 870979).collect(Collectors.toSet());

    private final RRHarvesterConfig config;
    private final RRHarvesterConfig.Content configContent;
    private final HarvesterJobBuilderFactory harvesterJobBuilderFactory;
    private final Map<Integer, HarvesterJobBuilder> harvesterJobBuilders = new HashMap<>();
    private final DocumentBuilder documentBuilder;
    private final Transformer transformer;
    private final RawRepoConnector rawRepoConnector;
    private final JSONBContext jsonbContext;

    public HarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.configContent = config.getContent();
        this.harvesterJobBuilderFactory = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilderFactory, "harvesterJobBuilderFactory");
        documentBuilder = getDocumentBuilder();
        transformer = getTransformer();
        rawRepoConnector = getRawRepoConnector(config);
        jsonbContext = new JSONBContext();
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed.
     * @param entityManager local database entity manager
     * @return number of records harvested and included in dataIO jobs
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute(EntityManager entityManager) throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordQueue recordQueue = getRecordQueue(config, rawRepoConnector);

        int itemsHarvested = 0;
        RecordWrapper recordWrapper = recordQueue.poll();
        while (recordWrapper != null) {
            LOGGER.info("{} ready for harvesting", recordWrapper);
            Record record = null;
            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordWrapper.getRecordId().getBibliographicRecordId())
                    .withSubmitterNumber(recordWrapper.getRecordId().getAgencyId());

            try {
                record = recordWrapper.getRecord().orElseThrow(recordWrapper::getError);

                DBCTrackedLogContext.setTrackingId(record.getTrackingId());

                addiMetaData
                    .withTrackingId(record.getTrackingId())
                    .withCreationDate(getRecordCreationDate(record));

                if (includeRecord(record)) {
                    final HarvesterXmlRecord xmlContentForRecord = getXmlContentForEnrichedRecord(record, addiMetaData);
                    getHarvesterJobBuilder(addiMetaData.submitterNumber().orElse(0))
                            .addRecord(
                                    createAddiRecord(addiMetaData, xmlContentForRecord.asBytes()));
                    itemsHarvested++;
                }
            } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
                final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordWrapper, e.getMessage());
                LOGGER.error(errorMsg);
                addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg));
                getHarvesterJobBuilder(addiMetaData.submitterNumber().orElse(0))
                        .addRecord(
                                createAddiRecord(addiMetaData, record != null ? record.getContent() : null));
                itemsHarvested++;
            } finally {
                DBCTrackedLogContext.remove();
            }

            if (itemsHarvested == configContent.getBatchSize()) {
                break;
            }
            recordWrapper = recordQueue.poll();
        }
        flushHarvesterJobBuilders();

        LOGGER.info("Harvested {} items from {} queue in {} ms",
                itemsHarvested, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsHarvested;
    }

    JobSpecification getJobSpecificationTemplate(int agencyId) {
        return new JobSpecification("addi-xml", getFormat(agencyId), "utf8", configContent.getDestination(), agencyId,
                "placeholder", "placeholder", "placeholder", "placeholder", configContent.getType());
    }

    int getAgencyIdFromEnrichmentTrail(Record record) throws HarvesterInvalidRecordException {
        final String enrichmentTrail = record.getEnrichmentTrail();
        if (enrichmentTrail == null || enrichmentTrail.trim().isEmpty()) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record with ID %s has no enrichment trail '%s'", record.getId(), enrichmentTrail));
        }
        final Optional<String> agencyIdAsString = Arrays.stream(enrichmentTrail.split(","))
                .filter(agencyId -> agencyId.startsWith("870"))
                .findFirst();
        try {
            if (agencyIdAsString.isPresent()) {
                return Integer.parseInt(agencyIdAsString.get());
            } else {
                throw new HarvesterInvalidRecordException(String.format(
                        "Record with ID %s has no 870* in its enrichment trail '%s'", record.getId(), enrichmentTrail));
            }
        } catch (NumberFormatException e) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record with ID %s has invalid 870* agency ID in its enrichment trail '%s'", record.getId(), enrichmentTrail));
        }
    }

    private boolean includeRecord(Record record) throws HarvesterInvalidRecordException {
        final int agencyId = record.getId().getAgencyId();
        // Special case handling for DBC records:
        // If the agency ID is either excluded or is equal to 191919...
        if (AGENCY_ID_EXCLUDES.contains(agencyId) || DBC_LIBRARY_NUMBER == agencyId) {
            // if the record IS marked as DELETED in RR...
            if (record.isDeleted()) {
                if (agencyId == DBC_LIBRARY_NUMBER) {
                    // skip the record if it has agency ID 191919.
                    return false;
                }
            // if the record is NOT marked as DELETED in RR...
            } else if (AGENCY_ID_EXCLUDES.contains(agencyId)) {
                // skip the record if has an excluded agency ID.
                return false;
            }
        }
        // else include the record.
        return true;
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

    /* Fetches rawrepo record collection associated with given record ID and adds its content to a new MARC exchange collection.
       Returns data container harvester record containing MARC exchange collection as data
     */
    private HarvesterXmlRecord getXmlContentForEnrichedRecord(Record record, AddiMetaData addiMetaData) throws HarvesterException {
        final Map<String, Record> records;
        try {
            records = rawRepoConnector.fetchRecordCollection(record.getId());
        } catch (SQLException | RawRepoException | MarcXMergerException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " + record.getId() + ": " + e.getMessage(), e);
        }
        LOGGER.debug("Fetched record collection<{}> for {}", records.values(), record.getId());
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty record collection returned for " + record.getId());
        }
        if (!records.containsKey(record.getId().getBibliographicRecordId())) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s was not found in returned collection", record.getId()));
        }
        // refresh - set to merged record
        record = records.get(record.getId().getBibliographicRecordId());
        if (record.getId().getAgencyId() == DBC_LIBRARY_NUMBER) {
            // extract agency ID from enrichment trail if the record has agency ID 191919.
            addiMetaData.withSubmitterNumber(getAgencyIdFromEnrichmentTrail(record));
        }
        addiMetaData.withEnrichmentTrail(record.getEnrichmentTrail());
        addiMetaData.withFormat(getFormat(addiMetaData.submitterNumber().orElse(0)));

        //// TODO: 6/24/16 We should teach our javascript to work with addi records - this would remove the need for XML-DOM functionality below.

        final MarcExchangeCollection marcExchangeCollection = getMarcExchangeCollection(record.getId(), records);
        final DataContainer dataContainer = new DataContainer(documentBuilder, transformer);
        dataContainer.setCreationDate(record.getCreated());
        dataContainer.setEnrichmentTrail(record.getEnrichmentTrail());
        dataContainer.setTrackingId(record.getTrackingId());
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        return dataContainer;
    }

    private MarcExchangeCollection getMarcExchangeCollection(RecordId recordId, Map<String, Record> records)
            throws HarvesterException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(documentBuilder, transformer);
        if (configContent.isIncludeRelations()) {
            for (Record record : records.values()) {
                LOGGER.debug("Adding {} member to {} marc exchange collection", record.getId(), recordId);
                marcExchangeCollection.addMember(getRecordContent(record.getId(), record));
            }
        } else {
            marcExchangeCollection.addMember(getRecordContent(recordId, records));
        }
        return marcExchangeCollection;
    }

    private byte[] getRecordContent(RecordId recordId, Map<String, Record> records) throws HarvesterInvalidRecordException {
        return getRecordContent(recordId, records.get(recordId.getBibliographicRecordId()));
    }

    private byte[] getRecordContent(RecordId recordId, Record record) throws HarvesterInvalidRecordException {
        if (record == null) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s has null-valued content", recordId));
        }
        return record.getContent();
    }

    private Date getRecordCreationDate(Record record) throws HarvesterInvalidRecordException {
        final Date created = record.getCreated();
        if (created == null) {
            throw new HarvesterInvalidRecordException("Record creation date is null");
        }
        return created;
    }

    /* Stand-alone methods to enable easy override during testing */

    RawRepoConnector getRawRepoConnector(RRHarvesterConfig config)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        final OpenAgencyTarget openAgencyTarget = config.getContent().getOpenAgencyTarget();
        if (openAgencyTarget == null) {
            throw new IllegalArgumentException("No OpenAgency target configured");
        }

        final OpenAgencyServiceFromURL openAgencyService;
        if (openAgencyTarget.getUser() == null && openAgencyTarget.getGroup() == null) {
            openAgencyService = OpenAgencyServiceFromURL.builder().build(openAgencyTarget.getUrl());
        } else {
            openAgencyService = OpenAgencyServiceFromURL.builder()
                                    .authentication(
                                            openAgencyTarget.getUser(),
                                            openAgencyTarget.getGroup(),
                                            openAgencyTarget.getPassword())
                                    .build(openAgencyTarget.getUrl());
        }

        final AgencySearchOrder agencySearchOrder = new AgencySearchOrderFromShowOrder(openAgencyService);
        final RelationHints relationHints = new RelationHintsOpenAgency(openAgencyService);

        return new RawRepoConnector(config.getContent().getResource(), agencySearchOrder, relationHints);
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

    private String getFormat(int agencyId) {
        final String formatOverride = configContent.getFormatOverrides().get(agencyId);
        return formatOverride != null ? formatOverride : configContent.getFormat();
    }

    private AddiRecord createAddiRecord(AddiMetaData metaData, byte[] content) throws HarvesterException {
        try {
            return new AddiRecord(jsonbContext.marshall(metaData).getBytes(StandardCharsets.UTF_8), content);
        } catch (JSONBException e) {
            throw new HarvesterException(e);
        }
    }

    private RecordQueue getRecordQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector) {
        return new RawRepoQueue(config, rawRepoConnector);
    }

    /**
     * Fetches rawrepo record with given record ID using given connector
     * @param recordId ID of record to be fetched
     * @param connector rawrepo connector
     * @return rawrepo record
     * @throws HarvesterSourceException on error communicating with the rawrepo
     * @throws HarvesterInvalidRecordException if null-valued record is retrieved
     */
    public static Record fetchRecordFromRR(RecordId recordId, RawRepoConnector connector)
            throws HarvesterSourceException, HarvesterInvalidRecordException {
        try {
            final Record record = connector.fetchRecord(recordId);
            if (record == null) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return record;
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId + ": " + e.getMessage(), e);
        }
    }
}
