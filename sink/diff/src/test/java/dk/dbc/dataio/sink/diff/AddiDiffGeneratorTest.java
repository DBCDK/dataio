package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class AddiDiffGeneratorTest {

    private static final String CONTENT = "title1";
    private static final String NEXT_CONTENT = "title2";
    private static final String META = "basis1";
    private static final String NEXT_META = "basis2";
    private static final String EMPTY = "";

    private final AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator();

    @Test
    public void testGetDiff_addiRecordsAreIdentical_returnsEmptyString() throws DiffGeneratorException, IOException {
        AddiRecord addiRecord = getAddiRecord(getMeta(META), getContent(CONTENT));

        String diff = addiDiffGenerator.getDiff(addiRecord, addiRecord);
        assertThat(diff, is(EMPTY));
    }

    @Test
    public void testGetDiff_metaDataIsNotIdentical_returnsMetaDataDiff() throws DiffGeneratorException, IOException {
        AddiRecord currentAddiRecord = getAddiRecord(getMeta(META), getContent(CONTENT));
        AddiRecord nextAddiRecord = getAddiRecord(getMeta(NEXT_META), getContent(CONTENT));

        String diff = addiDiffGenerator.getDiff(currentAddiRecord, nextAddiRecord);

        // Assert that the diff contains meta data
        assertThat(diff.contains(META), is(true));
        assertThat(diff.contains(NEXT_META), is(true));

        // Assert that the diff does not contain content data
        assertThat(diff.contains(CONTENT), is(false));
    }

    @Test
    public void testGetDiff_contentDataIsNotIdentical_returnsContentDataDiff() throws DiffGeneratorException, IOException {
        AddiRecord currentAddiRecord = getAddiRecord(getMeta(META), getContent(CONTENT));
        AddiRecord nextAddiRecord = getAddiRecord(getMeta(META), getContent(NEXT_CONTENT));

        String diff = addiDiffGenerator.getDiff(currentAddiRecord, nextAddiRecord);

        // Assert that the diff contains content data
        assertThat(diff.contains(CONTENT), is(true));
        assertThat(diff.contains(NEXT_CONTENT), is(true));

        // Assert that the diff does not contain meta data
        assertThat(diff.contains(META), is(false));
    }

    @Test
    public void testGetDiff_contentDataAndMetaDataAreNotIdentical_returnsMetaPlusContentDataDiff() throws DiffGeneratorException, IOException {
        AddiRecord currentAddiRecord = getAddiRecord(getMeta(META), getContent(CONTENT));
        AddiRecord nextAddiRecord = getAddiRecord(getMeta(NEXT_META), getContent(NEXT_CONTENT));

        String diff = addiDiffGenerator.getDiff(currentAddiRecord, nextAddiRecord);

        // Assert that the diff contain meta data
        assertThat(diff.contains(META), is(true));
        assertThat(diff.contains(NEXT_META), is(true));

        // Assert that the diff contains content data
        assertThat(diff.contains(CONTENT), is(true));
        assertThat(diff.contains(NEXT_CONTENT), is(true));
    }

    @Test
    public void testGetDiff_invalidXml_throws() throws IOException {
        AddiRecord addiRecord = getAddiRecord(EMPTY, getInvalidContent());
        try {
            addiDiffGenerator.getDiff(addiRecord, addiRecord);
            fail();
        } catch (DiffGeneratorException e) { }
    }

    private AddiRecord getAddiRecord(String metaXml, String contentXml) throws IOException {
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(getAddi(metaXml, contentXml)));
        return addiReader.getNextRecord();
    }

    private String getMeta(String attributeValue) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"" + attributeValue + "\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private String getContent(String contentAttributeValue) {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">" + contentAttributeValue + "</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private String getInvalidContent() {
        return  "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private byte[] getAddi(String metaXml, String contentXml) {
        return (metaXml.trim().getBytes().length +
                System.lineSeparator() +
                metaXml +
                System.lineSeparator() +
                contentXml.trim().getBytes().length +
                System.lineSeparator() +
                contentXml).getBytes();
    }

}
