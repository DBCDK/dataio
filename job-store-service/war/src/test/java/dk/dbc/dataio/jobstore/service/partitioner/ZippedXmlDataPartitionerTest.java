package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.marc.reader.MarcReaderException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

public class ZippedXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test(timeout = 5000)
    public void partitioning() {

        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        /*final List<DataPartitionerResult> ebscoRecords = new ArrayList<>(5);
        int numberOfIterations = 0;
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                ebscoRecords.add(result);
            }
            numberOfIterations++;
        }*/

        assertThat("Number of iterations", results.size(), is(5));
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
            The selected test chunk contains this xml fragment..

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

    // Check that we get all 5 chunks, in the right order
    @Test(timeout = 5000)
    public void sequence() {
        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        /*
        The chunks contains this xml fragment..

        <epdate...>
          <record...>
            <item name="AN">20482152</item>
            ...
           </record>
        </epdata>

        */

        // Expected values (and sequence from the <item name="AN"/> elements
        String[] anValues = {"20482152", "20482155", "20482161", "3427932", "3651706"};


        // Load each chunk and verify the id number
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            for(int chunkNo = 0; chunkNo < 5; chunkNo++) {
                ByteArrayInputStream chunkStream = getRecordStream(results.get(chunkNo).getChunkItem());


                Document document = documentBuilder.parse(chunkStream);


                XPath xPath = XPathFactory.newInstance().newXPath();
                String expression = "/epdata/record/item[@name=\"AN\"]";
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
                assertThat("Has one <item> element with name='AN'", nodeList.getLength(), is(1));
                assertThat("Is correct id", nodeList.item(0).getTextContent(), is(anValues[chunkNo]));
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

    private List<DataPartitionerResult> getResults(DataPartitioner partitioner) {
        final List<DataPartitionerResult> ebscoRecords = new ArrayList<>(5);
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                ebscoRecords.add(result);
            }
        }
        return ebscoRecords;
    }

    private ByteArrayInputStream getRecordStream(ChunkItem chunkItem) {
        return new ByteArrayInputStream(chunkItem.getData());
    }
}
