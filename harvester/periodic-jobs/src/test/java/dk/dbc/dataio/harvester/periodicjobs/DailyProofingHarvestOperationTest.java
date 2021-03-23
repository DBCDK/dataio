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

        final RecordId recordIdRef1 = new RecordId("ref1", 191919);
        final RecordData recordDataRef1 = mock(RecordData.class);
        when(recordDataRef1.getRecordId())
                .thenReturn(recordIdRef1);
        when(recordDataRef1.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordIdRef1, "e", null))
                        .getBytes());

        final RecordId recordIdRef2 = new RecordId("ref2", 191919);
        final RecordData recordDataRef2 = mock(RecordData.class);
        when(recordDataRef2.getRecordId())
                .thenReturn(recordIdRef2);
        when(recordDataRef2.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordIdRef2, "e", null))
                        .getBytes());

        final RecordId recordIdVolume = new RecordId("volume", 191919);
        final RecordData recordDataVolume = mock(RecordData.class);
        when(recordDataVolume.getRecordId())
                .thenReturn(recordIdVolume);
        when(recordDataVolume.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordIdVolume, "b", recordIdRef1))
                        .getBytes());
        when(recordDataVolume.getCreated())
                .thenReturn(creationTime.toString());
        when(recordDataVolume.getEnrichmentTrail())
                .thenReturn("191919");
        when(recordDataVolume.getTrackingId())
                .thenReturn(trackingId);

        final RecordId recordIdSection = new RecordId("section", 191919);
        final RecordData recordDataSection = mock(RecordData.class);
        when(recordDataSection.getRecordId())
                .thenReturn(recordIdSection);
        when(recordDataSection.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordIdSection, "s", null))
                        .getBytes());

        final RecordId recordIdHead = new RecordId("head", 191919);
        final RecordData recordDataHead = mock(RecordData.class);
        when(recordDataHead.getRecordId())
                .thenReturn(recordIdHead);
        when(recordDataHead.getContent())
                .thenReturn(asCollection(
                        getRecordContent(recordIdHead, "h", recordIdRef2))
                        .getBytes());

        when(recordServiceConnector.getRecordDataCollection(
                eq(new RecordId(recordIdVolume.getBibliographicRecordId(), recordIdVolume.getAgencyId())),
                any(RecordServiceConnector.Params.class)))
                .thenReturn(new LinkedHashMap<String, RecordData>() {{
                    put(recordIdVolume.getBibliographicRecordId(), recordDataVolume);
                    put(recordIdSection.getBibliographicRecordId(), recordDataSection);
                    put(recordIdHead.getBibliographicRecordId(), recordDataHead);
                }});

        when(recordServiceConnector.getRecordData(eq(recordIdRef1), any(RecordServiceConnector.Params.class)))
                .thenReturn(recordDataRef1);
        when(recordServiceConnector.getRecordData(eq(recordIdRef2), any(RecordServiceConnector.Params.class)))
                .thenReturn(recordDataRef2);


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
                        getRecordContent(recordIdVolume, "b", recordIdRef1),
                        getRecordContent(recordIdSection, "s", null),
                        getRecordContent(recordIdHead, "h", recordIdRef2),
                        getRecordContent(recordIdRef1, "e", null),
                        getRecordContent(recordIdRef2, "e", null)
                        )));
                        
    }

    private static String getRecordContent(RecordId recordId, String type, RecordId ref520) {
        return
                "<record>" +
                  "<leader>00000n 2200000 4500</leader>" +
                    "<datafield ind1='0' ind2='0' tag='001'>" +
                    "<subfield code='a'>" + recordId.getBibliographicRecordId() + "</subfield>" +
                    "<subfield code='b'>" + recordId.getAgencyId() + "</subfield>" +
                    "</datafield>" +
                  "<datafield ind1='0' ind2='0' tag='004'>" +
                    "<subfield code='a'>" + type + "</subfield>" +
                  "</datafield>" +
                  (ref520 != null ?
                  "<datafield ind1='0' ind2='0' tag='520'>" +
                    "<subfield code='n'>" + ref520.getBibliographicRecordId() + "</subfield>" +
                  "</datafield>" : "") +
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