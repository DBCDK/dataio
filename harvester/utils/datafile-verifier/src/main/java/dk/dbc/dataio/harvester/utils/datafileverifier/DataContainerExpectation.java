package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Verifier helper class for data-container expectations
 */
public class DataContainerExpectation implements DataFileExpectation {
    private static final String DATA_CONTAINER_ELEMENT_NAME = "data-container";
    private static final String DATA_SUPPLEMENTARY_ELEMENT_NAME = "data-supplementary";
    private static final String DATA_ELEMENT_NAME = "data";

    public DataFileExpectation dataExpectation;
    public Map<String, String> supplementaryDataExpectation;

    public DataContainerExpectation() {
        supplementaryDataExpectation = new HashMap<>();
        dataExpectation = null;
    }

    /**
     * Verifies given node as data container containing data-supplementary
     * and data elements throwing assertion error unless all expectations can be met
     * @param node data container node
     */
    @Override
    public void verify(Node node) {
        assertThat(node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(node.getLocalName(), is(DATA_CONTAINER_ELEMENT_NAME));
        final NodeList childNodes = node.getChildNodes();
        assertThat(childNodes.getLength(), is(2));
        verifyDataSupplementary(childNodes.item(0));
        verifyData(childNodes.item(1));
    }

    private void verifyData(Node node) {
        assertThat(node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(node.getLocalName(), is(DATA_ELEMENT_NAME));
        assertThat(dataExpectation, is(notNullValue()));
        if (dataExpectation != null) {
            dataExpectation.verify(node.getFirstChild());
        }
    }

    private void verifyDataSupplementary(Node node) {
        assertThat(node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(node.getLocalName(), is(DATA_SUPPLEMENTARY_ELEMENT_NAME));
        final HashMap<String, String> expectations = new HashMap<>(supplementaryDataExpectation);
        final NodeList childNodes = node.getChildNodes();
        assertThat(childNodes.getLength(), is(expectations.size()));
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node valueNode = childNodes.item(i);
            assertThat(valueNode.getNodeType(), is(Node.ELEMENT_NODE));
            final String valueName = valueNode.getLocalName();
            assertThat("/data-container/data-supplementary/" + valueName + " exists", expectations.containsKey(valueName), is(true));
            assertThat("/data-container/data-supplementary/" + valueName + "/textContent",
                    expectations.get(valueName), is(valueNode.getTextContent()));
            expectations.remove(valueName);
        }
        assertThat("All supplementary data expectations accounted for", expectations.size(), is(0));
    }
}
