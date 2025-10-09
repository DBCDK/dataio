package dk.dbc.dataio.harvester.rr_dm3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.harvester.utils.datafileverifier.BeanBuilder;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;

public class RecordEntryBuilder extends BeanBuilder<RecordEntryDTO> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final int AGENCY_ID = 123456;

    public RecordEntryBuilder() {
        super(new RecordEntryDTO());
    }

    public RecordEntryBuilder defaults(String recordId) {
       return defaults(recordId, AGENCY_ID);
    }

    public RecordEntryBuilder defaults(String recordId, int agencyId) {
        return defaults(new RecordIdDTO(recordId, agencyId));
    }

    public RecordEntryBuilder defaults(RecordIdDTO id) {
        return id(id).content().createdNow();
    }

    public RecordEntryBuilder id(RecordIdDTO recordId) {
        return set(r -> r.setRecordId(recordId));
    }

    public RecordEntryBuilder createdNow() {
        return set(r -> r.setCreated(Instant.now().toString()));
    }

    public RecordEntryBuilder deleted() {
        return set(r -> r.setDeleted(true));
    }

    public RecordEntryBuilder trackingId() {
        return set(r -> r.setTrackingId("tracking id"));
    }

    public RecordEntryBuilder trail(String trail) {
        return set(r -> r.setEnrichmentTrail(trail));
    }

    @Override
    public RecordEntryBuilder set(Consumer<RecordEntryDTO> c) {
        super.set(c);
        return this;
    }

    public RecordEntryBuilder content() {
        return set(r -> r.setContent(getRecordContent(r.getRecordId())));
    }

    public RecordEntryBuilder deleteContent(char recordType) {
        return set(r -> r.setContent(getDeleteRecordContent(r.getRecordId(), recordType)));
    }

    public static JsonNode getRecordContent(RecordIdDTO recordId) {
        return readContent("record-content.json", recordId.getBibliographicRecordId(),  recordId.getAgencyId());
    }

    public static JsonNode getDeleteRecordContent(RecordIdDTO recordId, char recordType) {
        return readContent("delete-record-content.json", recordId.getBibliographicRecordId(), recordId.getAgencyId(), recordType);
    }

    private static JsonNode readContent(String fileName, Object... args) {
        try {
            String content = String.format(readFile(fileName), args);
            return MAPPER.readTree(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readFile(String fileName) {
        try {
            URI uri = HarvestOperationTest.class.getClassLoader().getResource(fileName).toURI();
            return Files.readString(Path.of(uri));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }

    }
}
