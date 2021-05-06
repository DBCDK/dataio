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

package dk.dbc.dataio.commons.utils.lang;

import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PrettyPrintTest {

    private final String json = "{\"value\":42}";
    private final String text = "text";
    private final String marcXml =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\"></marcx:record>" +
            "</marcx:collection>";

    private final Charset encoding = StandardCharsets.UTF_8;

    @Test
    public void asXml_noneXmlInput_returnsOriginalDataAsString() {
        final String prettyPrint = PrettyPrint.asXml(text.getBytes(), encoding);
        assertThat(prettyPrint, is(text));
    }

    @Test
    public void asXml_xmlInput_returnsPrettyPrintedDataAsString() {
        final String expectedPrettyPrint =
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "\t<marcx:record format=\"danMARC2\"></marcx:record>\n" +
                "</marcx:collection>\n";

        final String prettyPrint = PrettyPrint.asXml(marcXml.getBytes(), encoding);
        assertThat(prettyPrint, is(expectedPrettyPrint));
    }

    @Test
    public void asJson_validJsonInput_returnsPrettyPrintedDataAsString() throws JSONBException {
        final String expectedPrettyPrint = "{\n\t\"value\" : 42\n}";

        final String prettyPrint = PrettyPrint.asJson(json.getBytes(), encoding);
        assertThat(prettyPrint, is(expectedPrettyPrint));
    }

    @Test
    public void asJson_invalidJson_throws() {
        try {
            PrettyPrint.asJson(text.getBytes(), encoding);
            fail("expected exception not thrown");
        } catch (JSONBException e) { }
    }

    @Test
    public void combinePrintElements_elementsCombined_returnsElementsAsString() throws JSONBException {
        final String prettyPrintJson = PrettyPrint.asJson(json.getBytes(), encoding);
        final String prettyPrintXml = PrettyPrint.asXml(marcXml.getBytes(), encoding);
        final String expectedPrint =
                "{\n" +
                    "\t\"value\" : 42\n" +
                "}\n\n" +
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "\t<marcx:record format=\"danMARC2\"></marcx:record>\n" +
                "</marcx:collection>\n";

        final String combinedPrintElements = PrettyPrint.combinePrintElements(prettyPrintJson, prettyPrintXml);
        assertThat(combinedPrintElements, is(expectedPrint));
    }
}
