package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.util.CharacterEncodingScheme;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A factory for simple partitioner of XML data read from an {@link InputStream} able to iterate over and extract
 * children below the root element.
 * <p>
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
 *   final DataPartitioner dataPartitioner = DefaultXmlDataPartitioner.newInstance(inputStream, encoding);
 *   for(String record : dataPartitioner) {
 *      // do something with record.
 *   }
 * } catch(DataException e) {
 *     // Something was wrong with the input data.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the DefaultXmlDataPartitioner.newInstance() method returns
 * a {@link DataPartitioner}, enabling you to step through the records one at a time.
 * Also note, that if an error occurs, a {@link UnrecoverableDataException} or sub type thereof is thrown.
 * {@link UnrecoverableDataException} is a {@link RuntimeException} since the {@link Iterable}
 * interface all DataPartitioner implementations must implement does not allow checked exceptions to be thrown.
 * When the expected data encoding set via the createDataPartitioner() method call differs from the actual encoding
 * from the XML document a {@link InvalidEncodingException} is thrown.
 * When the input data in any other way is deemed invalid a {@link InvalidDataException} is thrown.
 */

public class DefaultXmlDataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlDataPartitioner.class);

    private final XMLEventFactory xmlEventFactory;
    private final XMLOutputFactory xmlOutputFactory;
    private final ByteCountingInputStream inputStream;
    private final Charset encodingExpected;
    private String encodingNameFromDocument;
    private String rootTag;
    private XMLEventReader xmlReader;
    private List<XMLEvent> preRecordEvents;
    protected Set<String> extractedKeys;
    protected Map<String, String> extractedValues;
    private Iterator<DataPartitionerResult> iterator;

    private int positionInDatafile;

    /**
     * Creates new instance of default XML DataPartitioner
     *
     * @param inputStream stream from which XML data to be partitioned can be read
     * @param encoding    encoding of XML data to be partitioned
     * @return new instance of default XML DataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if given invalid encoding name
     */
    public static DefaultXmlDataPartitioner newInstance(InputStream inputStream, String encoding)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");
        return new DefaultXmlDataPartitioner(inputStream, encoding, 0);
    }

    protected DefaultXmlDataPartitioner(InputStream inputStream, String encodingExpected) throws InvalidEncodingException {
        this(inputStream, encodingExpected, 0);
    }

    protected DefaultXmlDataPartitioner(InputStream inputStream, String encodingExpected, int startPositionInDatafile) throws InvalidEncodingException {
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.encodingExpected = CharacterEncodingScheme.charsetOf(encodingExpected);
        encodingNameFromDocument = StandardCharsets.UTF_8.name();
        xmlEventFactory = XMLEventFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
        extractedKeys = new HashSet<>();
        extractedValues = new HashMap<>();
        positionInDatafile = startPositionInDatafile;
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return StandardCharsets.UTF_8;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() throws UnrecoverableDataException, PrematureEndOfDataException {
        if (iterator == null) {
            try {
                xmlReader = XMLInputFactory.newFactory().createXMLEventReader(inputStream);
                findPreRecordEvents();
                findRootTagFromPreRecordEvents();
                validateEncoding();
            } catch (XMLStreamException e) {
                throw asRuntimeException(e);
            }

            iterator = new Iterator<DataPartitionerResult>() {
                /**
                 * @inheritDoc
                 */
                @Override
                public boolean hasNext() throws UnrecoverableDataException, PrematureEndOfDataException {
                    try {
                        return hasNextRecord();
                    } catch (XMLStreamException e) {
                        throw asRuntimeException(e);
                    }
                }

                /**
                 * @inheritDoc
                 */
                @Override
                public DataPartitionerResult next() throws UnrecoverableDataException {
                    try {
                        // A note about optimization:
                        // It seems possible to move ByteArrayOutputStream,
                        // OutputStreamWriter and BufferedWriter out as private members
                        // of DefaultXMLRecordSplitter, and at each iteration call baos.reset();
                        // I'm not sure if the XMLEventWriter is reusable - look into it
                        // if you want to optimize.
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final Writer writer = new BufferedWriter(new OutputStreamWriter(baos, getEncoding()));
                        final XMLEventWriter xmlWriter = xmlOutputFactory.createXMLEventWriter(writer);

                        for (XMLEvent e : preRecordEvents) {
                            xmlWriter.add(e);
                        }

                        findRecordEvents(xmlWriter);
                        xmlWriter.add(xmlEventFactory.createEndElement("", null, rootTag));
                        xmlWriter.add(xmlEventFactory.createEndDocument());
                        xmlWriter.close();
                        return nextDataPartitionerResult(baos);
                    } catch (XMLStreamException e) {
                        throw asRuntimeException(e);
                    } finally {
                        extractedValues.clear();
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
        return iterator;
    }

    /**
     * @param baos byte array output stream containing the relevant data
     * @return DataPartitionerResult containing chunk item with baos as data and status SUCCESS
     */
    protected DataPartitionerResult nextDataPartitionerResult(ByteArrayOutputStream baos) {
        final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withData(baos.toByteArray())
                .withType(ChunkItem.Type.UNKNOWN);
        return new DataPartitionerResult(chunkItem, null, positionInDatafile++);
    }

    private void validateEncoding() throws InvalidEncodingException {
        if (!encodingExpected.name().equals(CharacterEncodingScheme.charsetOf(encodingNameFromDocument).name())) {
            throw new InvalidEncodingException(String.format(
                    "Actual encoding '%s' differs from expected '%s' encoding", encodingNameFromDocument, encodingExpected));
        }
    }

    private void findPreRecordEvents() throws XMLStreamException {
        preRecordEvents = new ArrayList<>();

        XMLEvent e;
        // handle xml declaration and processing instructions (if any)
        while (!isNextEventStartElement()) {
            e = xmlReader.nextEvent();
            if (e.getEventType() == XMLEvent.START_DOCUMENT) {
                final StartDocument sd = (StartDocument) e;
                if (sd.encodingSet()) {
                    encodingNameFromDocument = sd.getCharacterEncodingScheme();
                    LOGGER.info("Input document specifies encoding {}", encodingNameFromDocument);
                    e = xmlEventFactory.createStartDocument();
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

    private XMLEvent findFirstStartElementEventFromPreRecordEvents() throws UnrecoverableDataException {
        for (XMLEvent e : preRecordEvents) {
            if (e.isStartElement()) {
                return e;
            }
        }
        throw new InvalidDataException("Unable to find a root element in the xml stream");
    }

    private void findRecordEvents(XMLEventWriter xmlWriter) throws XMLStreamException {
        int depth = 0;
        String extractedName = null;
        XMLEvent e;
        do {
            e = xmlReader.nextEvent();
            if (e.isStartElement()) {
                final String eventNameLocalPart = e.asStartElement().getName().getLocalPart();
                if (extractedKeys.contains(eventNameLocalPart)) {
                    extractedName = eventNameLocalPart;
                }
                depth++;
            }
            if (extractedName != null && e.isCharacters()) {
                final String extractedValue = extractedValues.get(extractedName);
                final String eventCharacterData = e.asCharacters().getData();
                if (extractedValue == null) {
                    extractedValues.put(extractedName, eventCharacterData);
                } else {
                    extractedValues.put(extractedName, extractedValue + eventCharacterData);
                }
            }
            if (e.isEndElement()) {
                depth--;
                extractedName = null;
            }
            xmlWriter.add(e);
        } while (depth > 0 || isNextEventDifferentFromStartElementAndEndElement());
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

    private RuntimeException asRuntimeException(XMLStreamException e) throws UnrecoverableDataException, PrematureEndOfDataException {
        final Throwable cause = e.getNestedException();
        if (cause != null && cause instanceof IOException) {
            return new PrematureEndOfDataException(cause);
        }
        return new InvalidDataException(e);
    }
}
