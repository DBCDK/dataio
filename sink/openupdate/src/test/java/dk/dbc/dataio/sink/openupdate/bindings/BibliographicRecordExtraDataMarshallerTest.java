package dk.dbc.dataio.sink.openupdate.bindings;

import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BibliographicRecordExtraDataMarshallerTest {
    private final BibliographicRecordExtraDataMarshaller marshaller = new BibliographicRecordExtraDataMarshaller();

    public static BibliographicRecordExtraData unmarshall(Document document) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class).createUnmarshaller();
        return unmarshaller.unmarshal(document.getDocumentElement(), BibliographicRecordExtraData.class).getValue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void toXmlDocument_dataArgIsNull_throws() throws JAXBException {
        marshaller.toXmlDocument(null);
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
