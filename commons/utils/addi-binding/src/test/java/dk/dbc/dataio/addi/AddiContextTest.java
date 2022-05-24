/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.addi;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.addi.bindings.EsReferenceData;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddiContextTest {
    public static final String ES_REFERENCE_DATA_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">%s%s%s</es:referencedata>";

    public static final String ES_DIRECTIVES =
            "<es:info submitter=\"820040\" format=\"katalog\" language=\"dan\" contentFrom=\"820040\"/>";

    public static final String SINK_DIRECTIVES =
            "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>";

    public static final String UPDATE_SINK_DIRECTIVES =
            "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\" updateTemplate=\"template\"/>";

    private final AddiContext objectUnderTest = new AddiContext();
    private final AddiRecord addiRecord = createAddiRecord();

    @Test
    public void getEsReferenceData_addiRecordArgIsNull_throws() {
        assertThat(() -> objectUnderTest.getEsReferenceData(null), isThrowing(AddiException.class));
    }

    @Test
    public void getEsReferenceData_addiRecordContainsMetaDataWithInvalidXml_throws() throws AddiException {
        final AddiRecord addiRecord = createAddiRecord("not xml");
        assertThat(() -> objectUnderTest.getEsReferenceData(addiRecord), isThrowing(AddiException.class));
    }

    @Test
    public void getEsReferenceData_addiRecordContains_throws() throws AddiException {
        final AddiRecord addiRecord = createAddiRecord(String.format(ES_REFERENCE_DATA_XML_TEMPLATE, "<unknown-element />", "", ""));
        assertThat(() -> objectUnderTest.getEsReferenceData(addiRecord), isThrowing(AddiException.class));
    }

    @Test
    public void getEsReferenceData_addiRecordContainsEsReferenceData_returns() throws AddiException {
        final EsReferenceData esReferenceData = objectUnderTest.getEsReferenceData(addiRecord);
        assertThat("EsReferenceData", esReferenceData, is(notNullValue()));
        assertThat("EsDirectives", esReferenceData.esDirectives, is(notNullValue()));
        assertThat("SinkDirectives", esReferenceData.sinkDirectives, is(notNullValue()));
        assertThat("UpdateSinkDirectives", esReferenceData.updateSinkDirectives, is(notNullValue()));
    }

    private AddiRecord createAddiRecord() {
        return createAddiRecord(String.format(ES_REFERENCE_DATA_XML_TEMPLATE, ES_DIRECTIVES, SINK_DIRECTIVES, UPDATE_SINK_DIRECTIVES));
    }

    private AddiRecord createAddiRecord(String meta) {
        return new AddiRecord(meta.trim().getBytes(StandardCharsets.UTF_8), null);
    }
}
