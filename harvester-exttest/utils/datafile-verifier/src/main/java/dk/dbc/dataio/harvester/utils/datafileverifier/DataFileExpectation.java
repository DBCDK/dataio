package dk.dbc.dataio.harvester.utils.datafileverifier;

import org.w3c.dom.Node;

public interface DataFileExpectation {
    void verify(Node node);
}
