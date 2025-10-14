package dk.dbc.dataio.harvester.utils.datafileverifier;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;

public abstract class XmlExpectation extends Expectation {
    abstract void verify(Node node);

    @Override
    public void verify(byte[] data) {
        try {
            Document document = JaxpUtil.toDocument(data);
            verify(document.getDocumentElement());
        } catch (IOException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }
}
