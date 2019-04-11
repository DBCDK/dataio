package dk.dbc.dataio.sink.diff;

public class AbstractDiffGeneratorTest {
    public ExternalToolDiffGenerator newXmlDiffGenerator() {
        String xmldiff = System.getProperty("test.script.xmldiff");
        ExternalToolDiffGenerator xmlDiffGenerator = new ExternalToolDiffGenerator();
        xmlDiffGenerator.xmlDiffPath = xmldiff;
        //xmlDiffGenerator.threadFactory = Thread::new;
        return xmlDiffGenerator;
    }
}
