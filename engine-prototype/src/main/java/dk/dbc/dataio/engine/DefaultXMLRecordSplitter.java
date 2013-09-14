package dk.dbc.dataio.engine;

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

public class DefaultXMLRecordSplitter implements Iterable<String> {

    private final List<XMLEvent> preRecordEvents;
    private final XMLEventReader xmlReader;
    private final String rootTag;
    private final XMLEventFactory xmlEventFactory;

    public DefaultXMLRecordSplitter(InputStream is) throws XMLStreamException {
        xmlEventFactory = XMLEventFactory.newInstance();
        xmlReader = XMLInputFactory.newFactory().createXMLEventReader(is, "UTF-8");

        preRecordEvents = findPreRecordEvents();
        rootTag = findRootTagFromPreRecordEvents();
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

    @Override
    public Iterator<String> iterator() {
        Iterator<String> it = new Iterator<String>() {
            @Override
            public boolean hasNext() {
                try {
                    return hasNextRecord();
                } catch (XMLStreamException ex) {
                    // todo: handle exception
                }
                return false;
            }

            @Override
            public String next() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Writer writer = new BufferedWriter(new OutputStreamWriter(baos, "UTF-8"));
                    XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
                    XMLEventWriter xmlWriter = xmlof.createXMLEventWriter(writer);

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

                    return baos.toString("UTF-8");
                } catch (XMLStreamException | UnsupportedEncodingException ex) {
                    // todo: handle exception
                }
                return null;
            }

            @Override
            public void remove() {
            }
        };
        return it;
    }

    private boolean hasNextRecord() throws XMLStreamException {
        return xmlReader.peek().getEventType() == XMLEvent.START_ELEMENT;
    }

    private List<XMLEvent> findPreRecordEvents() throws XMLStreamException {
        List<XMLEvent> preRecordEvents = new ArrayList<>();

        XMLEvent e;
        // retrieve header
        while (xmlReader.peek().getEventType() != XMLEvent.START_ELEMENT) {
            e = xmlReader.nextEvent();
            preRecordEvents.add(e);
        }

        // retrieve root tag and contents of tag.
        do {
            e = xmlReader.nextEvent();
            preRecordEvents.add(e);
        } while (xmlReader.peek().getEventType() != XMLEvent.START_ELEMENT);

        return preRecordEvents;
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
        } while (depth > 0 || (xmlReader.peek().getEventType() != XMLEvent.END_ELEMENT && xmlReader.peek().getEventType() != XMLEvent.START_ELEMENT));

        return recordEvents;
    }

//    private boolean isNextEventStartElement(XMLEventReader xmlReader) throws XMLStreamException {
//        return xmlReader.peek().getEventType() == XMLEvent.START_ELEMENT;
//    }
    private String resolveEvent(int eventType) {
        String s;
        switch (eventType) {
            case XMLEvent.ATTRIBUTE:
                s = "Attribute";
                break;
            case XMLEvent.CDATA:
                s = "CDATA";
                break;
            case XMLEvent.CHARACTERS:
                s = "Characters";
                break;
            case XMLEvent.COMMENT:
                s = "Comment";
                break;
            case XMLEvent.END_DOCUMENT:
                s = "End document";
                break;
            case XMLEvent.START_DOCUMENT:
                s = "Start document";
                break;
            case XMLEvent.START_ELEMENT:
                s = "Start element";
                break;
            case XMLEvent.END_ELEMENT:
                s = "End element";
                break;
            case XMLEvent.SPACE:
                s = "Space";
                break;
            default:
                s = Integer.toString(eventType);
                break;
        }
        return s;
    }
}
