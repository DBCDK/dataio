package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecordsWithoutHoldingsHarvestOperationTest extends HarvestOperationTest {

    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final HoldingsItemsConnector holdingsItemsConnector = mock(HoldingsItemsConnector.class);
    private final PeriodicJobsHarvesterConfig configWithHoldings = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat")
                    .withHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType.STANDARD_WITH_HOLDINGS)
                    .withHoldingsSolrUrl("")
                    .withHoldingsFilter(PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS)
    );

    private final PeriodicJobsHarvesterConfig configWithoutHoldings = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat")
                    .withHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType.STANDARD_WITH_HOLDINGS)
                    .withHoldingsSolrUrl("")
                    .withHoldingsFilter(PeriodicJobsHarvesterConfig.HoldingsFilter.WITHOUT_HOLDINGS)
    );

    @Test
    public void addiRecordConfigWithoutHoldings_WithHoldings() throws HarvesterException {
        final RecordIdDTO recordIdWithHoldings = new RecordIdDTO("id1", 870970);
        final RecordDTO recordDataWithHoldings = new RecordDTO();
        recordDataWithHoldings.setRecordId(recordIdWithHoldings);
        recordDataWithHoldings.setContent("".getBytes());

        when(holdingsItemsConnector.hasHoldings("id1", null)).thenReturn(new HashSet<>(List.of(700300)));

        final AddiRecord addiRecord = new RecordsWithoutHoldingsHarvestOperation.RecordFetcher(
                recordIdWithHoldings, recordServiceConnector, holdingsItemsConnector, configWithoutHoldings)
                .call();

        assertThat(addiRecord, nullValue());
    }

    @Test
    public void addiRecordConfigWithoutHoldings_WithoutHoldings() throws RecordServiceConnectorException, HarvesterException, JSONBException {
        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordIdDTO recordIdWithoutHoldings = new RecordIdDTO("id1", 870970);
        final RecordDTO recordWithoutHoldings = new RecordDTO();
        recordWithoutHoldings.setCreated(creationTime.toString());
        recordWithoutHoldings.setEnrichmentTrail("870970,191919");
        recordWithoutHoldings.setTrackingId(trackingId);
        recordWithoutHoldings.setContent(asCollection(
                getRecordContent(recordIdWithoutHoldings))
                .getBytes());

        when(holdingsItemsConnector.hasHoldings("id1", null)).thenReturn(new HashSet<>(Collections.emptyList()));

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordIdDTO(recordIdWithoutHoldings.getBibliographicRecordId(), 191919)),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordIdWithoutHoldings.getBibliographicRecordId(), recordWithoutHoldings);
                }});

        final AddiRecord addiRecord = new RecordsWithoutHoldingsHarvestOperation.RecordFetcher(
                recordIdWithoutHoldings, recordServiceConnector, holdingsItemsConnector, configWithoutHoldings)
                .call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata", addiMetaData, is(new AddiMetaData()
                .withBibliographicRecordId(recordIdWithoutHoldings.getBibliographicRecordId())
                .withSubmitterNumber(870970)
                .withEnrichmentTrail("870970,191919")
                .withFormat(configWithHoldings.getContent().getFormat())
                .withCreationDate(Date.from(creationTime))
                .withTrackingId(trackingId)
                .withLibraryRules(new AddiMetaData.LibraryRules())));

        assertThat("Addi content", new String(addiRecord.getContentData(), StandardCharsets.UTF_8),
                is(asCollection(
                        getRecordContent(recordIdWithoutHoldings))));
    }

    private static String getRecordContent(RecordIdDTO recordId) {
        return
                "<record>" +
                        "<leader>00000n 2200000 4500</leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                        "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
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
