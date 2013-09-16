package dk.dbc.dataio.engine;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 *
 */
public class DefaultXMLRecordSplitter implements Iterable<String> {

    private XLogger log = XLoggerFactory.getXLogger(DefaultXMLRecordSplitter.class);
    //
    private final static String LOCAL_CHARSET = "UTF-8";
    //
    private final XMLEventReader xmlReader;
    private final XMLEventFactory xmlEventFactory;
    private final XMLOutputFactory xmlOutputFactory;
    //
    private final List<XMLEvent> preRecordEvents;
    private final String rootTag;

    /**
     *
     * @param inputStream
     * @throws XMLStreamException
     */
    public DefaultXMLRecordSplitter(InputStream inputStream) throws XMLStreamException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");

        xmlEventFactory = XMLEventFactory.newInstance();
        xmlReader = XMLInputFactory.newFactory().createXMLEventReader(inputStream);
        xmlOutputFactory = XMLOutputFactory.newInstance();

        preRecordEvents = findPreRecordEvents();
        rootTag = findRootTagFromPreRecordEvents();

    }

    private List<XMLEvent> findPreRecordEvents() {
        List<XMLEvent> preRecordEvents = new ArrayList<>();

        try {
            XMLEvent e;
            // retrieve header
            while (!isNextEventStartElement()) {
                e = xmlReader.nextEvent();
                preRecordEvents.add(e);
            }
            // retrieve root tag and contents of root tag.
            do {
                e = xmlReader.nextEvent();
                preRecordEvents.add(e);
            } while (isNextEventDifferentFromStartElementAndEndElement());
        } catch (XMLStreamException ex) {
            throw new IllegalDataException(ex);
        }

        return preRecordEvents;
    }

    private String findRootTagFromPreRecordEvents() {
        XMLEvent firstStartElementEvent = findFirstStartElementEventFromPreRecordEvents();
        return firstStartElementEvent.asStartElement().getName().toString();
    }

    private XMLEvent findFirstStartElementEventFromPreRecordEvents() {
        for (XMLEvent e : preRecordEvents) {
            if (e.isStartElement()) {
                return e;
            }
        }
        throw new IllegalStateException("Could not find a root element in the xml stream");
    }

    private List<XMLEvent> findRecordEvents() throws XMLStreamException {
        List<XMLEvent> recordEvents = new ArrayList<>(1000);
        int depth = 0;

        XMLEvent e;
        do {
            e = xmlReader.nextEvent();
            if (e.isStartElement()) {
                depth++;
            }
            if (e.isEndElement()) {
                depth--;
            }
            recordEvents.add(e);
        } while (depth > 0 || isNextEventDifferentFromStartElementAndEndElement());

        return recordEvents;
    }

    private boolean hasNextRecord() throws IllegalDataException {
        return isNextEventStartElement();
    }

    private boolean isNextEventStartElement() throws IllegalDataException {
        try {
            return xmlReader.peek().getEventType() == XMLEvent.START_ELEMENT;
        } catch (XMLStreamException ex) {
            throw new IllegalDataException("Could not peek at next event", ex);
        }
    }

    private boolean isNextEventEndElement() throws IllegalDataException {
        try {
            return xmlReader.peek().getEventType() == XMLEvent.END_ELEMENT;
        } catch (XMLStreamException ex) {
            throw new IllegalDataException("Could not peek at next event", ex);
        }
    }

    private boolean isNextEventDifferentFromStartElementAndEndElement() throws IllegalDataException {
        return !isNextEventStartElement() && !isNextEventEndElement();
    }

    @Override
    public Iterator<String> iterator() {
        Iterator<String> it = new Iterator<String>() {
            @Override
            public boolean hasNext() throws IllegalDataException {
                return hasNextRecord();
            }

            @Override
            public String next() throws IllegalDataException {
                try {
                    // A note about optimization:
                    // It seems possible to move ByteArrayOutputStream,
                    // OutputStreamWriter and BufferedWriter out as private members
                    // of DefaultXMLRecordSplitter, and at each iteration call baos.reset();
                    // I'm not sure if the XMLEventWriter is reusable - look into it
                    // if you want to optimize.
                    //
                    // Another optimization point may be writing directly to the XMLEventWriter
                    // instead of storing the XMLEVents in a List for later writing.
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Writer writer = new BufferedWriter(new OutputStreamWriter(baos, LOCAL_CHARSET));
                    XMLEventWriter xmlWriter = xmlOutputFactory.createXMLEventWriter(writer);

                    for (XMLEvent e : preRecordEvents) {
                        xmlWriter.add(e);
                    }

                    List<XMLEvent> recordEvents = findRecordEvents();
                    for (XMLEvent e : recordEvents) {
                        xmlWriter.add(e);
                    }
                    xmlWriter.add(xmlEventFactory.createEndElement("", null, rootTag));
                    xmlWriter.add(xmlEventFactory.createEndDocument());
                    xmlWriter.close();

                    return baos.toString(LOCAL_CHARSET);
                } catch (XMLStreamException | UnsupportedEncodingException ex) {
                    log.error("Exception", ex);
                    throw new IllegalDataException(ex);
                }
            }

            @Override
            public void remove() {
            }
        };
        return it;
    }
}
