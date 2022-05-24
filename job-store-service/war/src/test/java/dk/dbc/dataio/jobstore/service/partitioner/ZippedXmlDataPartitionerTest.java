package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ZippedXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test(timeout = 5000)
    public void partitioning() {

        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        assertThat("Number of iterations", results.size(), is(5));
        assertThat("Number of bytes read", partitioner.getBytesRead(),
                is(DataPartitioner.NO_BYTE_COUNT_AVAILABLE));
    }

    // Check that the xml document gets copied with all childnode subtrees
    @Test(timeout = 5000)
    public void childNodeSubtree() {
        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);
        ByteArrayInputStream chunkStream = getRecordStream(results.get(0).getChunkItem());

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(chunkStream);

            /*
            The xml doc. contains this xml fragment..

            <epdate...>
              <record...>
                <item...>
                ...
                <item name="CodeNAICDesc">
                  <subitem name="NAICCode">611310</subitem>
                </item>
                ...
               </record>
            </epdata>

            */

            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/epdata/record/item/subitem";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
            assertThat("Has subnodes", nodeList.getLength(), is(11));

            expression = "/epdata/record/item/subitem[@name=\"NAICCode\"]";
            nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
            assertThat("Has subitem with name=NAICCode", nodeList.getLength(), is(1));
            assertThat("Subitem has text value '611310'", nodeList.item(0).getTextContent(),is("611310"));
        }
        catch(ParserConfigurationException pce) {
            fail("Caught ParserConfigurationException: " + pce.getMessage());
        }
        catch(SAXException se) {
            fail("Caught SAXException: " + se.getMessage());
        }
        catch(IOException ioe) {
            fail("Caught IOException: " + ioe.getMessage());
        }
        catch(XPathExpressionException xpe) {
            fail("Caught XPathExpressionException: " + xpe.getMessage());
        }
    }

    // Check that we get all 5 items, in the right order
    @Test(timeout = 5000)
    public void sequence() {
        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        /*
        The xml doc. contains this xml fragment..

        <epdate...>
          <record...>
            <item name="AN">20482152</item>
            ...
           </record>
        </epdata>

        */

        // Expected values (and sequence from the <item name="AN"/> elements
        String[] anValues = {"20482152", "20482155", "20482161", "3427932", "3651706"};

        // Load each item and verify the id number
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            for(int itemNo = 0; itemNo < 5; itemNo++) {
                ByteArrayInputStream itemsStream = getRecordStream(results.get(itemNo).getChunkItem());
                Document document = documentBuilder.parse(itemsStream);

                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = "/epdata/record/item[@name=\"AN\"]";
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
                assertThat("Has one <item> element with name='AN'", nodeList.getLength(), is(1));
                assertThat("Is correct id", nodeList.item(0).getTextContent(), is(anValues[itemNo]));
            }
        }
        catch(ParserConfigurationException pce){
            fail("Caught ParserConfigurationException: " + pce.getMessage());
        } catch(SAXException se){
            fail("Caught SAXException: " + se.getMessage());
        } catch(IOException ioe){
            fail("Caught IOException: " + ioe.getMessage());
        } catch(XPathExpressionException xpe){
            fail("Caught XPathExpressionException: " + xpe.getMessage());
        }
    }

    @Test(timeout = 5000)
    public void partitioningWithEmpty() {

        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-empty-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        assertThat("Number of iterations", results.size(), is(5));
    }

    // Check that the items is numbered correctly
    @Test(timeout = 5000)
    public void positionInDatafile() {
        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        for(int itemNo = 0; itemNo < results.size(); itemNo++) {
            DataPartitionerResult item = results.get(itemNo);
            assertThat("Item has expected id", item.getPositionInDatafile(), is(itemNo));
        }
    }

    // Check that the items is numbered correctly when having empty items
    @Test(timeout = 5000)
    public void positionInDatafileWithEmpty() {
        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-empty-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        for(int itemNo = 0; itemNo < results.size(); itemNo++) {
            DataPartitionerResult item = results.get(itemNo);
            assertThat("Item has expected id", item.getPositionInDatafile(), is(itemNo));
        }
    }

    private ByteArrayInputStream getRecordStream(ChunkItem chunkItem) {
        return new ByteArrayInputStream(chunkItem.getData());
    }
}
