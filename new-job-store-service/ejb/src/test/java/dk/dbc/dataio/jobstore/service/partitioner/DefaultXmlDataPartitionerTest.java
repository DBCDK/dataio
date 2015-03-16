package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DefaultXmlDataPartitionerTest {
    @Test
    public void emptyRootElement_returnsNoXMLStrings() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel></topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void emptyCollapsedRootElement_returnsNoXMLStrings() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel/>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void singleXMLChild_givesOneStringWithXML() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void moreThanOneXMLChild_givesTheSameAmountOfStringsWithXML() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";
        final String expectedResult1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>";
        final String expectedResult2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult1));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult2));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void missingXMLHeaderInInput_xmlHeaderIsInsertedInResult() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedResult));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void errornousXMLContainingOnlyRootStartElement_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void errornousXMLContainingUnfinishedFirstChild_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grand";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));

        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void errornousXMLWrongNesting_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</child></grandChild></topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlActualEncodingDiffersFromDeclared_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>æøå</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml, StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlContainsIllegalAmpersand_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a single Ampersand: & which is not legal</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Larger Than sign: &gt; which is legal</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlContainsQuotationMark_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Quotation Mark: \" which is legal</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlContainsApostroph_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is an Aprostroph: ' which is legal</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlTagStartsWithColon_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<:test>"
                + "<child1>child text</child1>"
                + "</:test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlTagStartsWithUnderscore_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_test>"
                + "<child1>child text</child1>"
                + "</_test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlTagWithLegalSpecialCharacters_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_-.9>"
                + "<child1>child text</child1>"
                + "</_-.9>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlTagContainsWhiteSpace_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test is good>"
                + "<child1>This is a good test</child1>"
                + "</test is good>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!-- declarations for <head> & <body> -->"
                + "<test>"
                + "<!-- comment in top level -->"
                + "<child1>child text</child1>"
                + "<!-- comment in sub level -->"
                + "</test>";  // The trailing comment is removed
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlCommentsDashDash_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash -- is not legal -->"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlAttributeWithApostrophs_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='2'>What is the size here?</child1>"
                + "</test>";
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2\">What is the size here?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlAttributeWithoutQuotation_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=2>What is the size here?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Larger than: &gt; \">What is this?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlAttributeValueContainsQuotationMark_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: \" \">What is this?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(xml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlApostrophAttributeValueContainsQuotationMark_accepted() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Quotation Mark: \" '>What is this?</child1>"
                + "</test>";
        final String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: &quot; \">What is this?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

        final Iterator<String> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is(Long.valueOf(xml.getBytes(StandardCharsets.UTF_8).length)));
    }

    @Test
    public void xmlApostrophAttributeValueContainsApostroph_throws() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Apostroph: ' '>What is this?</child1>"
                + "</test>";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.UTF_8.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.ISO_8859_1.name());

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
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream(xml), StandardCharsets.ISO_8859_1.name());

        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void getEncoding_returnsCanonicalEncoding() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream("<test/>"), "utf8");
        assertThat(dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void getEncoding_illegalCharsetNameException_throws() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream("<test/>"), "[ILLEGAL_CHARSET_NAME]");
        try {
            dataPartitioner.getEncoding();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void getEncoding_UnsupportedCharsetException_throws() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream("<test/>"), "UNKNOWN_CHARSET_NAME");
        try {
            dataPartitioner.getEncoding();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void iterator_illegalCharsetNameException_throws() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream("<test/>"), "[ILLEGAL_CHARSET_NAME]");
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    @Test
    public void iterator_UnsupportedCharsetException_throws() {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(asInputStream("<test/>"), "UNKNOWN_CHARSET_NAME");
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) {
        }
    }

    private InputStream asInputStream(String xml, Charset encoding) {
        return new ByteArrayInputStream(xml.getBytes(encoding));
    }

    private InputStream asInputStream(String xml) {
        return asInputStream(xml, StandardCharsets.UTF_8);
    }
}