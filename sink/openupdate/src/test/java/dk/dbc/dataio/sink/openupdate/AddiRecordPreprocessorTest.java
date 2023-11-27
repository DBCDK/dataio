package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraData;
import dk.dbc.dataio.sink.openupdate.bindings.BibliographicRecordExtraDataMarshallerTest;
import dk.dbc.dataio.sink.util.DocumentTransformer;
import dk.dbc.oss.ns.catalogingupdate.BibliographicRecord;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class AddiRecordPreprocessorTest extends AbstractOpenUpdateSinkTestBase {
    private final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private final DocumentTransformer documentTransformer = new DocumentTransformer();
    private final String submitter = "870970";
    private final String updateTemplate = "bog";
    private final String queueProvider = null;
    private final AddiRecord addiRecord = newAddiRecord(getMetaXml(updateTemplate, submitter), getContentXml());

    @Test
    public void preprocess_addiArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> addiRecordPreprocessor.preprocess(null, queueProvider));
    }

    @Test
    public void preprocess_addiArgIsInvalid_throws() {
        AddiRecord addiRecord = newAddiRecord("", getContentXml());
        assertThrows(IllegalArgumentException.class, () -> addiRecordPreprocessor.preprocess(addiRecord, queueProvider));
    }

    @Test
    public void preprocess_esInfoElementNotFound_throws() {
        String invalidMetaXml = getInvalidMetaXml(
                "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>");
        AddiRecord addiRecord = newAddiRecord(invalidMetaXml, getContentXml());
        try {
            addiRecordPreprocessor.preprocess(addiRecord, queueProvider);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat("Message contains: " + AddiRecordPreprocessor.ES_INFO_ELEMENT, e.getMessage().contains(AddiRecordPreprocessor.ES_INFO_ELEMENT), is(true));
            assertThat("Message contains: " + AddiRecordPreprocessor.ES_NAMESPACE_URI, e.getMessage().contains(AddiRecordPreprocessor.ES_NAMESPACE_URI), is(true));
        }
    }

    @Test
    public void preprocess_updateElementNotFound_throws() {
        String invalidMetaXml = getInvalidMetaXml(
                "<es:info submitter=\"870970\"/>");
        AddiRecord addiRecord = newAddiRecord(invalidMetaXml, getContentXml());
        try {
            addiRecordPreprocessor.preprocess(addiRecord, queueProvider);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat("Message contains: " + AddiRecordPreprocessor.UPDATE_TEMPLATE_ELEMENT, e.getMessage().contains(AddiRecordPreprocessor.UPDATE_TEMPLATE_ELEMENT), is(true));
            assertThat("Message contains: " + AddiRecordPreprocessor.DATAIO_PROCESSING_NAMESPACE_URI, e.getMessage().contains(AddiRecordPreprocessor.DATAIO_PROCESSING_NAMESPACE_URI), is(true));
        }
    }

    @Test
    public void preprocess_esInfoSubmitterAttributeFound_setsSubmitterToAttributeValue() {
        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

        // Verify
        assertThat("update value", result.getTemplate(), is(updateTemplate));
    }

    @Test
    public void preprocess_updateTemplateAttributeFound_setsTemplateToAttributeValue() {
        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

        // Verify
        assertThat("submitter value", result.getSubmitter(), is(submitter));
    }

    @Test
    public void preprocess_esInfoSubmitterAttributeNotFound_setsSubmitterToEmptyString() {
        String invalidMetaXml = getInvalidMetaXml(
                "<es:info/>" +
                        "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>"
        );
        AddiRecord addiRecord = newAddiRecord(invalidMetaXml, getContentXml());

        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

        // Verify
        assertThat("submitter is empty", result.getSubmitter(), is(""));
    }

    @Test
    public void preprocess_updateTemplateAttributeNotFound_setsTemplateToEmptyString() {
        String invalidMetaXml = getInvalidMetaXml(
                "<es:info submitter=\"870970\"/>" +
                        "<dataio:sink-update-template xmlns:dataio=\"dk.dbc.dataio.processing\"/>"
        );
        AddiRecord addiRecord = newAddiRecord(invalidMetaXml, getContentXml());

        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);

        // Verify
        assertThat("template is empty", result.getTemplate(), is(""));
    }

    @Test
    public void preprocess_contentIsValid_setsBibliographicalRecord() throws TransformerException {
        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);
        BibliographicRecord bibliographicalRecord = result.getBibliographicRecord();

        // Verify
        assertThat("Bibliographical record not null", bibliographicalRecord, not(nullValue()));
        assertThat("BibliographicRecord recordPackaging", bibliographicalRecord.getRecordPacking(), is(AddiRecordPreprocessor.RECORD_PACKAGING));
        assertThat("BibliographicRecord recordSchema", bibliographicalRecord.getRecordSchema(), is(AddiRecordPreprocessor.RECORD_SCHEMA));
        assertThat("BibliographicRecord extraRecordData is empty", bibliographicalRecord.getExtraRecordData().getContent().size(), is(0));

        Element element = (Element) bibliographicalRecord.getRecordData().getContent().get(0);
        byte[] bibliographicalContent = documentTransformer.documentToByteArray(element.getOwnerDocument());
        String bibliographicalContentXml = new String(bibliographicalContent, StandardCharsets.UTF_8);
        assertThat("BibliographicalRecord content matches addi content", bibliographicalContentXml, isEquivalentTo(getContentXml()));
    }

    @Test
    public void preprocess_queueProviderIsNonNull_setsBibliographicalRecordExtraData() throws JAXBException {
        final String queueProvider = "queue";

        // Subject under test
        AddiRecordPreprocessor.Result result = addiRecordPreprocessor.preprocess(addiRecord, queueProvider);
        BibliographicRecord bibliographicalRecord = result.getBibliographicRecord();

        // Verify
        assertThat("BibliographicRecord extraRecordData is non-empty", bibliographicalRecord.getExtraRecordData().getContent().size(), is(1));
        Document bibliographicRecordExtraDataDocument =
                ((Element) bibliographicalRecord.getExtraRecordData().getContent().get(0)).getOwnerDocument();
        BibliographicRecordExtraData bibliographicRecordExtraData =
                BibliographicRecordExtraDataMarshallerTest.unmarshall(bibliographicRecordExtraDataDocument);
        assertThat(bibliographicRecordExtraData.getProviderName(), is(queueProvider));
    }
}
