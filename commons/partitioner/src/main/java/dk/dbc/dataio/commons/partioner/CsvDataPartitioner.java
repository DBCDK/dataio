package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.encoding.CharacterEncodingScheme;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.invariant.InvariantUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Data partitioner "default" CSV format
 * withDelimiter(',')
 * withQuote('"')
 * withRecordSeparator("\r\n")
 * withIgnoreEmptyLines(true)
 */
public class CsvDataPartitioner implements DataPartitioner {
    /*  Experimented with both commons-csv, super-csv and opencsv libraries.
        None of them are able to cope with erroneous CSV in the form of unbalanced double quotes in a way that allows us
        to step over the faulty record and continue partitioning. Therefore this partitioner is designed to do its own
        line reading and treat each line as a separate CSV document. Consequently this partitioner can not handle the
        case where CRLF are embedded within double quotes "...CRLF..." as described in
        https://tools.ietf.org/html/rfc4180.
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvDataPartitioner.class);

    final ByteCountingInputStream inputStream;
    final XMLEventFactory xmlEventFactory;
    final XMLOutputFactory xmlOutputFactory;
    final BufferedReader reader;

    int positionInDatafile = 0;

    CSVFormat csvFormat = CSVFormat.DEFAULT;

    /**
     * Creates new instance of DataPartitioner for CSV data
     *
     * @param inputStream  stream from which csv records can be read
     * @param encodingName encoding specified in job specification
     * @return new instance of CsvDataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     */
    public static CsvDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new CsvDataPartitioner(inputStream, encodingName);
    }

    CsvDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        this.inputStream = new ByteCountingInputStream(
                InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream"));
        final Charset inputEncoding = CharacterEncodingScheme.charsetOf(
                InvariantUtil.checkNotNullNotEmptyOrThrow(encodingName, "encodingName"));

        this.reader = new BufferedReader(new InputStreamReader(this.inputStream, inputEncoding));
        xmlEventFactory = XMLEventFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    @Override
    public Charset getEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return hasNextDataPartitionerResult();
            }

            @Override
            public DataPartitionerResult next() {
                return nextDataPartitionerResult();
            }
        };
    }

    boolean hasNextDataPartitionerResult() {
        try {
            reader.mark(1);
            final int readResult = reader.read();
            reader.reset();
            return readResult > -1;
        } catch (Exception e) {
            // let nextDataPartitionerResult() handle the problem
            return true;
        }
    }

    DataPartitionerResult nextDataPartitionerResult() {
        DataPartitionerResult result;
        try {
            result = getResultFromCsvRecord(reader.readLine());
        } catch (IOException e) {
            LOGGER.error("exception caught while reading CSV record", e);
            throw new PrematureEndOfDataException(e);
        }
        positionInDatafile++;
        return result;
    }

    DataPartitionerResult getResultFromCsvRecord(String csvLine) {
        if (csvLine == null || csvLine.isEmpty()) {
            return DataPartitionerResult.EMPTY;
        }

        CSVRecord csvRecord;
        try {
            csvRecord = parseLine(csvLine);
        } catch (RuntimeException e) {
            return new DataPartitionerResult(ChunkItem.failedChunkItem()
                    .withData(csvLine)
                    .withType(ChunkItem.Type.STRING)
                    .withEncoding(StandardCharsets.UTF_8)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.ERROR, "Illegal CSV", e)),
                    null, positionInDatafile);
        }

        try {
            final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                    .withData(toXml(csvRecord).toByteArray())
                    .withType(ChunkItem.Type.GENERICXML)
                    .withEncoding(StandardCharsets.UTF_8);
            return new DataPartitionerResult(chunkItem, null, positionInDatafile);
        } catch (XMLStreamException e) {
            return new DataPartitionerResult(ChunkItem.failedChunkItem()
                    .withData(csvLine)
                    .withType(ChunkItem.Type.STRING)
                    .withEncoding(StandardCharsets.UTF_8)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.ERROR, "Failed to convert CSV to XML", e)),
                    null, positionInDatafile);
        }
    }

    CSVRecord parseLine(String csvLine) {
        try (CSVParser parser = CSVParser.parse(csvLine, csvFormat)) {
            return parser.getRecords().get(0);
        } catch (IOException e) {
            throw new InvalidDataException("Failed to parse CSV", e);
        }
    }

    ByteArrayOutputStream toXml(CSVRecord csvRecord) throws XMLStreamException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Writer writer = new BufferedWriter(new OutputStreamWriter(baos, getEncoding()));
        final XMLEventWriter xmlWriter = xmlOutputFactory.createXMLEventWriter(writer);
        xmlWriter.add(newStartElementEvent("csv"));
        xmlWriter.add(newStartElementEvent("line"));
        int fieldPos = 0;
        for (String field : csvRecord) {
            final String fieldName = "C" + fieldPos++;
            xmlWriter.add(newStartElementEvent(fieldName));
            xmlWriter.add(newCharactersEvent(field));
            xmlWriter.add(newEndElementEvent(fieldName));
        }
        xmlWriter.add(newEndElementEvent("line"));
        xmlWriter.add(newEndElementEvent("csv"));
        xmlWriter.close();
        return baos;
    }

    StartElement newStartElementEvent(String localName) {
        return xmlEventFactory.createStartElement("", "", localName);
    }

    EndElement newEndElementEvent(String localName) {
        return xmlEventFactory.createEndElement("", "", localName);
    }

    Characters newCharactersEvent(String value) {
        return xmlEventFactory.createCharacters(value);
    }
}
