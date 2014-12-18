package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.DataException;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A factory for simple partitioner of XML data read from an {@link InputStream} able to iterate over and extract
 * children below the root element.
 *
 * Example:
 * <p>
 * Given XML input like this:
 * <pre>
 * {@code
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the first child</child>
 *     <child>This is the second child</child>
 * </test>
 * }
 * </pre>
 * DefaultXmlDataPartitioner will produce the two following XMLStrings:
 * <pre>
 * {@code
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the first child</child>
 * </test>
 * }
 * </pre>
 * and
 * <pre>
 * {@code
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <test>
 *     <child>This is the second child</child>
 * </test>
 * }
 * </pre>
 * if you use it like this:
 * <pre>
 * {@code
 *
 * try {
 *   final DataPartitionerFactory.DataPartitioner dataPartitioner = new DefaultXmlDataPartitionerFactory()
                .createDataPartitioner(inputStream, encoding);
 *   for(String record : dataPartitioner) {
 *      // do something with record.
 *   }
 * } catch(DataException e) {
 *     // Something was wrong with the input data.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the DefaultXmlDataPartitionerFactory.createDataPartitioner() method returns
 * a {@link DataPartitioner}, enabling you to step through the records one at a time.
 * Also note, that if an error occurs, a {@link DataException} or sub type thereof is thrown.
 * {@link DataException} is a {@link RuntimeException} since the {@link Iterable}
 * interface all DataPartitioner implementations must implement does not allow checked exceptions to be thrown.
 * When the expected data encoding set via the createDataPartitioner() method call differs from the actual encoding
 * a {@link InvalidEncodingException} is thrown.
 * When the input data in any other way is deemed invalid a {@link InvalidDataException} is thrown.
 */
public class DefaultXmlDataPartitionerFactory implements DataPartitionerFactory {
    /**
     * Creates new instance of default XML DataPartitioner
     * @param inputStream stream from which XML data to be partitioned can be read
     * @param encoding encoding of XML data to be partitioned
     * @return new instance of default XML DataPartitioner
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    @Override
    public DataPartitioner createDataPartitioner(InputStream inputStream, String encoding)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");
        return new DefaultXmlDataPartitioner(inputStream, encoding);
    }

    private static class DefaultXmlDataPartitioner implements DataPartitioner {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlDataPartitioner.class);

        private final XMLEventFactory xmlEventFactory;
        private final XMLOutputFactory xmlOutputFactory;
        private final InputStream inputStream;
        private final String expectedEncoding;
        private String encoding;
        private String rootTag;
        private XMLEventReader xmlReader;
        private List<XMLEvent> preRecordEvents;

        public DefaultXmlDataPartitioner(InputStream inputStream, String expectedEncoding) {
            this.inputStream = inputStream;
            this.expectedEncoding = expectedEncoding;
            encoding = StandardCharsets.UTF_8.name();
            xmlEventFactory = XMLEventFactory.newInstance();
            xmlOutputFactory = XMLOutputFactory.newInstance();
        }

        @Override
        public Iterator<String> iterator() throws DataException {
            try {
                xmlReader = XMLInputFactory.newFactory().createXMLEventReader(inputStream);
                findPreRecordEvents();
                findRootTagFromPreRecordEvents();
                validateEncoding();
            } catch (XMLStreamException e) {
                throw new InvalidDataException(e);
            }

            return new Iterator<String>() {
                /**
                 * @inheritDoc
                 */
                @Override
                public boolean hasNext() throws DataException {
                    try {
                        return hasNextRecord();
                    } catch (XMLStreamException e) {
                        throw new DataException(e);
                    }
                }

                /**
                 * @inheritDoc
                 */
                @Override
                public String next() throws DataException {
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
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final Writer writer = new BufferedWriter(new OutputStreamWriter(baos, encoding));
                        final XMLEventWriter xmlWriter = xmlOutputFactory.createXMLEventWriter(writer);

                        for (XMLEvent e : preRecordEvents) {
                            xmlWriter.add(e);
                        }

                        final List<XMLEvent> recordEvents = findRecordEvents();
                        for (XMLEvent e : recordEvents) {
                            xmlWriter.add(e);
                        }
                        xmlWriter.add(xmlEventFactory.createEndElement("", null, rootTag));
                        xmlWriter.add(xmlEventFactory.createEndDocument());
                        xmlWriter.close();

                        return baos.toString(encoding);
                    } catch (XMLStreamException | UnsupportedEncodingException e) {
                        LOGGER.error("Exception caught", e);
                        throw new InvalidDataException(e);
                    }
                }

                /**
                 * @inheritDoc This method does not do anything.
                 */
                @Override
                public void remove() {
                }
            };
        }

        private void validateEncoding() throws InvalidEncodingException {
            if (!EncodingsUtil.isEquivalent(encoding, expectedEncoding)) {
                throw new InvalidEncodingException(String.format(
                        "Actual encoding '%s' differs from expected '%s' encoding", encoding, expectedEncoding));
            }
        }

        private void findPreRecordEvents() throws XMLStreamException {
            preRecordEvents = new ArrayList<>();

            XMLEvent e;
            // handle xml declaration and processing instructions (if any)
            while (!isNextEventStartElement()) {
                e = xmlReader.nextEvent();
                if (e.getEventType() == XMLEvent.START_DOCUMENT) {
                    final StartDocument sd = ((StartDocument) e);
                    if (sd.encodingSet()) {
                        encoding = sd.getCharacterEncodingScheme();
                        LOGGER.info("Using {} encoding set in document", encoding);
                    }
                }
                preRecordEvents.add(e);
            }
            // retrieve root tag and contents of root tag.
            do {
                e = xmlReader.nextEvent();
                preRecordEvents.add(e);
            } while (isNextEventDifferentFromStartElementAndEndElement());
        }

        private void findRootTagFromPreRecordEvents() {
            final XMLEvent firstStartElementEvent = findFirstStartElementEventFromPreRecordEvents();
            rootTag = firstStartElementEvent.asStartElement().getName().toString();
        }

        private XMLEvent findFirstStartElementEventFromPreRecordEvents() throws DataException {
            for (XMLEvent e : preRecordEvents) {
                if (e.isStartElement()) {
                    return e;
                }
            }
            throw new InvalidDataException("Unable to find a root element in the xml stream");
        }

        private List<XMLEvent> findRecordEvents() throws XMLStreamException {
            final List<XMLEvent> recordEvents = new ArrayList<>(1000);
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

        private boolean hasNextRecord() throws XMLStreamException {
            return isNextEventStartElement();
        }

        private boolean isNextEventStartElement() throws XMLStreamException {
            try {
                return xmlReader.peek().getEventType() == XMLEvent.START_ELEMENT;
            } catch (XMLStreamException e) {
                LOGGER.error("Could not peek at next event", e);
                throw e;
            }
        }

        private boolean isNextEventEndElement() throws XMLStreamException {
            try {
                return xmlReader.peek().getEventType() == XMLEvent.END_ELEMENT;
            } catch (XMLStreamException e) {
                LOGGER.error("Could not peek at next event", e);
                throw e;
            }
        }

        private boolean isNextEventDifferentFromStartElementAndEndElement() throws XMLStreamException {
            return !isNextEventStartElement() && !isNextEventEndElement();
        }
    }
}
