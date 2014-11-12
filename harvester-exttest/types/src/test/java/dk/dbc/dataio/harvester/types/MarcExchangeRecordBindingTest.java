package dk.dbc.dataio.harvester.types;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MarcExchangeRecordBindingTest {
    private static final String IDENTIFIER = "id";
    private static final int LIBRARY_NUMBER = 870970;

    private static final String MARCX_COLLECTION_001B_VALID =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"a\">" + IDENTIFIER + "</marcx:subfield>" +
                        "<marcx:subfield code=\"b\">"+ LIBRARY_NUMBER + "</marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";

    private static final String MARCX_COLLECTION_001A_MISSING =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"b\">"+ LIBRARY_NUMBER + "</marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";

    private static final String MARCX_COLLECTION_001A_EMPTY =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"a\"></marcx:subfield>" +
                        "<marcx:subfield code=\"b\">"+ LIBRARY_NUMBER + "</marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";


    private static final String MARCX_COLLECTION_001B_MISSING =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"a\">" + IDENTIFIER + "</marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";

    private static final String MARCX_COLLECTION_001B_EMPTY =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"a\">" + IDENTIFIER + "</marcx:subfield>" +
                        "<marcx:subfield code=\"b\"></marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";

    private static final String MARCX_COLLECTION_001B_NON_NUMERIC =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                    "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                        "<marcx:subfield code=\"a\">" + IDENTIFIER + "</marcx:subfield>" +
                        "<marcx:subfield code=\"b\">non-numeric</marcx:subfield>" +
                    "</marcx:datafield>" +
                "</marcx:record>" +
            "</marcx:collection>";

    @Test(expected = NullPointerException.class)
    public void constructor_documentElementArgIsNull_throws() {
        new MarcExchangeRecordBinding(null);
    }

    @Test
    public void constructor_documentElementArgIsValidMARCXchange_extractsValues() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001B_VALID.getBytes(StandardCharsets.UTF_8));
        final MarcExchangeRecordBinding marcExchangeRecordBinding = new MarcExchangeRecordBinding(marcxChange);
        assertThat(marcExchangeRecordBinding.getId(), is(IDENTIFIER));
        assertThat(marcExchangeRecordBinding.getLibrary(), is(LIBRARY_NUMBER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_documentElementArgIsMissing001a_throws() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001A_MISSING.getBytes(StandardCharsets.UTF_8));
        new MarcExchangeRecordBinding(marcxChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_documentElementArgHasEmpty001a_throws() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001A_EMPTY.getBytes(StandardCharsets.UTF_8));
        new MarcExchangeRecordBinding(marcxChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_documentElementArgIsMissing001b_throws() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001B_MISSING.getBytes(StandardCharsets.UTF_8));
        new MarcExchangeRecordBinding(marcxChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_documentElementArgHasEmpty001b_throws() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001B_EMPTY.getBytes(StandardCharsets.UTF_8));
        new MarcExchangeRecordBinding(marcxChange);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_documentElementArgHasNonNumeric001b_throws() {
        final Document marcxChange = asDocument(MARCX_COLLECTION_001B_NON_NUMERIC.getBytes(StandardCharsets.UTF_8));
        new MarcExchangeRecordBinding(marcxChange);
    }

    private Document asDocument(byte[] data) {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(new ByteArrayInputStream(data));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}