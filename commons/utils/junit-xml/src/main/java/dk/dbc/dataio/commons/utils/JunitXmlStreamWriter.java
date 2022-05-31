package dk.dbc.dataio.commons.utils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * XmlStreamWriter capable of writing junit XML reports (as understood by the Jenkins CI)
 */
public class JunitXmlStreamWriter implements AutoCloseable {
    final XMLStreamWriter out;

    public JunitXmlStreamWriter(OutputStream os) throws XMLStreamException {
        final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        out = outputFactory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());
        out.writeStartElement("testsuites");
    }

    @Override
    public void close() throws Exception {
        if (out != null) {
            out.writeEndElement();
            out.close();
        }
    }
}
