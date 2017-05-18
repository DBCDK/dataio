package dk.dbc.dataio.sink.diff;

public class AbstractDiffGeneratorTest {
    public XmlDiffGenerator newXmlDiffGenerator() {
        String xmldiff = System.getProperty("test.script.xmldiff");
        XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        xmlDiffGenerator.xmlDiffPath = xmldiff;
        xmlDiffGenerator.threadFactory = Thread::new;
        return xmlDiffGenerator;
    }
}
