package dk.dbc.dataio.jobstore.recordsplitter;

import dk.dbc.dataio.jobstore.types.IllegalDataException;
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
 * Reads XML-files and split them on first child below the root element.
 *
 * A simple splitter which read XML from an {@link InputStream} and can iterate over, and extract children below the root element.
 *
 * Example:
 * <p>
 * Given an XML-file like this:
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the first child</child>
 *     <child>This is the second child</child>
 * </test>
 * }
 * </pre>
 * DefaultXMLRecordSplitter will produce the two following XMLStrings:
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the first child</child>
 * </test>
 * }
 * </pre>
 * and
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the second child</child>
 * </test>
 * }
 * </pre>
 * if you use it like this:
 * <pre>
 * {@code
 * try {
 *   DefaultXMLRecordSplitter recordSplitter = new DefaultXMLRecordSplitter(inputStream);
 *   for(String record : recordSplitter) {
 *      // do something with record.
 *   }
 * } catch(IllegalDataException ex) {
 *     // Something was wrong with the inputdata.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the DefaultXMLRecordSplitter implements {@link Iterable},
 * and you can step through the records one at the time.
 * Also note, that if an error occurs, {@link IllegalDataException} is thrown.
 * {@link IllegalDataException} is a {@link RuntimeException} since the {@link Iterable}
 * interface does not allow checked exceptions to be thrown.
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
     * Creates an instance of DefaultXMLRecordSplitter ready to read from {@code inputStream}.
     *
     * @param inputStream The {@link InputStream} containing the XML to be read.
     * @throws XMLStreamException
     */
    public DefaultXMLRecordSplitter(InputStream inputStream) throws IllegalDataException, XMLStreamException {
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
            String errMsg = "Could not peek at next event";
            log.error(errMsg, ex);
            throw new IllegalDataException(errMsg, ex);
        }
    }

    private boolean isNextEventEndElement() throws IllegalDataException {
        try {
            return xmlReader.peek().getEventType() == XMLEvent.END_ELEMENT;
        } catch (XMLStreamException ex) {
            String errMsg = "Could not peek at next event";
            log.error(errMsg, ex);
            throw new IllegalDataException(errMsg, ex);
        }
    }

    private boolean isNextEventDifferentFromStartElementAndEndElement() throws IllegalDataException {
        return !isNextEventStartElement() && !isNextEventEndElement();
    }

    @Override
    public Iterator<String> iterator() {
        Iterator<String> it = new Iterator<String>() {

            /**
             * @inheritDoc
             */
            @Override
            public boolean hasNext() throws IllegalDataException {
                return hasNextRecord();
            }

            /**
             * @inheritDoc
             */
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

            /**
             * @inheritDoc
             *
             * This method does not do anything.
             */
            @Override
            public void remove() {
            }
        };
        return it;
    }
}
