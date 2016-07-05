/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Verifier helper class for data-container expectations
 */
public class DataContainerExpectation implements XmlExpectation {
    private static final String HARVESTER_DATAFILE_ELEMENT_NAME = "dataio-harvester-datafile";
    private static final String DATA_CONTAINER_ELEMENT_NAME = "data-container";
    private static final String DATA_SUPPLEMENTARY_ELEMENT_NAME = "data-supplementary";
    private static final String DATA_ELEMENT_NAME = "data";

    public XmlExpectation dataExpectation;
    public Map<String, String> supplementaryDataExpectation;

    public DataContainerExpectation() {
        supplementaryDataExpectation = new HashMap<>();
        dataExpectation = node -> fail("No data expectation set");
    }

    /**
     * Verifies given node as data container containing data-supplementary
     * and data elements throwing assertion error unless all expectations can be met
     * @param node data container node
     */
    @Override
    public void verify(Node node) {
        assertThat("wrapper node is element", node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat("wrapper node name", node.getLocalName(), is(HARVESTER_DATAFILE_ELEMENT_NAME));
        final NodeList dataContainerElements = ((Element) node).getElementsByTagName(DATA_CONTAINER_ELEMENT_NAME);
        assertThat("has one container element", dataContainerElements.getLength(), is(1));
        final NodeList childNodes = dataContainerElements.item(0).getChildNodes();
        assertThat("container element has two children", childNodes.getLength(), is(2));
        verifyDataSupplementary(childNodes.item(0));
        verifyData(childNodes.item(1));
    }

    private void verifyData(Node node) {
        assertThat("data node is element", node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat("data node name", node.getLocalName(), is(DATA_ELEMENT_NAME));
        assertThat(dataExpectation, is(notNullValue()));
        dataExpectation.verify(node.getFirstChild());
    }

    private void verifyDataSupplementary(Node node) {
        assertThat("supplementary node is element", node.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat("supplementary node name", node.getLocalName(), is(DATA_SUPPLEMENTARY_ELEMENT_NAME));
        final HashMap<String, String> expectations = new HashMap<>(supplementaryDataExpectation);
        final NodeList childNodes = node.getChildNodes();
        assertThat("number of supplementary node children", childNodes.getLength(), is(expectations.size()));
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node valueNode = childNodes.item(i);
            assertThat("supplementary child node is element", valueNode.getNodeType(), is(Node.ELEMENT_NODE));
            final String valueName = valueNode.getLocalName();
            assertThat("data-container/data-supplementary/" + valueName + " exists", expectations.containsKey(valueName), is(true));
            assertThat("data-container/data-supplementary/" + valueName + "/textContent",
                    expectations.get(valueName), is(valueNode.getTextContent()));
            expectations.remove(valueName);
        }
        assertThat("All supplementary data expectations accounted for", expectations.size(), is(0));
    }
}
