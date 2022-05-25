package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class DefaultXmlDataPartitionerTest extends AbstractPartitionerTestBase {
    private final static String XML_HEADER = "<?xml version='1.0'?>";

    @Test
    public void emptyRootElement_returnsNoXMLStrings() {
        final String xml = XML_HEADER + "<topLevel></topLevel>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.UTF_8.name());

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void emptyCollapsedRootElement_returnsNoXMLStrings() {
        final String xml = XML_HEADER + "<topLevel/>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        assertThat(dataPartitioner.iterator().hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void next_dataPartitionerResultContainsExpectedChunkItemAndNullValuedRecordInfo() {
        final String xml = XML_HEADER
                + "<topLevel>"
                +   "<collection xmlns=\"info:lc/xmlns/marcxchange-v1\">"
                +     "<record>"
                +       "<marcx:datafield xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" tag=\"001\">"
                +         "<marcx:subfield code=\"a\">123456</marcx:subfield>"
                +       "</marcx:datafield>"
                +     "</record>"
                +   "</collection>"
                + "</topLevel>";
        final ChunkItem expectedResult = new ChunkItemBuilder().setData(xml).build();
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat(dataPartitionerResult.getChunkItem(), is(expectedResult));
        assertThat(dataPartitionerResult.getRecordInfo(), is(nullValue()));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void singleXMLChild_givesOneStringWithXML() {
        final String xml = XML_HEADER + "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final ChunkItem expectedResult = new ChunkItemBuilder().setData(xml).build();
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedResult));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void moreThanOneXMLChild_givesTheSameAmountOfStringsWithXML() {
        final String xml = XML_HEADER + "<topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>";
        final ChunkItem expectedResult1 = new ChunkItemBuilder().setData(XML_HEADER +
                "<topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>").build();
        final ChunkItem expectedResult2 = new ChunkItemBuilder().setData(XML_HEADER +
                "<topLevel>"
                + "<child><grandChild>Pirate so brave on the seven seas</grandChild></child>"
                + "</topLevel>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat("has 1st", iterator.hasNext(), is(true));
        DataPartitionerResult result = iterator.next();
        assertThat("1st chunk item", result.getChunkItem(), is(expectedResult1));
        assertThat("1st position in datafile", result.getPositionInDatafile(), is(0));
        assertThat("has 2nd", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("2nd chunk item", result.getChunkItem(), is(expectedResult2));
        assertThat("2nd position in datafile", result.getPositionInDatafile(), is(1));
        assertThat("has 3rd", iterator.hasNext(), is(false));
        assertThat("bytes read", dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void missingXMLHeaderInInput_xmlHeaderIsInsertedInResult() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>";
        final ChunkItem expectedResult = new ChunkItemBuilder()
                .setData(XML_HEADER
                        + "<topLevel>"
                        + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child></topLevel>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedResult));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void erroneousXMLContainingOnlyRootStartElement_throws() {
        final String xml = XML_HEADER + "<topLevel>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void erroneousXMLContainingUnfinishedFirstChild_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</grand";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void erroneousXMLWrongNesting_throws() {
        final String xml = "<topLevel><child><grandChild>This is the tale of Captain Jack Sparrow</child></grandChild></topLevel>";
        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);

        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void erroneousXMLContainingUnfinishedSecondChild_throws() {
        final String xml = XML_HEADER + "<topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "<child><grandChild>Pirate so brave on the seven seas</grand";
        final ChunkItem expectedResult = new ChunkItemBuilder().setData(XML_HEADER + "<topLevel>"
                + "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>"
                + "</topLevel>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedResult));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlWithDefaultNamespace() {
        final String xml = XML_HEADER
                + "<test xmlns=\"default\" xmlns:prefix=\"http://uri\">"
                + "<child1 id=\"1\">default ns</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();

        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlActualEncodingDiffersFromDeclared_throws() {
        final String xml =
                  "<?xml version='1.0' encoding='UTF-8'?>"
                + "<test>"
                + "<child1>æøå</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml, StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8.name());
        try {
            dataPartitioner.iterator();
            fail("No PrematureEndOfDataException thrown");
        } catch (PrematureEndOfDataException e) {}
    }

    @Test
    public void rootTagNameWitNamespacePrefix() {
        final String xml = XML_HEADER
                + "<ns:test xmlns:ns=\"http://uri\">"
                + "<child1>æøå</child1>"
                + "</ns:test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsIllegalAmpersand_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>This is a single Ampersand: & which is not legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlContainsIllegalLessThanSign_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>This is a Less Than sign: < which is not legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlContainsIllegalLargerThanSign_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>This is a Larger Than sign: > which is legal</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData(xml)
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsQuotationMark_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>This is a Quotation Mark: \" which is legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlContainsApostroph_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>This is an Aprostroph: ' which is legal</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagStartsWithUnderscore_accepted() {
        final String xml = XML_HEADER
                + "<_test>"
                + "<child1>child text</child1>"
                + "</_test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expceted = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expceted));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagWithLegalSpecialCharacters_accepted() {
        final String xml = XML_HEADER
                + "<_-.9>"
                + "<child1>child text</child1>"
                + "</_-.9>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlTagContainsWhiteSpace_throws() {
        final String xml = XML_HEADER
                + "<test is good>"
                + "<child1>This is a good test</child1>"
                + "</test is good>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlComments_accepted() {
        final String xml = XML_HEADER
                + "<!-- declarations for <head> & <body> -->"
                + "<test>"
                + "<!-- comment in top level -->"
                + "<child1>child text</child1>"
                + "<!-- comment in sub level -->"
                + "</test>"
                + "<!-- trailing comment -->";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData(XML_HEADER
                        + "<!-- declarations for <head> & <body> -->"
                        + "<test>"
                        + "<!-- comment in top level -->"
                        + "<child1>child text</child1>"
                        + "<!-- comment in sub level -->"
                        + "</test>")  // The trailing comment is removed
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlCommentsDashDash_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash -- is not legal -->"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlCommentsDashDashDashLargerThan_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>child text</child1>"
                + "<!-- Dash Dash Larger Than used as a comment end is not legal: --->"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator::next, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeWithQuotation_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeWithApostrophs_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size='2'>What is the size here?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData(XML_HEADER
                        + "<test>"
                        + "<child1 size=\"2\">What is the size here?</child1>"
                        + "</test>")
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeWithoutQuotation_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=2>What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeWithMissingStartQuotation_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeWithMissingEndQuotation_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"2>What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlIllegalAttributeName_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 2size=\"2\">What is the size here?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeValueContainsAmpersand_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"Ampersand: & \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeValueContainsLessThanSign_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"Less than: < \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeValueContainsLargerThanSign_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"Larger than: > \">What is this?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder()
                .setData(xml)
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlAttributeValueContainsQuotationMark_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"Quotation Mark: \" \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void xmlAttributeValueContainsApostroph_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size=\"Apostroph: ' \">What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final ChunkItem expected = new ChunkItemBuilder().setData(xml).build();
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expected));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlApostrophAttributeValueContainsQuotationMark_accepted() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size='Quotation Mark: \" '>What is this?</child1>"
                + "</test>";
        final ChunkItem expectedXml = new ChunkItemBuilder().setData(XML_HEADER
                + "<test>"
                + "<child1 size=\"Quotation Mark: &quot; \">What is this?</child1>"
                + "</test>").build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next().getChunkItem(), is(expectedXml));
        assertThat(iterator.hasNext(), is(false));
        assertThat(dataPartitioner.getBytesRead(), is((long) xml.getBytes(StandardCharsets.UTF_8).length));
    }

    @Test
    public void xmlApostrophAttributeValueContainsApostroph_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1 size='Apostroph: ' '>What is this?</child1>"
                + "</test>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(xml);
        assertThat(dataPartitioner::iterator, isThrowing(InvalidDataException.class));
    }

    @Test
    public void expectedEncodingDiffersFromActualEncoding_throws() {
        final String xml = XML_HEADER
                + "<test>"
                + "<child1>data</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.ISO_8859_1.name());
        assertThat(dataPartitioner::iterator, isThrowing(InvalidEncodingException.class));
    }

    @Test
    public void expectedEncodingDiffersFromDefaultEncoding_throws() {
        final String xml = "<?xml version=\"1.0\"?>"
                + "<test>"
                + "<child1>data</child1>"
                + "</test>";
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.ISO_8859_1.name());
        assertThat(dataPartitioner::iterator, isThrowing(InvalidEncodingException.class));
    }

    @Test
    public void getEncoding_returnsUTF8() {
        final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(getEmptyInputStream(), "latin1");
        assertThat(dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void newInstance_illegalCharsetNameException_throws() {
        assertThat(() -> DefaultXmlDataPartitioner.newInstance(asInputStream("<test/>"), "[ILLEGAL_CHARSET_NAME]"),
                isThrowing(InvalidEncodingException.class));
    }

    @Test
    public void iterator_next_returnsChunkItemsWithoutTrackingId() {
        final DataPartitioner dataPartitioner = newPartitionerInstance(getDataContainerXmlWithMarcExchangeAndTrackingIds());
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem0 = iterator.next().getChunkItem();
        assertThat("chunkItem0.trackingId", chunkItem0.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem1 = iterator.next().getChunkItem();
        assertThat("chunkItem1.trackingId", chunkItem1.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem chunkItem2 = iterator.next().getChunkItem();
        assertThat("chunkItem1.trackingId", chunkItem2.getTrackingId(), is(nullValue()));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void ioExceptionWhileReadingInputStream_throws() {
        final InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Connection lost");
            }
        };

        final DefaultXmlDataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(is, StandardCharsets.UTF_8.name());
        assertThat(dataPartitioner::iterator, isThrowing(PrematureEndOfDataException.class));
    }

    @Test
    public void convertsDocumentsWithNonUtf8Encoding() {
        final DefaultXmlDataPartitioner partitioner = DefaultXmlDataPartitioner
                .newInstance(getResourceAsStream("iso8859-1.xml"), "latin1");

        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("has 1st result", iterator.hasNext(), is(true));
        final DataPartitionerResult next = iterator.next();
        assertThat("content of 1st result chunk item", new String(next.getChunkItem().getData(), StandardCharsets.UTF_8),
                is(XML_HEADER + "<records><record>æÆ</record></records>"));
    }

    private DataPartitioner newPartitionerInstance(String xml) {
        return DefaultXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.UTF_8.name());
    }
}
