package dk.dbc.dataio.commons.utils.lang;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JaxpUtil unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class JaxpUtilTest {
    private final String xmlRecord = "<data/>";
    private final String xmlRecordWithNamespace = "<ns:data xmlns:ns=\"http://test.dbc.dk/ns\"/>";

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsNull_throwsNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> JaxpUtil.parseDocument((InputSource) null));
    }

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsInvalidXml_throwsSAXException() throws Exception {
        assertThrows(SAXException.class, () -> JaxpUtil.parseDocument(stringToInputSource("not xml")));
    }

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsValidXml_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(stringToInputSource(xmlRecord));
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void parseDocumentTakingInputSourceParameter_xmlArgIsValidXmlWithNs_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(stringToInputSource(xmlRecordWithNamespace));
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void parseDocumentTakingStringParameter_xmlArgIsNull_throwsNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> JaxpUtil.parseDocument((String)null));
    }

    @Test
    public void parseDocumentTakingStringParameter_xmlArgIsEmpty_throwsIllegalArgumentException() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> JaxpUtil.parseDocument(""));
    }

    @Test
    public void parseDocumentTakingStringParameter_xmlArgIsInvalidXml_throwsSAXException() throws Exception {
        assertThrows(SAXException.class, () -> JaxpUtil.parseDocument("not xml"));
    }

    @Test
    public void parseDocumentTakingStringParameter_xmlArgIsValidXml_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecord);
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void parseDocumentTakingInputStringParameter_xmlArgIsValidXmlWithNs_returnsDocumentInstance() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecordWithNamespace);
        assertThat(doc, is(notNullValue()));
    }

    @Test
    public void asString_nodeArgIsNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> JaxpUtil.asString(null));
    }

    @Test
    public void asString_nodeArgIsValid_returnsStringRepesentation() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecord);
        String docAsString = JaxpUtil.asString(doc.getDocumentElement());
        assertThat(docAsString, is(xmlRecord));
    }

    @Test
    public void asString_nodeArgIsValidWithNamespace_returnsStringRepesentation() throws Exception {
        Document doc = JaxpUtil.parseDocument(xmlRecordWithNamespace);
        String docAsString = JaxpUtil.asString(doc.getDocumentElement());
        assertThat(docAsString, is(xmlRecordWithNamespace));
    }

    private InputSource stringToInputSource(String str) {
        return new InputSource(new StringReader(str));
    }
}
