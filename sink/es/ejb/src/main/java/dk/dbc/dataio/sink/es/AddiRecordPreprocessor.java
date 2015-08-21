package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddiRecordPreprocessor {
    static final String PROCESSING_TAG = "dataio:sink-processing";

    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    public AddiRecordPreprocessor() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            transformer = transformerFactory.newTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method pre-processes an addi record according to the following rules:
     *
     * If the processing tag(dataio:sink-processing) is not found within the meta data,
     * the Addi record is returned unchanged
     *
     * If processing tag is found with value FALSE, the tag is removed from the meta data.
     *
     * If processing tag is found with value TRUE, the tag is removed from the meta data
     * and the content data is converted to iso2709.
     * @param addiRecord Addi record tp pre-process
     * @return the pre-processed Addi record
     * @throws IllegalArgumentException on invalid metadata or content
     */
    public AddiRecord execute(AddiRecord addiRecord) throws IllegalArgumentException {
        try {
            final Document metaDataDocument = getDocument(addiRecord.getMetaData());
            final NodeList nodeList = metaDataDocument.getElementsByTagName(PROCESSING_TAG);
            if (nodeList.getLength() > 0) { // The specific tag has been located
                byte[] content = addiRecord.getContentData();
                final Node processingNode = nodeList.item(0);
                if (do2709Encode(processingNode)) {
                    final Document contentDataDocument = getDocument(addiRecord.getContentData());
                    content = Iso2709Packer.create2709FromMarcXChangeRecord(contentDataDocument, new DanMarc2Charset());
                }
                removeFromDom(nodeList);
                return new AddiRecord(domToByteArray(metaDataDocument), content);
            }
            return addiRecord;
        } catch (IOException | SAXException | TransformerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* This method removes all nodes in given nodelist from the dom */
    private void removeFromDom(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            node.getParentNode().removeChild(node); // Remove node from dom
        }
    }

    /* This method converts a document to a byte array */
    private byte[] domToByteArray(Document document) throws TransformerException {
        Source source = new DOMSource(document);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Result result = new StreamResult(byteArrayOutputStream);
        transformer.reset();
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }

    /* This method determines if iso2709 encoding should be performed depending on the value
     * of the 'encodeAs2709' attribute of the given node
     * @return true if iso2709 encoding should be performed, otherwise false
     */
    private boolean do2709Encode(Node node) {
        return Boolean.valueOf(node.getAttributes().getNamedItem("encodeAs2709").getNodeValue());
    }

    private Document getDocument(byte[] byteArray) throws IOException, SAXException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        documentBuilder.reset();
        return documentBuilder.parse(byteArrayInputStream);
    }
}
