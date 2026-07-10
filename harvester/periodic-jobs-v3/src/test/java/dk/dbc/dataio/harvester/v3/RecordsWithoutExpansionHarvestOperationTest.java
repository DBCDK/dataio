package dk.dbc.dataio.harvester.v3;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordsWithoutExpansionHarvestOperationTest extends HarvestOperationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
            new PeriodicJobsV3HarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException, MarcWriterException, IOException {
        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordIdDTO recordId = new RecordIdDTO("rec", 191919);
        final RecordEntryDTO recordData = new RecordEntryDTO();
        recordData.setRecordId(recordId);
        recordData.setContent(asJsonNode(getRecordContent(recordId)));
        recordData.setCreated(creationTime.toString());
        recordData.setEnrichmentTrail("191919");
        recordData.setTrackingId(trackingId);

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordIdDTO(recordId.getBibliographicRecordId(), recordId.getAgencyId())),
                any(RecordServiceConnector.Params.class))).thenReturn(List.of(recordData));

        final AddiRecord addiRecord = new RecordsWithoutExpansionHarvestOperation.RecordFetcher(
                recordId, recordServiceConnector, config)
                .call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata", addiMetaData, is(new AddiMetaData()
                .withBibliographicRecordId(recordId.getBibliographicRecordId())
                .withSubmitterNumber(recordId.getAgencyId())
                .withEnrichmentTrail("191919")
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationTime))
                .withTrackingId(trackingId)
                .withLibraryRules(new AddiMetaData.LibraryRules())));

        assertThat("Addi content", new String(addiRecord.getContentData(), StandardCharsets.UTF_8),
                is(new String(asCollection(getRecordContent(recordId)).asBytes(), StandardCharsets.UTF_8)));

    }

    private static MarcBinding getRecordContent(RecordIdDTO recordId) throws MarcWriterException, IOException {
        MarcBinding marcBinding = new MarcBinding();
        marcBinding.setLeader(new Leader().setData("00000n 2200000 4500"));
        marcBinding.addField(new DataField("001", "00")
                .addSubField(new SubField('a', recordId.getBibliographicRecordId()))
                .addSubField(new SubField('b', "" + recordId.getAgencyId())));
        marcBinding.addField(new DataField("100", "00")
                .addSubField(new SubField('5', "870979"))
                .addSubField(new SubField('6', "134629681"))
                .addSubField(new SubField('4', "aut")));

        return marcBinding;
    }

    private static JsonNode asJsonNode(MarcBinding marcBinding) throws MarcWriterException, IOException {
        JSONBContext jsonbContext = new JSONBContext();
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
}