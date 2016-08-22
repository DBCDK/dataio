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
    public static final int DBC_LIBRARY = 191919;

    private static final Set<Integer> DBC_COMMUNITY = Stream.of(
            870970, 870971, 870972, 870973, 870974, 870975, 870976, 870977, 870978, 870979).collect(Collectors.toSet());

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    final RRHarvesterConfig config;
    final RRHarvesterConfig.Content configContent;
    final HarvesterJobBuilderFactory harvesterJobBuilderFactory;
    final AgencyConnection agencyConnection;
    final RawRepoConnector rawRepoConnector;

    final Map<Integer, HarvesterJobBuilder> harvesterJobBuilders = new HashMap<>();
    final JSONBContext jsonbContext = new JSONBContext();
    final DocumentBuilder documentBuilder = getDocumentBuilder();
    final Transformer transformer = getTransformer();

    public HarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory) {
        this(config, harvesterJobBuilderFactory, null, null);
    }

    HarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory,
                     AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector) {
        if (!hasOpenAgencyTarget(config)) {
            throw new IllegalArgumentException("No OpenAgency target configured");
        }
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.configContent = config.getContent();
        this.harvesterJobBuilderFactory = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilderFactory, "harvesterJobBuilderFactory");
        this.agencyConnection = agencyConnection != null ? agencyConnection : getAgencyConnection(config);
        this.rawRepoConnector = rawRepoConnector != null ? rawRepoConnector : getRawRepoConnector(config);
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed.
     * @param entityManager local database entity manager
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute(EntityManager entityManager) throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordQueue recordQueue = getRecordQueue(config, rawRepoConnector, entityManager);
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordQueue.size());

        int itemsProcessed = 0;
        RecordId recordId = recordQueue.poll();
        while (recordId != null) {
            LOGGER.info("{} ready for harvesting", recordId);

            final AddiMetaData addiMetaData = new AddiMetaData()
                    .withBibliographicRecordId(recordId.getBibliographicRecordId())
                    .withSubmitterNumber(recordId.getAgencyId());

            processRecord(recordId, addiMetaData);

            if (++itemsProcessed == batchSize) {
                break;
            }
            recordId = recordQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    protected void processRecord(RecordId recordId, AddiMetaData addiMetaData) throws HarvesterException {
        Record record = null;
        try {
            record = fetchRecord(recordId);

            DBCTrackedLogContext.setTrackingId(record.getTrackingId());

            addiMetaData
                    .withTrackingId(record.getTrackingId())
                    .withCreationDate(getRecordCreationDate(record));

            if (includeRecord(record)) {
                enrichAddiMetaData(addiMetaData);
                final HarvesterXmlRecord xmlContentForRecord = getXmlContentForEnrichedRecord(record, addiMetaData);
                getHarvesterJobBuilder(addiMetaData.submitterNumber().orElse(0))
                        .addRecord(
                                createAddiRecord(addiMetaData, xmlContentForRecord.asBytes()));
            }
        } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
            final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordId, e.getMessage());
            LOGGER.error(errorMsg);
            getHarvesterJobBuilder(addiMetaData.submitterNumber().orElse(0))
                    .addRecord(
                            createAddiRecord(addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg)),
                                             record != null ? record.getContent() : null));
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    RawRepoConnector getRawRepoConnector(RRHarvesterConfig config)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        final OpenAgencyTarget openAgencyTarget = config.getContent().getOpenAgencyTarget();
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

    JobSpecification getJobSpecificationTemplate(int agencyId) {
        return new JobSpecification("addi-xml", getFormat(agencyId), "utf8", configContent.getDestination(), agencyId,
                "placeholder", "placeholder", "placeholder", "placeholder", configContent.getType());
    }

    RecordQueue getRecordQueue(RRHarvesterConfig config, RawRepoConnector rawRepoConnector, EntityManager entityManager) throws HarvesterException {
        final RawRepoQueue rawRepoQueue = new RawRepoQueue(config, rawRepoConnector);
        if (rawRepoQueue.peek() != null) {
            return rawRepoQueue;
        }
        return new TaskQueue(config, entityManager);
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

   void flushHarvesterJobBuilders() throws HarvesterException {
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

    private boolean includeRecord(Record record) throws HarvesterInvalidRecordException {
        final int agencyId = record.getId().getAgencyId();
        // Special case handling for DBC records:
        // If the agency ID is either excluded or is equal to 191919...
        if (DBC_COMMUNITY.contains(agencyId) || DBC_LIBRARY == agencyId) {
            // if the record IS marked as DELETED in RR...
            if (record.isDeleted()) {
                if (agencyId == DBC_LIBRARY) {
                    // skip the record if it has agency ID 191919.
                    return false;
                }
            // if the record is NOT marked as DELETED in RR...
            } else if (DBC_COMMUNITY.contains(agencyId)) {
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

    private void enrichAddiMetaData(AddiMetaData addiMetaData) {
        if (configContent.hasIncludeLibraryRules()) {
            int agencyId = addiMetaData.submitterNumber().get();
            if (!isDbcAgencyId(agencyId)) {
                addiMetaData.withLibraryRules(
                        agencyConnection.getLibraryRules(
                                agencyId, addiMetaData.trackingId().orElse(null)));
            }
        }
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
        if (addiMetaData.submitterNumber().orElse(0) == DBC_LIBRARY) {
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
        dataContainer.setLibraryRules(addiMetaData.libraryRules().orElse(null));
        dataContainer.setData(marcExchangeCollection.asDocument().getDocumentElement());

        return dataContainer;
    }

    private MarcExchangeCollection getMarcExchangeCollection(RecordId recordId, Map<String, Record> records)
            throws HarvesterException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection(documentBuilder, transformer);
        if (configContent.hasIncludeRelations()) {
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

    private boolean hasOpenAgencyTarget(RRHarvesterConfig config) {
        return config.getContent().getOpenAgencyTarget() != null;
    }

    private boolean isDbcAgencyId(int agencyId) {
        return agencyId == DBC_LIBRARY || DBC_COMMUNITY.contains(agencyId);
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

    private Record fetchRecord(RecordId recordId) throws HarvesterSourceException, HarvesterInvalidRecordException {
        try {
            final Record record = rawRepoConnector.fetchRecord(recordId);
            if (record == null) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return record;
        } catch (SQLException | RawRepoException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId + ": " + e.getMessage(), e);
        }
    }

    private AgencyConnection getAgencyConnection(RRHarvesterConfig config) throws NullPointerException, IllegalArgumentException {
        return new AgencyConnection(config.getContent().getOpenAgencyTarget().getUrl());
    }

    private static DocumentBuilder getDocumentBuilder() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Transformer getTransformer() {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            return transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
