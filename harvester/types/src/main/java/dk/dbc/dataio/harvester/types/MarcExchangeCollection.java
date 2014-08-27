package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class represents a MARC Exchange Collection as a harvester XML record.
 * <p>
 * This class is not thread safe.
 * </p>
 */
public class MarcExchangeCollection implements HarvesterXmlRecord {
    static final String MARC_EXCHANGE_NAMESPACE = "info:lc/xmlns/marcxchange-v1";
    static final String COLLECTION_ELEMENT_NAME = "collection";
    static final String RECORD_ELEMENT_NAME = "record";

    private final Transformer transformer;
    private final DocumentBuilder documentBuilder;
    private final Document data;
    private final Charset charset = StandardCharsets.UTF_8;
    private int memberCount = 0;

    /**
     * Class constructor
     * @throws HarvesterException if unable to create internal MARC Exchange Collection representation
     */
    //@Deprecated
    public MarcExchangeCollection() throws HarvesterException {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new HarvesterException("Unable to create transformer", e);
        }
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new HarvesterException("Unable to create document builder", e);
        }
        data = documentBuilder.newDocument();
        data.setXmlStandalone(true);
        data.appendChild(data.createElementNS(MARC_EXCHANGE_NAMESPACE, COLLECTION_ELEMENT_NAME));
    }

    /**
     * Class constructor
     * @param documentBuilder builder used for XML representations (must be created by a namespace aware factory)
     * @param transformer transformer used for conversion to byte[]
     * @throws NullPointerException if given any null-valued argument
     * @throws HarvesterException if unable to create internal MARC Exchange Collection representation
     */
    public MarcExchangeCollection(DocumentBuilder documentBuilder, Transformer transformer)
            throws NullPointerException, HarvesterException {
        this.transformer = InvariantUtil.checkNotNullOrThrow(transformer, "transformer");
        this.documentBuilder = InvariantUtil.checkNotNullOrThrow(documentBuilder, "documentBuilder");
        data = documentBuilder.newDocument();
        data.setXmlStandalone(true);
        data.appendChild(data.createElementNS(MARC_EXCHANGE_NAMESPACE, COLLECTION_ELEMENT_NAME));
    }

    /**
     * @return this MARC Exchange Collection XML representation as byte array
     * @throws HarvesterInvalidRecordException if collection contains no record members
     * @throws HarvesterException if unable to transform internal MARC Exchange Collection representation
     * to byte array
     */
    @Override
    public byte[] asBytes() throws HarvesterException {
        final Document document = asDocument();
        final Source source = new DOMSource(document.getDocumentElement());
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final Result result = new StreamResult(out);
            configureTransformer().transform(source, result);
            return out.toByteArray();
        } catch (IOException | TransformerException e) {
            throw new HarvesterException("Unable to transform data into bytes", e);
        } finally {
            transformer.reset();
        }
    }

    /**
     * @return this MARC Exchange Collection XML representation as Document
     * @throws HarvesterInvalidRecordException if collection contains no record members
     */
    @Override
    public Document asDocument() throws HarvesterInvalidRecordException {
        if (memberCount == 0) {
            throw new HarvesterInvalidRecordException("MARC exchange collection contains no record members");
        }
        return data;
    }

    /**
     * @return MARC Exchange Collection XML encoding, currently always UTF-8
     */
    @Override
    public Charset getCharset() {
        return charset;
    }

    /**
     * Extracts record from given MARC Exchange document (either as collection or
     * standalone record) and adds it to this collection
     * @param memberData  MARC Exchange document as byte array
     * @throws HarvesterInvalidRecordException If given null-valued memberData argument.
     * If given byte array can not be parsed as XML.
     * If given document specifies an encoding != UTF-8.
     * If the document element of given document is not in namespace {@value #MARC_EXCHANGE_NAMESPACE}.
     * If given document is itself a collection with more than one record.
     */
    public void addMember(byte[] memberData) throws HarvesterException {
        final Document memberDoc;
        if (memberData == null) {
            throw new HarvesterInvalidRecordException("member data can not be null");
        }
        try {
            memberDoc = documentBuilder.parse(new ByteArrayInputStream(memberData));
        } catch (SAXException | IOException e) {
            throw new HarvesterInvalidRecordException("member data can not be parsed as XML", e);
        } finally {
            documentBuilder.reset();
        }
        addMember(memberDoc);
    }

    /**
     * Extracts record from given MARC Exchange document (either as collection or
     * standalone record) and adds it to this collection
     * @param memberDoc  MARC Exchange document
     * @throws HarvesterInvalidRecordException If given null-valued memberDoc argument.
     * If given document specifies an encoding != UTF-8.
     * If the document element of given document is not in namespace {@value #MARC_EXCHANGE_NAMESPACE}.
     * If given document is itself a collection with more than one record.
     */
    public void addMember(Document memberDoc) throws HarvesterException {
        if (memberDoc == null) {
            throw new HarvesterInvalidRecordException("member data can not be null");
        }
        verifyEncoding(memberDoc);
        data.getDocumentElement().appendChild(data.importNode(extractRecordNode(memberDoc), true));
        memberCount++;
    }

    private Transformer configureTransformer() throws HarvesterException {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        return transformer;
    }

    private Node extractRecordNode(Document member) throws HarvesterInvalidRecordException {
        final Element documentElement = member.getDocumentElement();
        if (!MARC_EXCHANGE_NAMESPACE.equals(documentElement.getNamespaceURI())) {
            throw new HarvesterInvalidRecordException(String.format(
                    "member data is not in %s namespace", MARC_EXCHANGE_NAMESPACE));
        }
        if (RECORD_ELEMENT_NAME.equals(documentElement.getLocalName())) {
            return documentElement;
        }
        final NodeList records = documentElement.getElementsByTagNameNS(MARC_EXCHANGE_NAMESPACE, RECORD_ELEMENT_NAME);
        if (records.getLength() != 1) {
            throw new HarvesterInvalidRecordException(String.format(
                    "member data has %d record elements", records.getLength()));
        }
        return records.item(0);
    }

    private void verifyEncoding(Document member) throws HarvesterInvalidRecordException {
        final String xmlEncoding = member.getXmlEncoding();
        if (xmlEncoding != null) {
            final Charset memberCharset = Charset.forName(xmlEncoding);
            if (charset.compareTo(memberCharset) != 0) {
                 throw new HarvesterInvalidRecordException(String.format("charset mismatch %s != %s",
                         memberCharset.displayName(), charset.displayName()));
            }
        }
    }
}
