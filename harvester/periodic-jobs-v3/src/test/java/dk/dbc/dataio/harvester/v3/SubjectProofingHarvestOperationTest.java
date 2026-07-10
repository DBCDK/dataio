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

public class SubjectProofingHarvestOperationTest extends HarvestOperationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
            new PeriodicJobsV3HarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException, MarcWriterException, IOException {

        Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        RecordIdDTO recordId191919 = new RecordIdDTO("id191919", 191919);
        RecordEntryDTO recordData191919 = new RecordEntryDTO();
        recordData191919.setRecordId(new RecordIdDTO(recordId191919.getBibliographicRecordId(), 191919));
        recordData191919.setContent(asJsonNode(getRecordContent(recordId191919)));

        RecordIdDTO recordId190004 = new RecordIdDTO("id190004", 190004);
        RecordEntryDTO recordData190004 = new RecordEntryDTO();
        recordData190004.setRecordId(new RecordIdDTO(recordId190004.getBibliographicRecordId(), 191919));
        recordData190004.setCreated(creationTime.toString());
        recordData190004.setEnrichmentTrail("190004,191919");
        recordData190004.setTrackingId(trackingId);
        recordData190004.setContent(asJsonNode(getRecordContent190004(recordId190004, recordId191919.getBibliographicRecordId())));

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordIdDTO(recordId190004.getBibliographicRecordId(), 191919)),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(
                        recordData190004)
                );
        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId191919),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(
                        recordData191919)
                );

        AddiRecord addiRecord = new SubjectProofingHarvestOperation.RecordFetcher(
                recordId190004, recordServiceConnector, config)
                .call();

        AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata", addiMetaData, is(new AddiMetaData()
                .withBibliographicRecordId(recordId190004.getBibliographicRecordId())
                .withSubmitterNumber(190004)
                .withEnrichmentTrail("190004,191919")
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationTime))
                .withTrackingId(trackingId)
                .withLibraryRules(new AddiMetaData.LibraryRules())));

        assertThat("Addi content", new String(addiRecord.getContentData(), StandardCharsets.UTF_8),
                is(new String(asCollection(getRecordContent190004(recordId190004, recordId191919.getBibliographicRecordId()),
                        getRecordContent(recordId191919)).asBytes(), StandardCharsets.UTF_8)));
    }

    private static MarcBinding getRecordContent(RecordIdDTO recordId) throws MarcWriterException, IOException {
        MarcBinding marcBinding = new MarcBinding();
        marcBinding.setLeader(new Leader().setData("00000n 2200000 4500"));
        marcBinding.addField(new DataField("001", "00")
                .addSubField(new SubField('a', recordId.getBibliographicRecordId()))
                .addSubField(new SubField('b', "" + recordId.getAgencyId())));
        marcBinding.addField(new DataField("245", "00")
                .addSubField(new SubField('a', "title")));

        return marcBinding;
    }

    private static MarcBinding getRecordContent190004(RecordIdDTO recordId, String bibliographicRecordId) throws MarcWriterException, IOException {
        MarcBinding marcBinding = new MarcBinding();
        marcBinding.setLeader(new Leader().setData("00000n 2200000 4500"));
        marcBinding.addField(new DataField("001", "00")
                .addSubField(new SubField('a', recordId.getBibliographicRecordId()))
                .addSubField(new SubField('b', "" + recordId.getAgencyId())));
        marcBinding.addField(new DataField("670", "00")
                .addSubField(new SubField('a', bibliographicRecordId)));

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
