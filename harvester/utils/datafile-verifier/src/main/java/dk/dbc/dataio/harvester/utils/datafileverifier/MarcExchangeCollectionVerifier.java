package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.harvester.types.MarcExchangeRecordBinding;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Class used for verification of MARC exchange collection
 */
public class MarcExchangeCollectionVerifier {
    private static final String MARC_EXCHANGE_NAMESPACE = "info:lc/xmlns/marcxchange-v1";
    private static final String COLLECTION_ELEMENT_NAME = "collection";
    private static final String RECORD_ELEMENT_NAME = "record";

    private final DomUtil domUtil;

    /**
     * Class constructor
     * @param domUtil XML conversion util
     */
    public MarcExchangeCollectionVerifier(final DomUtil domUtil) {
        this.domUtil = domUtil;
    }

    /**
     * Verifies given node as MARC exchange collection containing record
     * members specified by given list of record expectations throwing
     * assertion error unless all expectations can be met
     * @param node MARC exchange collection node
     * @param recordIds list of record member expectations
     */
    public void verify(Node node, List<MarcExchangeRecordExpectation> recordIds) {
        assertThat(node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(node.getLocalName(), is(COLLECTION_ELEMENT_NAME));
        assertThat(node.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));

        verifyMarcExchangeCollectionRecords(node.getChildNodes(), recordIds);
    }

    /* Verifies all record members
     */
    private void verifyMarcExchangeCollectionRecords(NodeList recordNodes, List<MarcExchangeRecordExpectation> recordIds) {
        assertThat(recordNodes, is(notNullValue()));
        assertThat(recordNodes.getLength(), is(recordIds.size()));
        for (int i = 0; i < recordNodes.getLength(); i++) {
            final Node recordNode = recordNodes.item(i);
            assertThat(recordNode.getNodeType(), is(Node.ELEMENT_NODE));
            assertThat(recordNode.getLocalName(), is(RECORD_ELEMENT_NAME));
            assertThat(recordNode.getNamespaceURI(), is(MARC_EXCHANGE_NAMESPACE));
            final Document recordDocument = domUtil.asDocument((Element) recordNode);
            final MarcExchangeRecordBinding marcExchangeRecordBinding = new MarcExchangeRecordBinding(recordDocument);
            assertThat(marcExchangeRecordBinding.getId(), is(recordIds.get(i).getId()));
            assertThat(marcExchangeRecordBinding.getLibrary(), is(recordIds.get(i).getNumber()));
        }
    }
}
