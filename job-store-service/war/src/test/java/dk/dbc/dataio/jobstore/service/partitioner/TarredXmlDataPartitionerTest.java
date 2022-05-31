package dk.dbc.dataio.jobstore.service.partitioner;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TarredXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test(timeout = 5000)
    public void partitioning() {
        final DataPartitioner partitioner = TarredXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-tarred.tar"), "UTF-8");
        final List<DataPartitionerResult> results = getResults(partitioner);

        assertThat("number of results", results.size(),
                is(5));
        assertThat("number of bytes read", partitioner.getBytesRead(),
                is(20480L));
    }

    @Test(timeout = 5000)
    public void positionInDatafile() {
        final DataPartitioner partitioner = TarredXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-tarred.tar"), "UTF-8");

        int expectedPosition = 0;
        for (DataPartitionerResult result : partitioner) {
            assertThat("result " + expectedPosition + " position", result.getPositionInDatafile(),
                    is(expectedPosition++));
        }
    }

    @Test(timeout = 5000)
    public void emptyTarArchive() {
        final DataPartitioner partitioner = TarredXmlDataPartitioner.newInstance(
                getResourceAsStream("empty.tar"), "UTF-8");

        assertThat("number of results", getResults(partitioner).size(),
                is(0));
    }

    @Test(timeout = 5000)
    public void extractsXmlDocuments() throws ParserConfigurationException, IOException, SAXException {
        final DataPartitioner partitioner = TarredXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-tarred.tar"), "UTF-8");

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        for (DataPartitionerResult result : partitioner) {
            documentBuilder.parse(new ByteArrayInputStream(result.getChunkItem().getData()));
        }
    }
}
