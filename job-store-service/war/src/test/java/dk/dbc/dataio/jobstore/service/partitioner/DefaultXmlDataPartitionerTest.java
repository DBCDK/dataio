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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class DefaultXmlDataPartitionerTest extends AbstractPartitionerTestBase{

    @Test
    public void emptyRootElement_returnsNoXMLStrings() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel></topLevel>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), getUft8Encoding());

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void emptyCollapsedRootElement_returnsNoXMLStrings() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel/>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void singleXMLChild_givesOneStringWithXML() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final ChunkItem expectedResult = new ChunkItemBuilder().setData(xml).build();
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void moreThanOneXMLChild_givesTheSameAmountOfStringsWithXML() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";
        final ChunkItem expectedResult1 = new ChunkItemBuilder().setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>").build();
        final ChunkItem expectedResult2 = new ChunkItemBuilder().setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult1));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult2));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void missingXMLHeaderInInput_xmlHeaderIsInsertedInResult() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final ChunkItem expectedResult = new ChunkItemBuilder()
                .setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void errornousXMLContainingOnlyRootStartElement_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void errornousXMLContainingUnfinishedFirstChild_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grand";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void erroneousXMLWrongNesting_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</child></grandChild></topLevel>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void errornousXMLContainingUnfinishedSecondChild_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grand";
        final ChunkItem expectedResult = new ChunkItemBuilder().setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult));
        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlWithDefaultNamespace() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test xmlns=\"default\" xmlns:prefix=\"http://uri\">"
                + "<child1 id=\"1\">default ns</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();

        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlActualEncodingDiffersFromDeclared_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>æøå</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml, StandardCharsets.ISO_8859_1), getUft8Encoding());
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void rootTagNameWitNamespacePrefix() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ns:test xmlns:ns=\"http://uri\">"
                + "<child1>æøå</child1>"
                + "</ns:test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsIllegalAmpersand_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a single Ampersand: & which is not legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlContainsIllegalLessThanSign_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Less Than sign: < which is not legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlContainsIllegalLargerThanSign_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Larger Than sign: > which is legal</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<test>"
                        + "<child1>This is a Larger Than sign: &gt; which is legal</child1>"
                        + "</test>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsQuotationMark_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Quotation Mark: \" which is legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsApostroph_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is an Aprostroph: ' which is legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagStartsWithColon_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<:test>"
                + "<child1>child text</child1>"
                + "</:test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagStartsWithUnderscore_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_test>"
                + "<child1>child text</child1>"
                + "</_test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expceted = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expceted));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagWithLegalSpecialCharacters_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_-.9>"
                + "<child1>child text</child1>"
                + "</_-.9>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagContainsWhiteSpace_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test is good>"
                + "<child1>This is a good test</child1>"
                + "</test is good>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlComments_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!-- declarations for <head> & <body> -->"
                + "<test>"
                + "<!-- comment in top level -->"
                + "<child1>child text</child1>"
                + "<!-- comment in sub level -->"
                + "</test>"
                + "<!-- trailing comment -->";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<!-- declarations for <head> & <body> -->"
                        + "<test>"
                        + "<!-- comment in top level -->"
                        + "<child1>child text</child1>"
                        + "<!-- comment in sub level -->"
                        + "</test>")  // The trailing comment is removed
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlCommentsDashDash_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash -- is not legal -->"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlCommentsDashDashDashLargerThan_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash Larger Than used as a comment end is not legal: --->"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeWithQuotation_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeWithApostrophs_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='2'>What is the size here?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<test>"
                        + "<child1 size=\"2\">What is the size here?</child1>"
                        + "</test>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeWithoutQuotation_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=2>What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeWithMissingStartQuotation_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeWithMissingEndQuotation_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2>What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlIllegalAttributeName_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 2size=\"2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeValueContainsAmpersand_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Ampersand: & \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeValueContainsLessThanSign_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Less than: < \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeValueContainsLargerThanSign_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Larger than: > \">What is this?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<test>"
                        + "<child1 size=\"Larger than: &gt; \">What is this?</child1>"
                        + "</test>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeValueContainsQuotationMark_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: \" \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void xmlAttributeValueContainsApostroph_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Apostroph: ' \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlApostrophAttributeValueContainsQuotationMark_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Quotation Mark: \" '>What is this?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder().setData("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: &quot; \">What is this?</child1>"
                + "</test>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlApostrophAttributeValueContainsApostroph_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Apostroph: ' '>What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void expectedEncodingDiffersFromActualEncoding_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>data</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.ISO_8859_1.name());

        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void expectedEncodingDiffersFromDefaultEncoding_throws() {
        final String xml = "<?xml version=\"1.0\"?>"
                + "<test>"
                + "<child1>data</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.ISO_8859_1.name());

        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void getEncoding_returnsCanonicalEncoding() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), "utf8");
        assertThat(dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void getEncoding_illegalCharsetNameException_throws() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), "[ILLEGAL_CHARSET_NAME]");
        try {
            dataPartitioner.getEncoding();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void getEncoding_UnsupportedCharsetException_throws() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), "UNKNOWN_CHARSET_NAME");
        try {
            dataPartitioner.getEncoding();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void iterator_illegalCharsetNameException_throws() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream("<test/>"), "[ILLEGAL_CHARSET_NAME]");
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void iterator_UnsupportedCharsetException_throws() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream("<test/>"), "UNKNOWN_CHARSET_NAME");
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            DefaultXmlDataPartitioner.newInstance(null, getUft8Encoding());
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), getUft8Encoding()), is(notNullValue()));
    }

    @Test
    public void iterator_next_returnsChunkItemsWithoutTrackingId() {
        final DataPartitioner dataPartitioner = newPartitionerInstance(getDataContainerXmlWithMarcExchangeAndTrackingIds());
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem0 = iterator.next();
        assertThat("chunkItem0.trackingId", chunkItem0.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem1 = iterator.next();
        assertThat("chunkItem1.trackingId", chunkItem1.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem2 = iterator.next();
        assertThat("chunkItem1.trackingId", chunkItem2.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(false));
    }

    private DataPartitioner newPartitionerInstance(String xml) {
        return DefaultXmlDataPartitioner.newInstance(asInputStream(xml), getUft8Encoding());
    }
}