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
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.rawrepo.RelationHintsOpenAgency;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.RecordData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;



public class HarvestOperation {
    static final int DBC_LIBRARY = 191919;

    static final Set<Integer> DBC_COMMUNITY = Stream.of(
            870970, 870971, 870972, 870973, 870974, 870975, 870976, 870977, 870978, 870979).collect(Collectors.toSet());

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvestOperation.class);

    final RRHarvesterConfig config;
    final RRHarvesterConfig.Content configContent;
    final HarvesterJobBuilderFactory harvesterJobBuilderFactory;
    final AgencyConnection agencyConnection;
    final RawRepoConnector rawRepoConnector;
    final RecordServiceConnector rawRepoRecordServiceConnector;

    private final Map<Integer, HarvesterJobBuilder> harvesterJobBuilders = new LinkedHashMap<>();
    private final JSONBContext jsonbContext = new JSONBContext();
    private final TaskRepo taskRepo;
    private int basedOnJob = 0;

    public HarvestOperation(RRHarvesterConfig config,
            HarvesterJobBuilderFactory harvesterJobBuilderFactory,
            TaskRepo taskRepo, String openAgencyEndpoint)
            throws SQLException, QueueException, ConfigurationException {
        this(config, harvesterJobBuilderFactory, taskRepo,
            new AgencyConnection(openAgencyEndpoint), null, null);
    }

    HarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                     AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector, RecordServiceConnector recordServiceConnector) {
        this.config = InvariantUtil.checkNotNullOrThrow(config, "config");
        this.configContent = config.getContent();
        this.harvesterJobBuilderFactory = InvariantUtil.checkNotNullOrThrow(harvesterJobBuilderFactory, "harvesterJobBuilderFactory");
        this.taskRepo = InvariantUtil.checkNotNullOrThrow(taskRepo, "taskRepo");
        this.agencyConnection = InvariantUtil.checkNotNullOrThrow(
                agencyConnection, "agencyConnection");
        this.rawRepoConnector = rawRepoConnector != null ? rawRepoConnector : getRawRepoConnector(config);
        this.rawRepoRecordServiceConnector = recordServiceConnector;
    }

    HarvestOperation(RRHarvesterConfig config, HarvesterJobBuilderFactory harvesterJobBuilderFactory, TaskRepo taskRepo,
                     AgencyConnection agencyConnection, RawRepoConnector rawRepoConnector)
            throws SQLException, QueueException, ConfigurationException {
        this(config, harvesterJobBuilderFactory, taskRepo, agencyConnection, rawRepoConnector,
                new RecordServiceConnector(HttpClient.newClient(), rawRepoConnector.getRecordServiceUrl()));
    }

    /**
     * Runs this harvest operation, creating dataIO jobs from harvested records.
     * If any non-internal error occurs a record is marked as failed.
     * @return number of records processed
     * @throws HarvesterException on failure to complete harvest operation
     */
    public int execute() throws HarvesterException {
        final StopWatch stopWatch = new StopWatch();
        final RecordHarvestTaskQueue recordHarvestTaskQueue = createTaskQueue();
        // Since we might (re)run batches with a size larger than the one currently configured
        final int batchSize = Math.max(configContent.getBatchSize(), recordHarvestTaskQueue.estimatedSize());

        int itemsProcessed = 0;
        RawRepoRecordHarvestTask recordHarvestTask = recordHarvestTaskQueue.poll();
        while (recordHarvestTask != null) {
            LOGGER.info("{} ready for harvesting", recordHarvestTask.getRecordId());

            processRecordHarvestTask(recordHarvestTask);

            if (++itemsProcessed == batchSize) {
                break;
            }
            recordHarvestTask = recordHarvestTaskQueue.poll();
        }
        flushHarvesterJobBuilders();

        recordHarvestTaskQueue.commit();

        LOGGER.info("Processed {} items from {} queue in {} ms",
                itemsProcessed, configContent.getConsumerId(), stopWatch.getElapsedTime());

        return itemsProcessed;
    }

    void processRecordHarvestTask(RawRepoRecordHarvestTask recordHarvestTask) throws HarvesterException {
        RecordData recordData = null;
        try {
            recordData = fetchRecord(recordHarvestTask.getRecordId());

            DBCTrackedLogContext.setTrackingId(recordData.getTrackingId());

            final AddiMetaData addiMetaData = recordHarvestTask.getAddiMetaData()
                    .withTrackingId(recordData.getTrackingId())
                    .withCreationDate(getRecordCreationDate(recordData));

            if (includeRecord(recordData.getRecordId ().getAgencyId (), recordData.isDeleted() || recordHarvestTask.isForceAdd())) {
                enrichAddiMetaData(addiMetaData);
                final HarvesterXmlRecord xmlContentForRecord = getXmlContentForEnrichedRecord(recordData, addiMetaData);
                getHarvesterJobBuilder(addiMetaData.submitterNumber())
                        .addRecord(
                                createAddiRecord(addiMetaData, xmlContentForRecord.asBytes()));
            }
        } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
            final String errorMsg = String.format("Harvesting RawRepo %s failed: %s",
                    recordHarvestTask.getRecordId(), e.getMessage());
            LOGGER.error(errorMsg);
            getHarvesterJobBuilder(recordHarvestTask.getAddiMetaData().submitterNumber())
                    .addRecord(
                            createAddiRecord(recordHarvestTask.getAddiMetaData().withDiagnostic(
                                    new Diagnostic(Diagnostic.Level.FATAL, errorMsg)),
                                    recordData != null ? recordData.getContent() : null));
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    RawRepoConnector getRawRepoConnector(RRHarvesterConfig config)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        final OpenAgencyServiceFromURL openAgencyService =
            OpenAgencyServiceFromURL.builder().build(agencyConnection
            .getConnector().getEndpoint());

        final RelationHintsOpenAgency relationHints = new RelationHintsOpenAgency(openAgencyService);

        return new RawRepoConnector(config.getContent().getResource(), relationHints);
    }

    JobSpecification getJobSpecificationTemplate(int agencyId) {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry()
                .withHarvesterToken(config.getHarvesterToken());
        if (basedOnJob > 0) {
            ancestry.withPreviousJobId(basedOnJob);
        }

        return new JobSpecification()
                .withPackaging("addi-xml")
                .withFormat(getFormat(agencyId))
                .withCharset("utf8")
                .withDestination(configContent.getDestination())
                .withSubmitterId(agencyId)
                .withType(configContent.getType())
                .withAncestry(ancestry);
    }

    RecordHarvestTaskQueue createTaskQueue() throws HarvesterException {
        final RawRepoQueue rawRepoQueue = new RawRepoQueue(config, rawRepoConnector);
        if (rawRepoQueue.peek() != null) {
            return rawRepoQueue;
        }
        final TaskQueue queue = new TaskQueue(config, taskRepo);
        basedOnJob = queue.basedOnJob();
        return queue;
    }

    int getAgencyIdFromEnrichmentTrail(RecordData recordData) throws HarvesterInvalidRecordException {
        final String enrichmentTrail = recordData.getEnrichmentTrail();
        if (enrichmentTrail == null || enrichmentTrail.trim().isEmpty()) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record with ID %s has no enrichment trail '%s'", recordData.getRecordId(), enrichmentTrail));
        }
        final Optional<String> agencyIdAsString = Arrays.stream(enrichmentTrail.split(","))
                .filter(agencyId -> agencyId.startsWith("870"))
                .findFirst();
        try {
            if (agencyIdAsString.isPresent()) {
                return Integer.parseInt(agencyIdAsString.get());
            } else {
                throw new HarvesterInvalidRecordException(String.format(
                        "Record with ID %s has no 870* in its enrichment trail '%s'", recordData.getRecordId (), enrichmentTrail));
            }
        } catch (NumberFormatException e) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record with ID %s has invalid 870* agency ID in its enrichment trail '%s'", recordData.getRecordId(), enrichmentTrail));
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

    private boolean includeRecord(int agencyId, boolean isDeleted) throws HarvesterInvalidRecordException {
        // Special case handling for DBC records:
        // If the agency ID is either excluded or is equal to 191919...
        if (DBC_COMMUNITY.contains(agencyId) || DBC_LIBRARY == agencyId) {
            // if the record IS marked as DELETED in RR...
            if (isDeleted) {
                if (agencyId == DBC_LIBRARY) {
                    // skip the record if it has agency ID 191919 and is not force delete.
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
            addiMetaData.withLibraryRules(agencyConnection.getLibraryRules(
                    addiMetaData.submitterNumber(), addiMetaData.trackingId()));
        }
    }

    /* Fetches rawrepo record collection associated with given record ID and adds its content to a new MARC exchange collection.
       Returns MARC exchange collection
     */
    private HarvesterXmlRecord getXmlContentForEnrichedRecord(RecordData recordData, AddiMetaData addiMetaData) throws HarvesterException {
        final Map<String, RecordData> records;
        try {
            records = fetchRecordCollection (recordData.getRecordId ());
        } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " + recordData.getRecordId() + ": " + e.getMessage(), e);
        }
        LOGGER.debug("Fetched record collection<{}> for {}", records.values(), recordData.getRecordId());
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty record collection returned for " + recordData.getRecordId());
        }
        if (!records.containsKey(recordData.getRecordId().getBibliographicRecordId())) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s was not found in returned collection", recordData.getRecordId()));
        }
        // refresh - set to merged record
        recordData = records.get(recordData.getRecordId().getBibliographicRecordId());
        if (addiMetaData.submitterNumber() == DBC_LIBRARY) {
            // extract agency ID from enrichment trail if the record has agency ID 191919.
            addiMetaData.withSubmitterNumber(getAgencyIdFromEnrichmentTrail(recordData));
        }
        addiMetaData.withEnrichmentTrail(recordData.getEnrichmentTrail());
        addiMetaData.withFormat(getFormat(addiMetaData.submitterNumber()));

        return getMarcExchangeCollection(recordData.getRecordId(), records);
    }

    private MarcExchangeCollection getMarcExchangeCollection(RecordData.RecordId recordId, Map<String, RecordData> records) throws HarvesterException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection();
        marcExchangeCollection.addMember(getRecordContent(recordId, records));
        if (configContent.hasIncludeRelations()) {
            for (RecordData recordData : records.values()) {
                if (recordId.equals(recordData.getRecordId ())) {
                    continue;
                }
                LOGGER.debug("Adding {} member to {} marc exchange collection", recordData.getRecordId (), recordId);
                marcExchangeCollection.addMember(getRecordContent(recordData.getRecordId(), recordData));
            }
        }
        return marcExchangeCollection;
    }

    private byte[] getRecordContent(RecordData.RecordId recordId, Map<String, RecordData> records) throws HarvesterInvalidRecordException {
        return getRecordContent(recordId, records.get(recordId.getBibliographicRecordId()));
    }

    private byte[] getRecordContent(RecordData.RecordId recordId, RecordData record) throws HarvesterInvalidRecordException {
        if (record == null) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s has null-valued content", recordId));
        }
        return record.getContent();
    }

    private Date getRecordCreationDate(RecordData recordData) throws HarvesterInvalidRecordException {
        if (recordData.getCreated() == null) {
            throw new HarvesterInvalidRecordException("Record creation date is null");
        }
        final Instant created = Instant.parse(recordData.getCreated());
        return Date.from(created);
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

    RecordData fetchRecord(RecordData.RecordId recordId) throws HarvesterSourceException, HarvesterInvalidRecordException {
        try {

            final RecordData recordData = rawRepoRecordServiceConnector.getRecordData (recordId);

            if (recordData == null) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return recordData;
        } catch ( RecordServiceConnectorException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId + ": " + e.getMessage(), e);
        }
    }

    Map<String, RecordData> fetchRecordCollection(RecordData.RecordId recordId)
        throws HarvesterInvalidRecordException, HarvesterSourceException{
        try {

            RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                    .withAllowDeleted(true);
            final HashMap<String, RecordData> recordDataCollection =
                    rawRepoRecordServiceConnector.getRecordDataCollection (
                            recordId.getAgencyId(), recordId.getBibliographicRecordId (), params);

            if (recordDataCollection == null || recordDataCollection.isEmpty ()) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return recordDataCollection;
        } catch ( RecordServiceConnectorException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId + ": " + e.getMessage(), e);
        }
    }
}
