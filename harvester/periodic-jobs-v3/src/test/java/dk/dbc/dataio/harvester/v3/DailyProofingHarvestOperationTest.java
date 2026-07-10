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

public class DailyProofingHarvestOperationTest extends HarvestOperationTest {
    private final static JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(1, 2,
            new PeriodicJobsV3HarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException, MarcWriterException, IOException {

        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordIdDTO recordIdRef1 = new RecordIdDTO("ref1", 191919);
        final RecordEntryDTO recordDataRef1 = new RecordEntryDTO();
        recordDataRef1.setRecordId(recordIdRef1);
        recordDataRef1.setContent(asJsonNode(getRecordContent(recordIdRef1, "e")));

        final RecordIdDTO recordIdRef2 = new RecordIdDTO("ref2", 191919);
        final RecordEntryDTO recordDataRef2 = new RecordEntryDTO();
        recordDataRef2.setRecordId(recordIdRef2);
        recordDataRef2.setContent(asJsonNode(getRecordContent(recordIdRef2, "e")));

        final RecordIdDTO recordIdRef3 = new RecordIdDTO("ref3", 191919);
        final RecordEntryDTO recordDataRef3 = new RecordEntryDTO();
        recordDataRef3.setRecordId(recordIdRef3);
        recordDataRef3.setContent(asJsonNode(getRecordContent(recordIdRef3, "e")));

        final RecordIdDTO recordIdVolume = new RecordIdDTO("volume", 191919);
        final RecordEntryDTO recordDataVolume = new RecordEntryDTO();
        recordDataVolume.setRecordId(recordIdVolume);
        recordDataVolume.setContent(asJsonNode(getRecordContent(recordIdVolume, "b", recordIdRef1, recordIdRef3)));
        recordDataVolume.setCreated(creationTime.toString());
        recordDataVolume.setEnrichmentTrail("191919");
        recordDataVolume.setTrackingId(trackingId);

        final RecordIdDTO recordIdSection = new RecordIdDTO("section", 191919);
        final RecordEntryDTO recordDataSection = new RecordEntryDTO();
        recordDataSection.setRecordId(recordIdSection);
        recordDataSection.setContent(asJsonNode(getRecordContent(recordIdSection, "s")));

        final RecordIdDTO recordIdHead = new RecordIdDTO("head", 191919);
        final RecordEntryDTO recordDataHead = new RecordEntryDTO();
        recordDataHead.setRecordId(recordIdHead);
        recordDataHead.setContent(asJsonNode(getRecordContent(recordIdHead, "h", recordIdRef2)));

        when(recordServiceConnector.getRecordDataCollectionDataIO(
                eq(new RecordIdDTO(recordIdVolume.getBibliographicRecordId(), recordIdVolume.getAgencyId())),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(List.of(
                        recordDataRef1,
                        recordDataRef2,
                        recordDataRef3,
                        recordDataVolume,
                        recordDataSection,
                        recordDataHead
                ));

        final AddiRecord addiRecord = new DailyProofingHarvestOperation.RecordFetcher(
                recordIdVolume, recordServiceConnector, config)
                .call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata", addiMetaData, is(new AddiMetaData()
                .withBibliographicRecordId(recordIdVolume.getBibliographicRecordId())
                .withSubmitterNumber(recordIdVolume.getAgencyId())
                .withEnrichmentTrail("191919")
                .withFormat(config.getContent().getFormat())
                .withCreationDate(Date.from(creationTime))
                .withTrackingId(trackingId)
                .withLibraryRules(new AddiMetaData.LibraryRules())));

        assertThat("Addi content", new String(addiRecord.getContentData(), StandardCharsets.UTF_8),
                is(new String(asCollection(
                        getRecordContent(recordIdHead, "h", recordIdRef2),
                        getRecordContent(recordIdVolume, "b", recordIdRef1, recordIdRef3),
                        getRecordContent(recordIdRef3, "e"),
                        getRecordContent(recordIdSection, "s"),
                        getRecordContent(recordIdRef2, "e"),
                        getRecordContent(recordIdRef1, "e")
                ).asBytes(), StandardCharsets.UTF_8)));
    }

    private static MarcBinding getRecordContent(RecordIdDTO recordId, String type, RecordIdDTO... ref520) throws MarcWriterException, IOException {
        MarcBinding marcBinding = new MarcBinding();
        marcBinding.setLeader(new Leader().setData("00000n 2200000 4500"));
        marcBinding.addField(new DataField("001", "00")
                .addSubField(new SubField('a', recordId.getBibliographicRecordId()))
                .addSubField(new SubField('b', "" + recordId.getAgencyId()))
        );
        marcBinding.addField(new DataField("004", "00")
                .addSubField(new SubField('a', type))
        );
        marcBinding.addField(new DataField("520", "00")
                .addSubField(new SubField('a', "Originaludgave: 2021"))
        );

        if (ref520 != null) {
            for (RecordIdDTO ref : ref520) {
                // This is a bit ugly, but we test handling of repeated 520n's using the same record twice.
                marcBinding.addField(new DataField("520", "00")
                        .addSubField(new SubField('a', "Originaludgave: 2021"))
                        .addSubField(new SubField('n', ref.getBibliographicRecordId()))
                        .addSubField(new SubField('n', ref.getBibliographicRecordId()))
                );
            }
        }

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
