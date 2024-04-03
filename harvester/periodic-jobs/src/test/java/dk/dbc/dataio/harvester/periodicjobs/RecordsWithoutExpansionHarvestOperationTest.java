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
import org.junit.jupiter.api.Test;

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

class RecordsWithoutExpansionHarvestOperationTest extends HarvestOperationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException {

        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordIdDTO recordId = new RecordIdDTO("rec", 191919);
        final RecordDTO recordData = new RecordDTO();
        recordData.setRecordId(recordId);
        recordData.setContent(asCollection(getRecordContent(recordId)).getBytes());
        recordData.setCreated(creationTime.toString());
        recordData.setEnrichmentTrail("191919");
        recordData.setTrackingId(trackingId);

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordIdDTO(recordId.getBibliographicRecordId(), recordId.getAgencyId())),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new LinkedHashMap<>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

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
                is(asCollection(getRecordContent(recordId))));

    }

    private static String getRecordContent(RecordIdDTO recordId) {
        return
                "<record>" +
                        "<leader>00000n 2200000 4500</leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                        "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
                        "</datafield>" +
                        "<datafield ind1='0' ind2='0' tag='100'>" +
                        "<subfield code='5'>870979</subfield>" +
                        "<subfield code='6'>134629681</subfield>" +
                        "<subfield code='4'>aut</subfield>" +
                        "</datafield>" +
                "</record>";
    }

    private static String asCollection(String... records) {
        return
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        String.join("", records) +
                        "</collection>";
    }
}