/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.libcore.DBC;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class RecordFetcher implements Callable<AddiRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private final RecordData.RecordId recordId;
    private final RecordServiceConnector recordServiceConnector;
    private final PeriodicJobsHarvesterConfig config;

    public RecordFetcher(RecordData.RecordId recordId, RecordServiceConnector recordServiceConnector,
                         PeriodicJobsHarvesterConfig config) {
        this.recordId = DBC.governs(recordId.getAgencyId())
                ? new RecordData.RecordId(recordId.getBibliographicRecordId(), DBC.agency.toInt())
                : recordId;
        this.recordServiceConnector = recordServiceConnector;
        this.config = config;
    }

    @Override
    public AddiRecord call() throws HarvesterException {
        final AddiMetaData addiMetaData = new AddiMetaData()
                .withBibliographicRecordId(recordId.getBibliographicRecordId());
        try {
            return createAddiRecord(addiMetaData, getAddiContent(addiMetaData).asBytes());
        } catch (HarvesterInvalidRecordException | HarvesterSourceException e) {
            final String errorMsg = String.format("Harvesting RawRepo %s failed: %s", recordId, e.getMessage());
            LOGGER.error(errorMsg);

            return createAddiRecord(
                    addiMetaData.withDiagnostic(new Diagnostic(Diagnostic.Level.FATAL, errorMsg)), null);
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    private HarvesterXmlRecord getAddiContent(AddiMetaData addiMetaData)
            throws HarvesterException {
        final Map<String, RecordData> records;
        try {
            records = fetchRecordCollection(recordId);
        } catch (HarvesterSourceException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " +
                    recordId + ": " + e.getMessage(), e);
        }
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty record collection returned for " +
                    recordId);
        }
        if (!records.containsKey(recordId.getBibliographicRecordId())) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record %s was not found in returned collection", recordId));
        }

        final RecordData recordData = records.get(recordId.getBibliographicRecordId());

        DBCTrackedLogContext.setTrackingId(recordData.getTrackingId());

        LOGGER.info("Fetched record collection for {}", recordId);

        addiMetaData
                .withTrackingId(recordData.getTrackingId())
                .withCreationDate(getRecordCreationDate(recordData))
                .withSubmitterNumber(resolveAgencyId(recordData))
                .withEnrichmentTrail(recordData.getEnrichmentTrail())
                .withFormat(config.getContent().getFormat());

        return createMarcExchangeCollection(records);
    }

    private int resolveAgencyId(RecordData recordData) throws HarvesterInvalidRecordException {
        final String enrichmentTrail = recordData.getEnrichmentTrail();
        if (enrichmentTrail == null || enrichmentTrail.trim().isEmpty()) {
            return recordId.getAgencyId();
        }
        final Optional<String> agencyIdAsString = Arrays.stream(enrichmentTrail.split(","))
                .filter(agencyId -> agencyId.startsWith("870") || agencyId.startsWith("19000"))
                .findFirst();
        try {
            return agencyIdAsString.map(Integer::parseInt)
                    .orElseGet(() -> recordData.getRecordId().getAgencyId());
        } catch (NumberFormatException e) {
            throw new HarvesterInvalidRecordException(String.format(
                    "Record with ID %s has invalid 870* or 19000* agency ID in its enrichment trail '%s'",
                    recordData.getRecordId(), enrichmentTrail));
        }
    }

    private Map<String, RecordData> fetchRecordCollection(RecordData.RecordId recordId)
            throws HarvesterSourceException {
        try {
            final RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                    .withUseParentAgency(false)
                    .withExcludeAutRecords(true)
                    .withAllowDeleted(true)
                    .withExpand(true);
            if (recordId.getAgencyId() == DBC.agency.toInt()) {
                params.withUseParentAgency(true);
            }
            final HashMap<String, RecordData> recordDataCollection =
                    recordServiceConnector.getRecordDataCollection(recordId, params);

            if (recordDataCollection == null) {
                return Collections.emptyMap();
            }
            return recordDataCollection;
        } catch (RecordServiceConnectorException e) {
            throw new HarvesterSourceException("Unable to fetch record for " +
                    recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + ". " +
                    e.getMessage(), e);
        }
    }

    private Date getRecordCreationDate(RecordData recordData) throws HarvesterInvalidRecordException {
        if (recordData.getCreated() == null) {
            throw new HarvesterInvalidRecordException("Record creation date is null");
        }
        return Date.from(Instant.parse(recordData.getCreated()));
    }

    private MarcExchangeCollection createMarcExchangeCollection(Map<String, RecordData> records)
            throws HarvesterException {
        final MarcExchangeCollection marcExchangeCollection = new MarcExchangeCollection();
        for (RecordData recordData : records.values()) {
            LOGGER.debug("Adding {} member to {} marc exchange collection", recordData.getRecordId (), recordId);
            marcExchangeCollection.addMember(getRecordContent(recordData));
        }
        return marcExchangeCollection;
    }

    private byte[] getRecordContent(RecordData record) {
        if (record == null) {
            return null;
        }
        return record.getContent();
    }

    private AddiRecord createAddiRecord(AddiMetaData metaData, byte[] content) throws HarvesterException {
        try {
            return new AddiRecord(JSONB_CONTEXT.marshall(metaData).getBytes(StandardCharsets.UTF_8), content);
        } catch (JSONBException e) {
            throw new HarvesterException(e);
        }
    }
}