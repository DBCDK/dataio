package dk.dbc.dataio.harvester.v3;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterInvalidRecordException;
import dk.dbc.dataio.harvester.types.HarvesterNoContentException;
import dk.dbc.dataio.harvester.types.HarvesterRecord;
import dk.dbc.dataio.harvester.types.HarvesterSourceException;
import dk.dbc.dataio.harvester.types.MarcJSonCollection;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.libcore.DBC;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.rawrepo.record.RecordServiceConnectorNoContentStatusCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class RecordFetcher implements Callable<AddiRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    final RecordIdDTO recordId;
    final RecordServiceConnector recordServiceConnector;
    final PeriodicJobsV3HarvesterConfig config;
    private final MarcXchangeV1Writer marcXchangeWriter = getMarcXchangeWriter();

    public RecordFetcher(RecordIdDTO recordId, RecordServiceConnector recordServiceConnector,
                         PeriodicJobsV3HarvesterConfig config) {
        this.recordId = DBC.governs(recordId.getAgencyId())
                ? new RecordIdDTO(recordId.getBibliographicRecordId(), DBC.agency.toInt())
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

    HarvesterRecord<MarcBinding> getAddiContent(AddiMetaData addiMetaData)
            throws HarvesterException {
        final Map<String, RecordEntryDTO> records;
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

        final RecordEntryDTO recordData = records.get(recordId.getBibliographicRecordId());

        DBCTrackedLogContext.setTrackingId(recordData.getTrackingId());

        LOGGER.info("Fetched record collection for {}", recordId);

        addiMetaData
                .withTrackingId(recordData.getTrackingId())
                .withCreationDate(getRecordCreationDate(recordData))
                .withSubmitterNumber(resolveAgencyId(recordData))
                .withEnrichmentTrail(recordData.getEnrichmentTrail())
                .withFormat(config.getContent().getFormat());

        return toMarcJSonCollection(records);
    }

    int resolveAgencyId(RecordEntryDTO recordData) throws HarvesterInvalidRecordException {
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


    Map<String, RecordEntryDTO> fetchRecordCollection(RecordIdDTO recordId)
            throws HarvesterInvalidRecordException, HarvesterSourceException {
        try {
            RecordServiceConnector.Params params = new RecordServiceConnector.Params().withExpand(true);
            List<RecordEntryDTO> recordDataCollection = recordServiceConnector.getRecordDataCollection(recordId, params);
            if (recordDataCollection == null || recordDataCollection.isEmpty()) {
                return Collections.emptyMap();
            }
            return recordDataCollection.stream().collect(Collectors.groupingBy(e -> e.getRecordId().getBibliographicRecordId(), Collectors.reducing(null, (e1, e2) -> e1 == null ? e2 : e1)));
        } catch (RecordServiceConnectorNoContentStatusCodeException e) {
            throw new HarvesterNoContentException(recordId.toString() + " - fetchRecordCollection");
        } catch (RecordServiceConnectorException e) {
            throw new HarvesterSourceException("Unable to fetch record for " + recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + ". " + e.getMessage(), e);
        }
    }


    Map<String, RecordEntryDTO> fetchRecordCollectionDataIO(RecordIdDTO recordId,
                                                            boolean expand,
                                                            boolean handleControlRecords,
                                                            boolean useParentAgency)
            throws HarvesterSourceException {
        try {
            final RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                    .withExpand(expand)
                    .withHandleControlRecords(handleControlRecords)
                    .withUseParentAgency(useParentAgency);
            List<RecordEntryDTO> recordDataCollection = recordServiceConnector.getRecordDataCollectionDataIO(recordId, params);
            if (recordDataCollection == null || recordDataCollection.isEmpty()) {
                throw new HarvesterInvalidRecordException("Record for " + recordId + " was not found");
            }
            return recordDataCollection.stream().collect(Collectors.groupingBy(e -> e.getRecordId().getBibliographicRecordId(), Collectors.reducing(null, (e1, e2) -> e1 == null ? e2 : e1)));
        } catch (RecordServiceConnectorException e) {
            throw new HarvesterSourceException("Unable to fetch record collection for " +
                    recordId.getAgencyId() + ":" + recordId.getBibliographicRecordId() + " " +
                    e.getMessage(), e);
        }
    }

    Date getRecordCreationDate(RecordEntryDTO recordData) throws HarvesterInvalidRecordException {
        if (recordData.getCreated() == null) {
            throw new HarvesterInvalidRecordException("Record creation date is null");
        }
        return Date.from(Instant.parse(recordData.getCreated()));
    }


    byte[] getRecordContent(RecordIdDTO recordId, RecordEntryDTO record) throws HarvesterInvalidRecordException {
        if (record == null) {
            throw new HarvesterInvalidRecordException(String.format("Record %s has null-valued content", recordId));
        }
        if (record.getContent() == null) return null;
        return record.getContent().toString().getBytes();
    }

    byte[] getRecordContent(RecordIdDTO recordId, Map<String, RecordEntryDTO> records) throws HarvesterInvalidRecordException {
        RecordEntryDTO record = records.get(recordId.getBibliographicRecordId());
        if (record == null) {
            throw new HarvesterInvalidRecordException(String.format("Record %s has null-valued content", recordId));
        }
        if (record.getContent() == null) return null;
        return record.getContent().toString().getBytes();
    }

    MarcJSonCollection toMarcJSonCollection(Map<String, RecordEntryDTO> records) throws HarvesterInvalidRecordException {
        MarcJSonCollection marcJSonCollection = new MarcJSonCollection();
        for (RecordEntryDTO recordData : records.values()) {
            LOGGER.debug("Adding {} member to {} marcjson collection", recordData.getRecordId(), recordId);
            marcJSonCollection.addMember(getRecordContent(recordData.getRecordId(), recordData));
        }
        return marcJSonCollection;
    }

    AddiRecord createAddiRecord(AddiMetaData metaData, byte[] content) throws HarvesterException {
        try {
            return new AddiRecord(JSONB_CONTEXT.marshall(metaData).getBytes(StandardCharsets.UTF_8), content);
        } catch (JSONBException e) {
            throw new HarvesterException(e);
        }
    }

    private MarcXchangeV1Writer getMarcXchangeWriter() {
        final MarcXchangeV1Writer marcXchangeWriter = new MarcXchangeV1Writer();
        marcXchangeWriter.setProperty(MarcXchangeV1Writer.Property.ADD_COLLECTION_WRAPPER, false);
        marcXchangeWriter.setProperty(MarcXchangeV1Writer.Property.ADD_XML_DECLARATION, false);
        return marcXchangeWriter;
    }
}
