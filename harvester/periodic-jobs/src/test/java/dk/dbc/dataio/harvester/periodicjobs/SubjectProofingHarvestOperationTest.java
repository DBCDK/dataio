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
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubjectProofingHarvestOperationTest extends HarvestOperationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final RecordServiceConnector recordServiceConnector = mock(RecordServiceConnector.class);
    private final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
            new PeriodicJobsHarvesterConfig.Content()
                    .withFormat("testFormat"));

    @Test
    public void addiRecord() throws RecordServiceConnectorException, HarvesterException, JSONBException {

        final Instant creationTime = Instant.now();
        final String trackingId = "-trackingId-";

        final RecordId recordId191919 = new RecordId("id191919", 191919);
        final RecordData recordData191919 = mock(RecordData.class);
        when(recordData191919.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordId191919))
                        .getBytes());

        final RecordId recordId190004 = new RecordId("id190004", 190004);
        final RecordData recordData190004 = mock(RecordData.class);
        when(recordData190004.getCreated())
                .thenReturn(creationTime.toString());
        when(recordData190004.getEnrichmentTrail())
                .thenReturn("190004,191919");
        when(recordData190004.getTrackingId())
                .thenReturn(trackingId);
        when(recordData190004.getContent())
                .thenReturn(asCollection(
                        getRecordContent190004(recordId190004, recordId191919.getBibliographicRecordId()))
                        .getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordId(recordId190004.getBibliographicRecordId(), 191919)),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId190004.getBibliographicRecordId(), recordData190004);
                }});
        when(recordServiceConnector.getRecordDataCollection(
                eq(recordId191919),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new HashMap<String, RecordData>() {{
                    put(recordId191919.getBibliographicRecordId(), recordData191919);
                }});

        final AddiRecord addiRecord = new SubjectProofingHarvestOperation.RecordFetcher(
                recordId190004, recordServiceConnector, config)
                .call();

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(
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
                is(asCollection(
                        getRecordContent190004(recordId190004, recordId191919.getBibliographicRecordId()),
                        getRecordContent(recordId191919))));
    }

    private static String getRecordContent190004(RecordId recordId, String bibliographicRecordId) {
        return
                "<record>" +
                  "<leader>00000n 2200000 4500</leader>" +
                  "<datafield ind1='0' ind2='0' tag='001'>" +
                    "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                    "<subfield code='b'>190004</subfield>" +
                  "</datafield>" +
                  "<datafield ind1='0' ind2='0' tag='670'>" +
                    "<subfield code='a'>" + bibliographicRecordId + "</subfield>" +
                  "</datafield>" +
                "</record>";
    }

    private static String getRecordContent(RecordId recordId) {
        return
                "<record>" +
                  "<leader>00000n 2200000 4500</leader>" +
                  "<datafield ind1='0' ind2='0' tag='001'>" +
                    "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                    "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
                  "</datafield>" +
                  "<datafield ind1='0' ind2='0' tag='245'>" +
                    "<subfield code='a'>title</subfield>" +
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