package dk.dbc.dataio.commons.utils;

import javax.xml.stream.XMLStreamException;

/**
 * Test case abstraction for JUNIT XML reports
 */
public class JunitXmlTestCase {
    private final Type type;

    enum Type {
        ERROR,
        FAILURE,
        PASSED,
        SKIPPED
    }

    public static JunitXmlTestCase erred(String name, String classname, String message, String output) {
        return new JunitXmlTestCase(name, classname, Type.ERROR, message, output);
    }

    public static JunitXmlTestCase failed(String name, String classname, String message, String output) {
        return new JunitXmlTestCase(name, classname, Type.FAILURE, message, output);
    }

    public static JunitXmlTestCase passed(String name, String classname) {
        return new JunitXmlTestCase(name, classname, Type.PASSED, null, null);
    }

    public static JunitXmlTestCase skipped(String name, String classname, String message, String output) {
        return new JunitXmlTestCase(name, classname, Type.SKIPPED, message, output);
    }

    private final String name;
    private final String classname;
    private final String message;
    private final String output;

    private String stderr;
    private String stdout;
    private Integer time;

    public JunitXmlTestCase(String name, String classname, Type type, String message, String output) {
        this.name = name;
        this.classname = classname;
        this.type = type;
        this.message = message;
        this.output = output;
    }

    public JunitXmlTestCase withTime(Integer time) {
        this.time = time;
        return this;
    }

    public JunitXmlTestCase withStderr(String stderr) {
        this.stderr = stderr;
        return this;
    }

    public JunitXmlTestCase withStdout(String stdout) {
        this.stdout = stdout;
        return this;
    }

    void write(JunitXmlStreamWriter writer) throws XMLStreamException {
        writer.out.writeStartElement("testcase");
        writer.out.writeAttribute("classname", classname);
        writer.out.writeAttribute("name", name);
        if (time != null) {
            writer.out.writeAttribute("time", String.valueOf(time));
        }
        if (type != Type.PASSED) {
            writeType(writer, type);
        }
        if (stdout != null) {
            writer.out.writeStartElement("system-out");
            writer.out.writeCharacters(stdout);
            writer.out.writeEndElement();
        }
        if (stderr != null) {
            writer.out.writeStartElement("system-err");
            writer.out.writeCharacters(stderr);
            writer.out.writeEndElement();
        }
        writer.out.writeEndElement();
    }

    private void writeType(JunitXmlStreamWriter writer, Type type) throws XMLStreamException {
        writer.out.writeStartElement(type.name().toLowerCase());
        writer.out.writeAttribute("message", message);
        if (output != null) {
            writer.out.writeCharacters(output);
        }
        writer.out.writeEndElement();
    }
}
