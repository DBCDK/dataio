package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.harvester.types.MarcExchangeRecordBinding;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Verifier helper class for MARC exchange collection expectations
 */
public class MarcExchangeCollectionExpectation implements DataFileExpectation {
    static final String MARC_EXCHANGE_NAMESPACE = "info:lc/xmlns/marcxchange-v1";
    static final String COLLECTION_ELEMENT_NAME = "collection";
    static final String RECORD_ELEMENT_NAME = "record";

    public Set<MarcExchangeRecord> records;

    private final DomUtil domUtil;

    public MarcExchangeCollectionExpectation() {
        this.records = new HashSet<>();
        try {
            domUtil = new DomUtil();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Verifies given node as MARC exchange collection containing record
     * members specified by given list of record expectations throwing
     * assertion error unless all expectations can be met
     * @param node MARC exchange collection node
     */
    @Override
    public void verify(Node node) {
        assertThat(node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(node.getLocalName(), is(COLLECTION_ELEMENT_NAME));
        assertThat(node.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));

        verifyMarcExchangeCollectionRecords(node.getChildNodes());
    }

    /* Verifies all record members
     */
    private void verifyMarcExchangeCollectionRecords(NodeList recordNodes) {
        final Set<MarcExchangeRecord> actualRecords = new HashSet<>();
        assertThat(recordNodes, is(notNullValue()));
        assertThat(recordNodes.getLength(), is(records.size()));
        for (int i = 0; i < recordNodes.getLength(); i++) {
            final Node recordNode = recordNodes.item(i);
            assertThat(recordNode.getNodeType(), is(Node.ELEMENT_NODE));
            assertThat(recordNode.getLocalName(), is(RECORD_ELEMENT_NAME));
            assertThat(recordNode.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));
            final Document recordDocument = domUtil.asDocument((Element) recordNode);
            final MarcExchangeRecordBinding marcExchangeRecordBinding = new MarcExchangeRecordBinding(recordDocument);
            actualRecords.add(new MarcExchangeRecord(
                    marcExchangeRecordBinding.getId(), marcExchangeRecordBinding.getLibrary()));
        }
        for (MarcExchangeRecord expectation : records) {
            assertThat(expectation.toString(), actualRecords.remove(expectation), is(true));
        }
        assertThat("All records accounted for", actualRecords.isEmpty(), is(true));
    }
}
