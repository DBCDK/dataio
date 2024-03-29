package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class XmlDiffGeneratorTest extends AbstractDiffGeneratorTest {

    @Disabled("xmllint + diff cannot handle default >< explicit namespaces")
    @Test
    public void testGetDiff_semanticEqual_returnsEmptyString() throws DiffGeneratorException, InvalidMessageException {
        ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
        String diff = xmlDiffGenerator.getDiff(Kind.XML, getXml(), getXmlSemanticEquals());
        assertThat(diff, is(""));
    }

    @Test
    public void testXmlUnitDiff_semanticEqual_returnsEmptyString() throws DiffGeneratorException, InvalidMessageException {
        DiffGenerator generator = new JavaDiffGenerator();
        String diff = generator.getDiff(Kind.XML, getXml(), getXmlSemanticEquals());
        assertThat(diff, is(""));
    }

    @Test
    public void testGetDiff_different_returnsDiffString() throws DiffGeneratorException, InvalidMessageException {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            String diff = xmlDiffGenerator.getDiff(Kind.XML, getXml(), getXmlNext());
            assertThat(diff, not(""));
        }
    }

    @Test
    public void testXmlUnitDiff_different_returnsDiffString() throws DiffGeneratorException, InvalidMessageException {
        DiffGenerator diffGenerator = new JavaDiffGenerator();
        String diff = diffGenerator.getDiff(Kind.XML, getXml(), getXmlNext());
        assertThat(diff, not(""));
    }


    @Test
    public void testGetDiff_bug18965() throws DiffGeneratorException, IOException, URISyntaxException, InvalidMessageException {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            String diff = xmlDiffGenerator.getDiff(Kind.XML,
                    readTestRecord("bug_18956.xml"),
                    readTestRecord("bug_18956-differences.xml"));
            assertThat(diff, not(""));
        }
    }


    @Test
    public void testGetDiff_output() throws DiffGeneratorException, IOException, URISyntaxException, InvalidMessageException {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            String diff = xmlDiffGenerator.getDiff(Kind.XML,
                    readTestRecord("small-current.xml"),
                    readTestRecord("small-next.xml"));
            assertThat(diff, not(""));
        }
    }

    @Test
    public void testGetDiff_contentEquals_returnsEmptyString() throws DiffGeneratorException, InvalidMessageException {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            String diff = xmlDiffGenerator.getDiff(Kind.XML, getXml(), getXml());
            assertThat(diff, is(""));
        }
    }

    @Test
    public void testGetDiff_failureComparingInput_throws() {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            assertThrows(DiffGeneratorException.class, () -> xmlDiffGenerator.getDiff(Kind.XML, "<INVALID>".getBytes(), "<INVALID>".getBytes()));
        }
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


    static byte[] readTestRecord(String resourceName) throws IOException, URISyntaxException {
        URL url = XmlDiffGeneratorTest.class.getResource("/" + resourceName);
        Path resPath;
        resPath = Paths.get(url.toURI());
        return Files.readAllBytes(resPath);
    }

    static byte[] readTestRecord(Path resourcePath) throws IOException, URISyntaxException {
        return Files.readAllBytes(resourcePath);
    }

}
