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

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

public class XmlDiffGeneratorTest extends AbstractDiffGeneratorTest {

    // xmllint + diff cannot handle default >< explicit namespaces
    @Ignore
    @Test
    public void testGetDiff_semanticEqual_returnsEmptyString() throws DiffGeneratorException {
        ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
        String diff = xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML,
                getXml(), getXmlSemanticEquals());
        assertThat(diff, is(""));
    }

    @Test
    public void testGetDiff_different_returnsDiffString() throws DiffGeneratorException {
        if (canXmlDiff()) {
            final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            final String diff = xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML, getXml(), getXmlNext());
            assertThat(diff, not(""));
        }
    }

    @Test
    public void testGetDiff_bug18965() throws DiffGeneratorException, IOException, URISyntaxException {
        if (canXmlDiff()) {
            final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            final String diff = xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML,
                readTestRecord("bug_18956.xml"),
                readTestRecord("bug_18956-differences.xml"));
            assertThat(diff, not(""));
        }
    }


    @Test
    public void testGetDiff_output() throws DiffGeneratorException, IOException, URISyntaxException {
        if (canXmlDiff()) {
            final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            final String diff = xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML,
                    readTestRecord("small-current.xml"),
                    readTestRecord("small-next.xml"));
            assertThat(diff, not(""));
        }
    }

    @Test
    public void testGetDiff_contentEquals_returnsEmptyString() throws DiffGeneratorException {
        if (canXmlDiff()) {
            final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            final String diff = xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML, getXml(), getXml());
            assertThat(diff, is(""));
        }
    }

    @Test
    public void testGetDiff_failureComparingInput_throws() {
        if (canXmlDiff()) {
            try {
                final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
                xmlDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML, "<INVALID>".getBytes(), "<INVALID>".getBytes());
                fail("No DiffGeneratorException thrown");
            } catch (DiffGeneratorException e) {}
        }
    }

    private byte[] getXml() {
        return ("<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Moon</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private byte[] getXmlNext() {
        return ("<dataio-harvester-datafile>" +
                "<data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader> " +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Sun</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private byte[] getXmlSemanticEquals() {
        return ("<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data><foo:collection xmlns:foo=\"info:lc/xmlns/marcxchange-v1\">" +
                "<foo:record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<foo:leader>00000n 2200000 4500</foo:leader>" +
                "<foo:datafield ind2=\"0\" ind1=\"0\" tag=\"001\">" +
                "<foo:subfield code=\"a\">Sun Kil Moon</foo:subfield>" +
                "</foo:datafield>" +
                "</foo:record></foo:collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }


    static byte[] readTestRecord(String resourceName) throws IOException, URISyntaxException {
        final java.net.URL url = XmlDiffGeneratorTest.class.getResource("/" + resourceName);
        final java.nio.file.Path resPath;
        resPath = java.nio.file.Paths.get(url.toURI());
        return java.nio.file.Files.readAllBytes(resPath);
    }

}
