package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class exposes specialized field value bindings from MarcXchange documents
 * <br/>
 * * Extracts identifier from 001a (must be non-empty)
 * <br/>
 * * Extracts library from 001b (must be non-empty numeric)
 */
public class MarcExchangeRecordBinding {
    public static final String DATAFIELD_ELEMENT_NAME = "datafield";
    public static final String DATAFIELD_TAG_ATTRIBUTE_NAME = "tag";
    public static final String SUBFIELD_ELEMENT_NAME = "subfield";
    public static final String SUBFIELD_CODE_ATTRIBUTE_NAME = "code";
    private String namespace;
    private String id = null;
    private int library = 0;

    /**
     * Class constructor
     * @param document MARCXchange document
     * @throws NullPointerException if given null valued argument
     * @throws IllegalArgumentException if unable to extract required values from given document
     */
    public MarcExchangeRecordBinding(final Document document) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(document, "document");
        final Element documentElement = document.getDocumentElement();
        namespace = documentElement.getNamespaceURI();
        extract(documentElement);
    }

    public String getId() {
        return id;
    }

    public int getLibrary() {
        return library;
    }

    private void extract(Element documentElement) throws IllegalArgumentException {
        final NodeList datafields = documentElement.getElementsByTagNameNS(namespace, DATAFIELD_ELEMENT_NAME);
        for (int i = 0; i < datafields.getLength(); i++) {
            final Element datafield = (Element) datafields.item(i);
            final String tag = datafield.getAttribute(DATAFIELD_TAG_ATTRIBUTE_NAME);
            switch (tag) {
                case "001": extractFrom001(datafield);
                    break;
                default:
            }
        }
        verifyExtraction();
    }

    private void extractFrom001(Element datafield) {
        final NodeList subfields = datafield.getElementsByTagNameNS(namespace, SUBFIELD_ELEMENT_NAME);
        for (int i = 0; i < subfields.getLength(); i++) {
            final Element subfield = (Element) subfields.item(i);
            final String code = subfield.getAttribute(SUBFIELD_CODE_ATTRIBUTE_NAME);
            switch (code) {
                case "a": id = subfield.getTextContent();
                    break;
                case "b": library = Integer.parseInt(subfield.getTextContent());
                    break;
                default:
            }
        }
    }

    private void verifyExtraction() throws IllegalArgumentException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("invalid identifier (001*a)");
        }
        if (library == 0) {
            throw new IllegalArgumentException("invalid library (001*b)");
        }
    }
}
