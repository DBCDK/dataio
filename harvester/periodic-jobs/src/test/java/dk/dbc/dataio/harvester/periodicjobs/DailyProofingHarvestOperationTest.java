package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DailyProofingHarvestOperationTest extends HarvestOperationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException {

        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordIdDTO recordIdRef1 = new RecordIdDTO("ref1", 191919);
        final RecordDTO recordDataRef1 = new RecordDTO();
        recordDataRef1.setRecordId(recordIdRef1);
        recordDataRef1.setContent(asCollection(getRecordContent(recordIdRef1, "e")).getBytes());

        final RecordIdDTO recordIdRef2 = new RecordIdDTO("ref2", 191919);
        final RecordDTO recordDataRef2 = new RecordDTO();
        recordDataRef2.setRecordId(recordIdRef2);
        recordDataRef2.setContent(asCollection(getRecordContent(recordIdRef2, "e")).getBytes());

        final RecordIdDTO recordIdRef3 = new RecordIdDTO("ref3", 191919);
        final RecordDTO recordDataRef3 = new RecordDTO();
        recordDataRef3.setRecordId(recordIdRef3);
        recordDataRef3.setContent(asCollection(getRecordContent(recordIdRef3, "e")).getBytes());

        final RecordIdDTO recordIdVolume = new RecordIdDTO("volume", 191919);
        final RecordDTO recordDataVolume = new RecordDTO();
        recordDataVolume.setRecordId(recordIdVolume);
        recordDataVolume.setContent(asCollection(getRecordContent(recordIdVolume, "b", recordIdRef1, recordIdRef3)).getBytes());
        recordDataVolume.setCreated(creationTime.toString());
        recordDataVolume.setEnrichmentTrail("191919");
        recordDataVolume.setTrackingId(trackingId);

        final RecordIdDTO recordIdSection = new RecordIdDTO("section", 191919);
        final RecordDTO recordDataSection = new RecordDTO();
        recordDataSection.setRecordId(recordIdSection);
        recordDataSection.setContent(asCollection(getRecordContent(recordIdSection, "s")).getBytes());

        final RecordIdDTO recordIdHead = new RecordIdDTO("head", 191919);
        final RecordDTO recordDataHead = new RecordDTO();
        recordDataHead.setRecordId(recordIdHead);
        recordDataHead.setContent(asCollection(getRecordContent(recordIdHead, "h", recordIdRef2)).getBytes());

        when(recordServiceConnector.getRecordDataCollectionDataIO(
                eq(new RecordIdDTO(recordIdVolume.getBibliographicRecordId(), recordIdVolume.getAgencyId())),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new LinkedHashMap<String, RecordDTO>() {{
                    put(recordIdVolume.getBibliographicRecordId(), recordDataVolume);
                    put(recordIdSection.getBibliographicRecordId(), recordDataSection);
                    put(recordIdHead.getBibliographicRecordId(), recordDataHead);
                    put(recordIdRef1.getBibliographicRecordId(), recordDataRef1);
                    put(recordIdRef2.getBibliographicRecordId(), recordDataRef2);
                    put(recordIdRef3.getBibliographicRecordId(), recordDataRef3);
                }});

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
                is(asCollection(
                        getRecordContent(recordIdVolume, "b", recordIdRef1, recordIdRef3),
                        getRecordContent(recordIdSection, "s"),
                        getRecordContent(recordIdHead, "h", recordIdRef2),
                        getRecordContent(recordIdRef1, "e"),
                        getRecordContent(recordIdRef2, "e"),
                        getRecordContent(recordIdRef3, "e")
                )));

    }

    private static String getRecordContent(RecordIdDTO recordId, String type, RecordIdDTO... ref520) {
        String record =
                "<record>" +
                        "<leader>00000n 2200000 4500</leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                        "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
                        "</datafield>" +
                        "<datafield ind1='0' ind2='0' tag='004'>" +
                        "<subfield code='a'>" + type + "</subfield>" +
                        "</datafield>" +
                        "<datafield ind1='0' ind2='0' tag='520'>" +
                        "<subfield code='a'>Originaludgave: 2021</subfield>" +
                        "</datafield>";
        if (ref520 != null) {
            for (RecordIdDTO ref : ref520) {
                // This is a bit ugly, but we test handling af repeated 520n's using the same record twice.
                record +=
                        "<datafield ind1='0' ind2='0' tag='520'>" +
                                "<subfield code='a'>Tidligere udgave: 2021</subfield>" +
                                "<subfield code='n'>" + ref.getBibliographicRecordId() + "</subfield>" +
                                "<subfield code='n'>" + ref.getBibliographicRecordId() + "</subfield>" +
                                "</datafield>";
            }
        }
        record +=
                "</record>";

        return record;
    }

    private static String asCollection(String... records) {
        return
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        String.join("", records) +
                        "</collection>";
    }
}
