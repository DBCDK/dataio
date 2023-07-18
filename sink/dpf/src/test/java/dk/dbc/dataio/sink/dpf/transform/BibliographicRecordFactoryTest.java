package dk.dbc.dataio.sink.dpf.transform;

import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xmlunit.matchers.CompareMatcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BibliographicRecordFactoryTest {
    private final BibliographicRecordFactory bibliographicRecordFactory = new BibliographicRecordFactory();

    private static BibliographicRecordExtraData unmarshallBibliographicRecordExtraData(Element element) {
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(BibliographicRecordExtraData.class)
                    .createUnmarshaller();
            return unmarshaller.unmarshal(element, BibliographicRecordExtraData.class).getValue();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void toBibliographicalRecord() throws MarcReaderException, BibliographicRecordFactoryException, TransformerException {
        final String marcXchange =
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                        "<leader>00000     22000000 4500 </leader>" +
                        "<datafield ind1='0' ind2='0' tag='001'>" +
                        "<subfield code='a'>123456</subfield>" +
                        "<subfield code='b'>870970</subfield>" +
                        "</datafield>" +
                        "</record>";

        MarcRecord marcRecord = MarcRecordFactory.fromMarcXchange(marcXchange.getBytes(StandardCharsets.UTF_8));

        BibliographicRecord bibliographicRecord = bibliographicRecordFactory.toBibliographicRecord(marcRecord, "myQueueProvider");
        assertThat("Bibliographic record not null", bibliographicRecord, is(notNullValue()));
        assertThat("Bibliographic record recordPackaging", bibliographicRecord.getRecordPacking(), is("xml"));
        assertThat("Bibliographic record recordSchema", bibliographicRecord.getRecordSchema(), is("info:lc/xmlns/marcxchange-v1"));

        BibliographicRecordExtraData bibliographicRecordExtraData = unmarshallBibliographicRecordExtraData(
                (Element) bibliographicRecord.getExtraRecordData().getContent().get(0));
        assertThat("Bibliographic record extra data queue provider", bibliographicRecordExtraData.getProviderName(),
                is("myQueueProvider"));
        assertThat("Bibliographic record extra data queue priority", bibliographicRecordExtraData.getPriority(),
                is(1000));

        Element element = (Element) bibliographicRecord.getRecordData().getContent().get(0);
        byte[] bibliographicalContent = bibliographicRecordFactory.documentToByteArray(element.getOwnerDocument());
        String bibliographicalContentXml = new String(bibliographicalContent, StandardCharsets.UTF_8);
        assertThat(bibliographicalContentXml, CompareMatcher.isIdenticalTo(marcXchange));
    }
}
