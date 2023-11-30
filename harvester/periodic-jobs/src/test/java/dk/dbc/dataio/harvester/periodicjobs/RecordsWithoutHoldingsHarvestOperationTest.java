package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.bfs.api.BinaryFileFsImpl;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.utils.holdingsitems.HoldingsItemsConnector;
import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
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
                    .withSubmitterNumber("870970")
    );

    private final PeriodicJobsHarvesterConfig configWithoutHoldings = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat")
                    .withHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType.STANDARD_WITH_HOLDINGS)
                    .withHoldingsSolrUrl("")
                    .withHoldingsFilter(PeriodicJobsHarvesterConfig.HoldingsFilter.WITHOUT_HOLDINGS)
                    .withSubmitterNumber("870970")
    );

    @Test
    public void addiRecordConfigWithoutHoldings_WithHoldings() throws HarvesterException {
        RecordIdDTO recordIdWithHoldings = new RecordIdDTO("id1", 870970);
        RecordDTO recordDataWithHoldings = new RecordDTO();
        recordDataWithHoldings.setRecordId(recordIdWithHoldings);
        recordDataWithHoldings.setContent("".getBytes());

        when(holdingsItemsConnector.hasHoldings("id1", Set.of())).thenReturn(new HashSet<>(List.of(700300)));
        when(holdingsItemsConnector.hasAnyHoldings(any(), any())).thenCallRealMethod();

        AddiRecord addiRecord = new RecordsWithoutHoldingsHarvestOperation.RecordFetcher(
                recordIdWithHoldings, recordServiceConnector, holdingsItemsConnector, configWithHoldings)
                .call();

        assertThat(new String(addiRecord.getMetaData()), containsString("id1"));
    }

    @Test
    public void addiRecordConfigWithoutHoldings_WithoutHoldings() throws RecordServiceConnectorException, HarvesterException, JSONBException {
        Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        RecordIdDTO recordIdWithoutHoldings = new RecordIdDTO("id1", 870970);
        RecordDTO recordWithoutHoldings = new RecordDTO();
        recordWithoutHoldings.setCreated(creationTime.toString());
        recordWithoutHoldings.setEnrichmentTrail("870970,191919");
        recordWithoutHoldings.setTrackingId(trackingId);
        recordWithoutHoldings.setContent(asCollection(
                getRecordContent(recordIdWithoutHoldings))
                .getBytes());

        when(holdingsItemsConnector.hasHoldings("id1", Set.of())).thenReturn(new HashSet<>(Collections.emptyList()));
        when(holdingsItemsConnector.hasAnyHoldings(any(), any())).thenCallRealMethod();

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordIdDTO(recordIdWithoutHoldings.getBibliographicRecordId(), 191919)),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordIdWithoutHoldings.getBibliographicRecordId(), recordWithoutHoldings);
                }});

        AddiRecord addiRecord = new RecordsWithoutHoldingsHarvestOperation.RecordFetcher(
                recordIdWithoutHoldings, recordServiceConnector, holdingsItemsConnector, configWithoutHoldings)
                .call();

        AddiMetaData addiMetaData = jsonbContext.unmarshall(
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

    @Test
    public void validateQueryUsingHoldings_scenarios() throws HarvesterException, IOException {
        Path originalFile = Paths.get("src/test/resources/record-ids.txt");
        Path copy = Paths.get("target/record-ids.txt");
        Files.copy(originalFile, copy, StandardCopyOption.REPLACE_EXISTING);
        BinaryFileFsImpl recordsIdFile = new BinaryFileFsImpl(copy);
        RecordsWithoutHoldingsHarvestOperation recordsWithoutHoldingsHarvestOperation = mock(RecordsWithoutHoldingsHarvestOperation.class);
        recordsWithoutHoldingsHarvestOperation.holdingsItemsConnector = holdingsItemsConnector;
        recordsWithoutHoldingsHarvestOperation.config = configWithHoldings;
        when(recordsWithoutHoldingsHarvestOperation.validateQuery()).thenCallRealMethod();
        when(recordsWithoutHoldingsHarvestOperation.searchAndPersist(any())).thenReturn(recordsIdFile);
        when(holdingsItemsConnector.hasHoldings("id6", Set.of(987654))).thenReturn(Set.of(987654));
        when(holdingsItemsConnector.hasHoldings("id6", Set.of())).thenReturn(Set.of(987654));
        when(holdingsItemsConnector.hasAnyHoldings(any(), any())).thenCallRealMethod();

        // Case 1: With holdings. Submitter 870970.
        // Should return records with holdings for any agency with holdings on that recordid.
        String expected = "Found 1 record by combined rawrepo solr search and holdingssolr search.";
        String actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found with holdings", actual, is(expected));

        // Case 2: Some other agency (that has no holdings).
        configWithHoldings.getContent().withSubmitterNumber("999999");
        expected = "Found 0 record by combined rawrepo solr search and holdingssolr search.";
        actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found with holdings", actual, is(expected));

        // Case 3: Agency has holding for one.
        configWithHoldings.getContent().withSubmitterNumber("987654");
        expected = "Found 1 record by combined rawrepo solr search and holdingssolr search.";
        actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found with holdings", actual, is(expected));


        // Case 4: WithOUT holdings. Submitter 870970.
        // Should return records with no holdings at all for any agency on that recordid.
        configWithoutHoldings.getContent().withSubmitterNumber("870970");
        recordsWithoutHoldingsHarvestOperation.config = configWithoutHoldings;
        expected = "Found 9 record by combined rawrepo solr search and holdingssolr search.";
        actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found without holdings", actual, is(expected));

        // Case 5: WithOUT holdings. Some other agency.
        // Should return records with no holdings for that agency. (But for all others!).
        configWithoutHoldings.getContent().withSubmitterNumber("999999");
        expected = "Found 10 record by combined rawrepo solr search and holdingssolr search.";
        actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found without holdings", actual, is(expected));

        // Case 6: WithOUT holdings. Agency has holdings for one.
        // Should return records with no holdings for that particular agency.
        configWithoutHoldings.getContent().withSubmitterNumber("987654");
        expected = "Found 9 record by combined rawrepo solr search and holdingssolr search.";
        actual = recordsWithoutHoldingsHarvestOperation.validateQuery();
        assertThat("Found without holdings", actual, is(expected));
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
