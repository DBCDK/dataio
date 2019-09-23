/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
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
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 870970);

        new RecordFetcher(recordId, recordServiceConnector, config).call();

        verify(recordServiceConnector).getRecordDataCollection(
                eq(new RecordData.RecordId("id", 191919)),
                any(RecordServiceConnector.Params.class));
    }

    @Test
    public void recordServiceConnectorFetchRecordCollectionThrows()
            throws RecordServiceConnectorException, HarvesterException {
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);

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
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);
        final RecordData recordData = mock(RecordData.class);

        when(recordData.getCreated()).thenReturn(Instant.now().toString());
        when(recordData.getEnrichmentTrail()).thenReturn("191919,870970");
        when(recordData.getContent()).thenReturn("invalidXML".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "member data can not be parsed as marcXchange");
    }

    @Test
    public void recordHasNoCreationDate()
            throws HarvesterException, RecordServiceConnectorException {
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);
        final RecordData recordData = mock(RecordData.class);

        when(recordData.getCreated()).thenReturn(null);
        when(recordData.getEnrichmentTrail()).thenReturn("191919,870970");
        when(recordData.getContent()).thenReturn("<record xmlns='info:lc/xmlns/marcxchange-v1'/>".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "Record creation date is null");
    }

    @Test
    public void emptyCollection()
            throws HarvesterException, RecordServiceConnectorException {
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);
        final RecordData recordData = mock(RecordData.class);

        when(recordData.getCreated()).thenReturn(Instant.now().toString());
        when(recordData.getEnrichmentTrail()).thenReturn("191919,870970");
        when(recordData.getContent()).thenReturn("<record xmlns='info:lc/xmlns/marcxchange-v1'/>".getBytes());

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
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);
        final RecordData recordData = mock(RecordData.class);

        when(recordData.getCreated()).thenReturn(Instant.now().toString());
        when(recordData.getEnrichmentTrail()).thenReturn("191919,870970");
        when(recordData.getContent()).thenReturn("<record xmlns='info:lc/xmlns/marcxchange-v1'/>".getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put("notId", recordData);
                }});

        assertHasDiagnostic(new RecordFetcher(recordId, recordServiceConnector, config).call(),
                "was not found in returned collection");
    }

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException {
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 123456);
        final RecordData recordData = mock(RecordData.class);

        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        when(recordData.getCreated()).thenReturn(creationTime.toString());
        when(recordData.getTrackingId()).thenReturn(trackingId);
        when(recordData.getContent()).thenReturn(getRecordContent(recordId).getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        final AddiRecord addiRecord = new RecordFetcher(recordId, recordServiceConnector, config).call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
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
        final RecordData.RecordId recordId = new RecordData.RecordId("id", 191919);
        final RecordData recordData = mock(RecordData.class);

        when(recordData.getCreated()).thenReturn(Instant.now().toString());
        when(recordData.getEnrichmentTrail()).thenReturn("191919,870970");
        when(recordData.getContent()).thenReturn(getRecordContent(recordId).getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId.getBibliographicRecordId(), recordData);
                }});

        final AddiRecord addiRecord = new RecordFetcher(recordId, recordServiceConnector, config).call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), AddiMetaData.class);

        assertThat("Addi metadata enrichment trail", addiMetaData.enrichmentTrail(),
                is("191919,870970"));
        assertThat("Addi metadata submitter", addiMetaData.submitterNumber(),
                is(870970));
    }

    private static String getRecordContent(RecordData.RecordId recordId) {
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
            final AddiMetaData addiMetaData = jsonbContext.unmarshall(
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