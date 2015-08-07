package dk.dbc.dataio.sink.diff;

import dk.dbc.xmldiff.XmlDiff;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class XmlDiffGeneratorTest {

    @Test
    public void testHasDiff_contentEqual_returnsFalse() throws IOException, SAXException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        boolean hasDiff = xmlDiffGenerator.hasDiff(XmlDiff.Result.CONTENT_EQUAL);
        assertThat(hasDiff, is(false));
    }

    @Test
    public void testHasDiff_different_returnsTrue() throws IOException, SAXException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        boolean hasDiff = xmlDiffGenerator.hasDiff(XmlDiff.Result.DIFFERENT);
        assertThat(hasDiff, is(true));
    }

    @Test
    public void testGetDiff_semanticEqual_returnsEmptyString() throws DiffGeneratorException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        String diff = xmlDiffGenerator.getDiff(getXml(), getXmlSemanticEquals());
        assertThat(diff, is(""));
    }

    @Test
    public void testGetDiff_different_returnsDiffString() throws DiffGeneratorException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        String diff = xmlDiffGenerator.getDiff(getXml(), getXmlNext());
        assertThat(diff, not(""));
    }

    @Test
    public void testGetDiff_contentEquals_returnsEmptyString() throws DiffGeneratorException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        String diff = xmlDiffGenerator.getDiff(getXml(), getXml());
        assertThat(diff, is(""));
    }

    @Test
    public void testGetDiff_failureComparingInput_throws() throws DiffGeneratorException {
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        try {
            xmlDiffGenerator.getDiff("<INVALID>".getBytes(), "<INVALID>".getBytes());
        } catch (DiffGeneratorException e) {}

    }

    private byte[] getXml() {
        return ("<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Moon</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private byte[] getXmlNext() {
        return ("<dataio-harvester-datafile>" +
                "<data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader> " +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Sun</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private byte[] getXmlSemanticEquals() {
        return ("<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data><foo:collection xmlns:foo=\"info:lc/xmlns/marcxchange-v1\">" +
                "<foo:record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<foo:leader>00000n 2200000 4500</foo:leader>" +
                "<foo:datafield ind2=\"0\" ind1=\"0\" tag=\"001\">" +
                "<foo:subfield code=\"a\">Sun Kil Moon</foo:subfield>" +
                "</foo:datafield>" +
                "</foo:record></foo:collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }
}
