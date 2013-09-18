package dk.dbc.dataio.engine;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

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
}