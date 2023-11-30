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
import java.util.HashMap;

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
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
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

    @Test
    public void recordHasInvalidXmlContent()
            throws HarvesterException, RecordServiceConnectorException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);

        RecordDTO recordData = new RecordDTO();
        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent("invalidXML".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "member data can not be parsed as marcXchange");
    }

    @Test
    public void recordHasNoCreationDate()
            throws HarvesterException, RecordServiceConnectorException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordDTO recordData = new RecordDTO();
        recordData.setCreated(null);
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent("<record xmlns='info:lc/xmlns/marcxchange-v1'/>".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

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
                .thenReturn(new HashMap<>());

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "Empty record collection returned");
    }

    @Test
    public void recordIdNotInCollection()
            throws HarvesterException, RecordServiceConnectorException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordDTO recordData = new RecordDTO();
        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent("<record xmlns='info:lc/xmlns/marcxchange-v1'/>".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put("notId", recordData);
                }});

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "was not found in returned collection");
    }

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException {
        RecordIdDTO recordId = new RecordIdDTO("id", 123456);
        RecordDTO recordData = new RecordDTO();

        Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        recordData.setCreated(Instant.now().toString());
        recordData.setTrackingId(trackingId);
        recordData.setContent(getRecordContent(recordId).getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

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
                is(getRecordContent(recordId)));
    }

    @Test
    public void getSubmitterFromEnrichmentTrail()
            throws RecordServiceConnectorException, HarvesterException, JSONBException {
        RecordIdDTO recordId = new RecordIdDTO("id", 191919);
        RecordDTO recordData = new RecordDTO();

        recordData.setCreated(Instant.now().toString());
        recordData.setEnrichmentTrail("191919,870970");
        recordData.setContent(getRecordContent(recordId).getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        AddiRecord addiRecord = new RecordFetcher(recordId, recordServiceConnector, config).call();

        AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata enrichment trail", addiMetaData.enrichmentTrail(),
                is("191919,870970"));
        assertThat("Addi metadata submitter", addiMetaData.submitterNumber(),
                is(870970));
    }

    private static String getRecordContent(RecordIdDTO recordId) {
        return
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<collection xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        "<record>" +
                        "<leader>00000n 2200000 4500</leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                        "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
                        "</datafield>" +
                        "<datafield ind1='0' ind2='0' tag='245'>" +
                        "<subfield code='a'>title</subfield>" +
                        "</datafield>" +
                        "</record>" +
                        "</collection>";
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
