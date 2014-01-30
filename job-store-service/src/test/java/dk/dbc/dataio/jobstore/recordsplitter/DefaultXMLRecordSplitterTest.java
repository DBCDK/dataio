package dk.dbc.dataio.jobstore.recordsplitter;

import dk.dbc.dataio.jobstore.types.IllegalDataException;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultXMLRecordSplitterTest {

    private static final String UTF8_CHARSET = "UTF-8";

    @Test(expected = NullPointerException.class)
    public void testInputStreamIsNull_throwsException() throws XMLStreamException {
        new DefaultXMLRecordSplitter(null);
    }

    @Test
    public void testEmptyRootElement_returnsNoXMLStrings() throws XMLStreamException, UnsupportedEncodingException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel></topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testEmptyCollapsedRootElement_returnsNoXMLStrings() throws XMLStreamException, UnsupportedEncodingException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel/>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testSingleXMLChild_givesOneStringWithXML() throws XMLStreamException, UnsupportedEncodingException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testMoreThanOneXMLChild_givesTheSameAmountOfStringsWithXML() throws XMLStreamException, UnsupportedEncodingException {
        String originalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";
        String expectedResult1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>";
        String expectedResult2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(originalXml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedResult1));
        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedResult2));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testMissingXMLHeaderInInputIsInsertedInResult() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedResult));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testMissingXMLHeaderDefaultEncodingIsReturned() throws UnsupportedEncodingException, XMLStreamException {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";

        final DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        final Iterator<String> it = xmlRecordSplitter.iterator();
        assertThat(it.hasNext(), is(true));
        assertThat(xmlRecordSplitter.getEncoding(), is(StandardCharsets.UTF_8.name()));
    }

    @Test(expected = IllegalDataException.class)
    public void testErrornousXMLContainingOnlyRootStartElement_throwsException() throws XMLStreamException, UnsupportedEncodingException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>";
        new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
    }

    @Test(expected = IllegalDataException.class)
    public void testErrornousXMLContainingUnfinishedFirstChild_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grand";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testErrornousXMLWrongNesting_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</child></grandChild></topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testErrornousXMLContainingUnfinishedSecondChild_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grand";

        String expectedResult1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedResult1));
        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLWithSimpleNamespaces() throws XMLStreamException, UnsupportedEncodingException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test xmlns=\"default\" xmlns:prefix=\"http://uri\">"
                + "<child1 id=\"1\">default ns</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLActualEncodingDiffersFromDeclared_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>æøå</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testRootTagNameWitNamespacePrefix() throws UnsupportedEncodingException, IllegalDataException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ns:test xmlns:ns=\"http://uri\">"
                + "<child1>æøå</child1>"
                + "</ns:test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLContainsIllegalAmpersand_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a single Ampersand: & which is not legal</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLContainsIllegalLessThanSign_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Less Than sign: < which is not legal</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLContainsIllegalLargerThanSign_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Larger Than sign: > which is legal</child1>"
                + "</test>";
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Larger Than sign: &gt; which is legal</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedXml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLContainsQuotationMark_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is a Quotation Mark: \" which is legal</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLContainsApostroph_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>This is an Aprostroph: ' which is legal</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLTagStartsWithColon_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<:test>"
                + "<child1>child text</child1>"
                + "</:test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLTagStartsWithUnderscore_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_test>"
                + "<child1>child text</child1>"
                + "</_test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLTagWithLegalSpecialCharacters_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<_-.9>"
                + "<child1>child text</child1>"
                + "</_-.9>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLTagContainsWhiteSpace_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test is good>"
                + "<child1>This is a good test</child1>"
                + "</test is good>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLComments_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!-- declarations for <head> & <body> -->"
                + "<test>"
                + "<!-- comment in top level -->"
                + "<child1>child text</child1>"
                + "<!-- comment in sub level -->"
                + "</test>"
                + "<!-- trailing comment -->";
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!-- declarations for <head> & <body> -->"
                + "<test>"
                + "<!-- comment in top level -->"
                + "<child1>child text</child1>"
                + "<!-- comment in sub level -->"
                + "</test>";  // The trailing comment is removed

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedXml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLCommentsDashDash_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash -- is not legal -->"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLCommentsDashDashDashLargerThan_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash Larger Than used as a comment end is not legal: --->"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLAttributeWithQuotation_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2\">What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLAttributeWithApostrophs_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='2'>What is the size here?</child1>"
                + "</test>";
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2\">What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedXml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeWithoutQuotation_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=2>What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeWithMissingStartQuotation_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=2\">What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeWithMissingEndQuotation_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"2>What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLIllegalAttributeName_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 2size=\"2\">What is the size here?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeValueContainsAmpersand_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Ampersand: & \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeValueContainsLessThanSign_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Less than: < \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLAttributeValueContainsLargerThanSign_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Larger than: > \">What is this?</child1>"
                + "</test>";
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Larger than: &gt; \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedXml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLAttributeValueContainsQuotationMark_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: \" \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void testXMLAttributeValueContainsApostroph_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Apostroph: ' \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(xml));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void testXMLApostrophAttributeValueContainsQuotationMark_accepted() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Quotation Mark: \" '>What is this?</child1>"
                + "</test>";
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size=\"Quotation Mark: &quot; \">What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        assertThat(it.next(), is(expectedXml));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = IllegalDataException.class)
    public void testXMLApostrophAttributeValueContainsApostroph_throwsException() throws UnsupportedEncodingException, XMLStreamException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<test>"
                + "<child1 size='Apostroph: ' '>What is this?</child1>"
                + "</test>";

        DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        Iterator<String> it = xmlRecordSplitter.iterator();

        assertThat(it.hasNext(), is(true));
        it.next();
    }

    @Test
    public void getEncoding_documentSpecifiesEncodingInDeclaration_returnsEncoding() throws UnsupportedEncodingException, XMLStreamException {
        final String encoding = "ISO-8859-1";
        final String xml = String.format("<?xml version=\"1.0\" encoding=\"%s\"?><topLevel></topLevel>", encoding);
        final DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));
        assertThat(xmlRecordSplitter.getEncoding(), is(encoding));
    }

    @Test
    public void getEncoding_documentHasNoDeclaration_returnsDefaultEncoding() throws UnsupportedEncodingException, XMLStreamException {
        final String encoding = "UTF-8";
        final String xml = "<topLevel></topLevel>";
        final DefaultXMLRecordSplitter xmlRecordSplitter = new DefaultXMLRecordSplitter(new ByteArrayInputStream(xml.getBytes(UTF8_CHARSET)));
        assertThat(xmlRecordSplitter.getEncoding(), is(encoding));
    }

}