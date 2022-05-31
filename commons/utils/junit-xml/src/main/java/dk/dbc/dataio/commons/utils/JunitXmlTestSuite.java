package dk.dbc.dataio.commons.utils;

import javax.xml.stream.XMLStreamException;

/**
 * Streams test suites to JUNIT XML report
 */
public class JunitXmlTestSuite implements AutoCloseable {
    private final JunitXmlStreamWriter writer;

    public JunitXmlTestSuite(String name, JunitXmlStreamWriter writer) throws XMLStreamException {
        this.writer = writer;
        this.writer.out.writeStartElement("testsuite");
        this.writer.out.writeAttribute("name", name);
    }

    /**
     * Writes given {@link JunitXmlTestCase} to this test suite
     *
     * @param testCase test case to be written
     * @throws XMLStreamException on failure to write test case
     */
    public void addTestCase(JunitXmlTestCase testCase) throws XMLStreamException {
        testCase.write(writer);
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            writer.out.writeEndElement();
        }
    }
}
