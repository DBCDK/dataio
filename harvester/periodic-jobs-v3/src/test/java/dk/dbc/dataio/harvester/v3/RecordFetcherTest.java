package dk.dbc.dataio.harvester.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.MarcJSonCollection;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Leader;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.writer.JsonWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecordFetcherTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final ObjectMapper mapper = new ObjectMapper();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
            new PeriodicJobsV3HarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    public void replaceWithDbcAgency() throws HarvesterException, RecordServiceConnectorException {
        RecordIdDTO recordId = new RecordIdDTO("id", 870970);

        new RecordFetcher(recordId, recordServiceConnector, config).call();

        verify(recordServiceConnector).getRecordDataCollection(
                eq(new RecordIdDTO("id", 191919)),
                any(RecordServiceConnector.Params.class));
    }

    @Test
    public void recordServiceConnectorFetchRecordCollectionThrows()
            throws RecordServiceConnectorException, HarvesterException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenThrow(new RecordServiceConnectorException("Message from connector"));

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "Message from connector");
    }

    //@Test
    public void recordHasInvalidXmlContent()
            throws HarvesterException, RecordServiceConnectorException, IOException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);

        RecordEntryDTO recordData = new RecordEntryDTO();
        recordData.setRecordId(recordId);
        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        //recordData.setContent("invalidXML".getBytes(StandardCharsets.UTF_8));

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(recordData)
                );

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "member data can not be parsed as marcJson");
    }

    @Test
    public void recordHasNoCreationDate()
            throws HarvesterException, RecordServiceConnectorException, JsonProcessingException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordEntryDTO recordData = new RecordEntryDTO();
        recordData.setRecordId(recordId);
        recordData.setCreated(null);
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent(mapper.readTree("{}"));

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(recordData));

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "Record creation date is null");
    }

    @Test
    public void emptyCollection()
            throws HarvesterException, RecordServiceConnectorException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(Collections.emptyList());

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "Empty record collection returned");
    }

    @Test
    public void recordIdNotInCollection()
            throws HarvesterException, RecordServiceConnectorException, JsonProcessingException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordEntryDTO recordData = new RecordEntryDTO();
        recordData.setRecordId(new RecordIdDTO("notId", 191919));
        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent(mapper.readTree("{}"));

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(recordData));

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "was not found in returned collection");
    }

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException, MarcWriterException, IOException {
        RecordIdDTO recordId = new RecordIdDTO("id", 123456);
        RecordEntryDTO recordData = new RecordEntryDTO();

        Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";
        recordData.setRecordId(recordId);
        recordData.setCreated(Instant.now().toString());
        recordData.setTrackingId(trackingId);
        recordData.setContent(asJsonNode(getRecordContent(recordId)));

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(recordData));

        AddiRecord addiRecord = new RecordFetcher(recordId, recordServiceConnector, config).call();

        AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
        assertThat("Addi metadata", addiMetaData, is(new AddiMetaData()
                .withBibliographicRecordId(recordId.getBibliographicRecordId())
                .withSubmitterNumber(recordId.getAgencyId())
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationTime))
                .withTrackingId(trackingId)
                .withLibraryRules(new AddiMetaData.LibraryRules())));

        assertThat("Addi content", new String(addiRecord.getContentData(), StandardCharsets.UTF_8),
                is(new String(asCollection(getRecordContent(recordId)).asBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    public void getSubmitterFromEnrichmentTrail()
            throws RecordServiceConnectorException, HarvesterException, JSONBException, MarcWriterException, IOException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordEntryDTO recordData = new RecordEntryDTO();
        recordData.setRecordId(recordId);
        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent(asJsonNode(getRecordContent(recordId)));

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(recordData));

        AddiRecord addiRecord = new RecordFetcher(recordId, recordServiceConnector, config).call();

        AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata enrichment trail", addiMetaData.enrichmentTrail(),
                is("191919,870970"));
        assertThat("Addi metadata submitter", addiMetaData.submitterNumber(),
                is(870970));
    }

    private static MarcBinding getRecordContent(RecordIdDTO recordId) {
        MarcBinding marcBinding = new MarcBinding();
        marcBinding.setLeader(new Leader().setData("00000n 2200000 4500"));
        marcBinding.addField(new DataField("001", "00")
                .addSubField(new SubField('a', recordId.getBibliographicRecordId()))
                .addSubField(new SubField('b', "" + recordId.getAgencyId())));
        marcBinding.addField(new DataField("245", "00")
                .addSubField(new SubField('a', "title")));

        return marcBinding;
    }

    private static JsonNode asJsonNode(MarcBinding marcBinding) throws MarcWriterException, IOException {
        final JSONBContext jsonbContext = new JSONBContext();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter jsonWriter = new JsonWriter();
        baos.write(jsonWriter.writeBinding(marcBinding, Charset.defaultCharset()));

        return jsonbContext.getObjectMapper().reader().readTree(baos.toByteArray());
    }

    private static MarcJSonCollection asCollection(MarcBinding... marcBindings) throws MarcWriterException, IOException {
        MarcJSonCollection marcJSonCollection = new MarcJSonCollection();
        for (MarcBinding marcBinding : marcBindings) {
            marcJSonCollection.addMember(asJsonNode(marcBinding).toString().getBytes());
        }

        return marcJSonCollection;
    }

    private void assertHasDiagnostic(AddiRecord addiRecord, String messageContains) {
        assertThat("Addi record", addiRecord, is(notNullValue()));
        try {
            AddiMetaData addiMetaData = jsonbContext.unmarshall(
                    new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);
            assertThat("Addi record metadata", addiMetaData,
                    is(notNullValue()));
            assertThat("Addi record metadata has diagnostic", addiMetaData.diagnostic(),
                    is(notNullValue()));
            assertThat("diagnostic message", addiMetaData.diagnostic().getMessage(),
                    containsString(messageContains));

        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
