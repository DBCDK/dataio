package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Node;

public abstract class XmlExpectation extends Expectation {
    abstract void verify(Node node);
}
