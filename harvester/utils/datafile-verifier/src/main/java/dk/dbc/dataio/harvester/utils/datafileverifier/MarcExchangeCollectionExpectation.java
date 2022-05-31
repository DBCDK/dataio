package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifier helper class for MARC exchange collection expectations
 */
public class MarcExchangeCollectionExpectation extends XmlExpectation {
    static final String MARC_EXCHANGE_NAMESPACE = "info:lc/xmlns/marcxchange-v1";
    static final String COLLECTION_ELEMENT_NAME = "collection";
    static final String RECORD_ELEMENT_NAME = "record";

    public Set<MarcExchangeRecordExpectation> records;

    public MarcExchangeCollectionExpectation() {
        records = new HashSet<>();
    }

    /**
     * Verifies given node as MARC exchange collection containing record
     * members specified by given list of record expectations throwing
     * assertion error unless all expectations can be met
     *
     * @param node MARC exchange collection node
     */
    @Override
    public void verify(Node node) {
        assertThat("collection node is element", node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat("collection node name", node.getLocalName(), is(COLLECTION_ELEMENT_NAME));
        assertThat("collection node namespace", node.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));

        verifyMarcExchangeCollectionRecords(node.getChildNodes());
    }

    /* Verifies all record members
     */
    private void verifyMarcExchangeCollectionRecords(NodeList recordNodes) {
        final Set<MarcExchangeRecordExpectation> actualRecords = new HashSet<>();
        assertThat("record nodes", recordNodes, is(notNullValue()));
        assertThat("number of record nodes", recordNodes.getLength(), is(records.size()));
        for (int i = 0; i < recordNodes.getLength(); i++) {
            final Node recordNode = recordNodes.item(i);
            assertThat("record node is element", recordNode.getNodeType(), is(Node.ELEMENT_NODE));
            assertThat("record node name", recordNode.getLocalName(), is(RECORD_ELEMENT_NAME));
            assertThat("record node namespace", recordNode.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));
            actualRecords.add(new MarcExchangeRecordExpectation(toMarcRecord(recordNode)));
        }
        for (MarcExchangeRecordExpectation expectation : records) {
            assertThat(expectation.toString(), actualRecords.remove(expectation), is(true));
        }
        assertThat("All records accounted for", actualRecords.isEmpty(), is(true));
    }

    private MarcRecord toMarcRecord(Node recordNode) {
        try {
            final byte[] bytes = JaxpUtil.getBytes(JaxpUtil.toDocument((Element) recordNode));
            final MarcXchangeV1Reader reader = new MarcXchangeV1Reader(
                    new BufferedInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8);
            return reader.read();
        } catch (TransformerException | MarcReaderException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
