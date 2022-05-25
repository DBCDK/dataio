package dk.dbc.dataio.sink.openupdate.bindings;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.dom.DOMResult;
import java.nio.charset.StandardCharsets;

/**
 * Handles marshalling of dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData
 */
public class BibliographicRecordExtraDataMarshaller {
    private final Marshaller marshaller;

    public BibliographicRecordExtraDataMarshaller() {
        try {
            marshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.toString());
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, BibliographicRecordExtraData.NAMESPACE);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a BibliographicRecordExtraData instance to W3C Document object
     *
     * @param data BibliographicRecordExtraData to convert
     * @return Document representation
     * @throws IllegalArgumentException if given null-valued argument
     * @throws JAXBException            If any unexpected problem occurs during the marshalling.
     */
    public Document toXmlDocument(BibliographicRecordExtraData data) throws IllegalArgumentException, JAXBException {
        final DOMResult domResult = new DOMResult();
        marshaller.marshal(data, domResult);
        return (Document) domResult.getNode();
    }
}
