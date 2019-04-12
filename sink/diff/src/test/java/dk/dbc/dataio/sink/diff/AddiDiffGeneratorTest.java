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

package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Ignore("Since the tests are not run in a docker container" +
        " where we ca be sure that the binaries called by " +
        "the external tool exist.")
public class AddiDiffGeneratorTest extends AbstractDiffGeneratorTest {
    public static final String XML_METADATA =
            "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
              "<es:info format=\"currentFormat\" language=\"dan\" submitter=\"870970\"/>" +
            "</es:referencedata>";
    public static final String XML_METADATA_NEXT =
            "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
              "<es:info format=\"nextFormat\" language=\"dan\" submitter=\"870970\"/>" +
            "</es:referencedata>";
    public static final String XML_CONTENT =
            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\">" +
              "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">currentTitle</marcx:subfield>" +
              "</marcx:datafield>" +
            "</marcx:record>";
    public static final String XML_CONTENT_NEXT =
            "<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\">" +
              "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">nextTitle</marcx:subfield>" +
              "</marcx:datafield>" +
            "</marcx:record>";

    private static final String JSON_METADATA = "{\"format\": \"currentFormat\"}";
    private static final String JSON_METADATA_NEXT = "{\"format\": \"nextFormat\"}";

    private static final String EMPTY = "";

    private final AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator();

    @Before
    public void setup() {
        addiDiffGenerator.externalToolDiffGenerator = newExternalToolDiffGenerator();
    }

    @Test
    public void addiRecordsAreIdentical_returnsEmptyString() throws DiffGeneratorException {
        final AddiRecord addiRecord = getAddiRecord(XML_METADATA, XML_CONTENT);
        assertThat(addiDiffGenerator.getDiff(addiRecord, addiRecord), is(EMPTY));
    }

    @Test
    public void xmlMetaDataIsNotIdentical_returnsDiff() throws DiffGeneratorException {
        final AddiRecord current = getAddiRecord(XML_METADATA, XML_CONTENT);
        final AddiRecord next = getAddiRecord(XML_METADATA_NEXT, XML_CONTENT);

        final String diff = addiDiffGenerator.getDiff(current, next);

        // Assert that the diff contains meta data
        assertThat("diff contains 'nextFormat'", diff.contains("nextFormat"), is(true));
        assertThat("diff contains 'currentFormat'", diff.contains("currentFormat"), is(true));

        // Assert that the diff does not contain content data
        assertThat("diff contains 'currentTitle'", diff.contains("currentTitle"), is(false));
    }

    @Test
    public void xmlContentIsNotIdentical_returnsDiff() throws DiffGeneratorException {
        final AddiRecord current = getAddiRecord(XML_METADATA, XML_CONTENT);
        final AddiRecord next = getAddiRecord(XML_METADATA, XML_CONTENT_NEXT);

        final String diff = addiDiffGenerator.getDiff(current, next);

        // Assert that the diff contains content data
        assertThat("diff contains 'currentTitle'", diff.contains("currentTitle"), is(true));
        assertThat("diff contains 'nextTitle'", diff.contains("nextTitle"), is(true));

        // Assert that the diff does not contain meta data
        assertThat("diff contains 'currentFormat'", diff.contains("currentFormat"), is(false));
    }

    @Test
    public void xmlContentAndXmlMetaDataAreNotIdentical_returnsDiff() throws DiffGeneratorException {
        final AddiRecord current = getAddiRecord(XML_METADATA, XML_CONTENT);
        final AddiRecord next = getAddiRecord(XML_METADATA_NEXT, XML_CONTENT_NEXT);

        final String diff = addiDiffGenerator.getDiff(current, next);

        // Assert that the diff contains meta data
        assertThat("diff contains 'nextFormat'", diff.contains("nextFormat"), is(true));
        assertThat("diff contains 'currentFormat'", diff.contains("currentFormat"), is(true));

        // Assert that the diff contains content data
        assertThat("diff contains 'currentTitle'", diff.contains("currentTitle"), is(true));
        assertThat("diff contains 'nextTitle'", diff.contains("nextTitle"), is(true));
    }

    @Test
    public void invalidXmlMetadata_throws() {
        final AddiRecord current = getAddiRecord("<meta>", XML_CONTENT);
        final AddiRecord next = getAddiRecord(XML_METADATA_NEXT, XML_CONTENT_NEXT);
        assertThat(() -> addiDiffGenerator.getDiff(current, next), isThrowing(DiffGeneratorException.class));
    }

    @Test
    public void jsonMetaDataIsNotIdentical_returnsDiff() throws DiffGeneratorException {
        final AddiRecord current = getAddiRecord(JSON_METADATA, XML_CONTENT);
        final AddiRecord next = getAddiRecord(JSON_METADATA_NEXT, XML_CONTENT);

        final String diff = addiDiffGenerator.getDiff(current, next);

        // Assert that the diff contains meta data
        assertThat("diff contains 'nextFormat'", diff.contains("nextFormat"), is(true));
        assertThat("diff contains 'currentFormat'", diff.contains("currentFormat"), is(true));

        // Assert that the diff does not contain content data
        assertThat("diff contains 'currentTitle'", diff.contains("currentTitle"), is(false));
    }

    @Test
    public void jsonMetaDataIsIdentical_returnsEmptyString() throws DiffGeneratorException {
        final AddiRecord current = getAddiRecord(JSON_METADATA, XML_CONTENT);
        final AddiRecord next = getAddiRecord(JSON_METADATA, XML_CONTENT);

        assertThat(addiDiffGenerator.getDiff(current, next), is(EMPTY));
    }

    public static AddiRecord getAddiRecord(String metadata, String content) {
        return new AddiRecord(
                metadata.trim().getBytes(StandardCharsets.UTF_8),
                content.trim().getBytes(StandardCharsets.UTF_8));
    }
}
