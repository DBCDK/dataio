package dk.dbc.dataio.sink.openupdate.bindings;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BibliographicRecordExtraDataMarshallerTest {
    private final BibliographicRecordExtraDataMarshaller marshaller = new BibliographicRecordExtraDataMarshaller();

    public static BibliographicRecordExtraData unmarshall(Document document) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class).createUnmarshaller();
        return unmarshaller.unmarshal(document.getDocumentElement(), BibliographicRecordExtraData.class).getValue();
    }

    @Test
    public void toXmlDocument_dataArgIsNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> marshaller.toXmlDocument(null));
    }

    @Test
    public void toXmlDocument() throws JAXBException {
        BibliographicRecordExtraData bibliographicRecordExtraData = new BibliographicRecordExtraData();
        bibliographicRecordExtraData.setProviderName("myProvider");
        bibliographicRecordExtraData.setPriority(1000);

        BibliographicRecordExtraData unmarshalled = unmarshall(marshaller.toXmlDocument(bibliographicRecordExtraData));
        assertThat("providerName", unmarshalled.getProviderName(),
                is(bibliographicRecordExtraData.getProviderName()));
        assertThat("priority", unmarshalled.getPriority(),
                is(1000));
    }
}
