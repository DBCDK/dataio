/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.CharacterEncodingScheme;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
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
 * Data partitioner for "Den Store Danske" encyclopedia CSV format
 */
public class DsdCsvDataPartitioner implements DataPartitioner {
    /*  Experimented with both commons-csv, super-csv and opencsv libraries.
        None of them are able to cope with erroneous CSV in the form of unbalanced double quotes in a way that allows us
        to step over the faulty record and continue partitioning. Therefore this partitioner is designed to do its own
        line reading and treat each line as a separate CSV document. Consequently this partitioner can not handle the
        case where CRLF are embedded within double quotes "...CRLF..." as described in
        https://tools.ietf.org/html/rfc4180.
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(DsdCsvDataPartitioner.class);

    private final ByteCountingInputStream inputStream;
    private final XMLEventFactory xmlEventFactory;
    private final XMLOutputFactory xmlOutputFactory;
    private final BufferedReader reader;

    private int positionInDatafile = 0;

    /**
     * Creates new instance of DataPartitioner for CSV data
     * @param inputStream stream from which csv records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of CsvDataPartitioner
     */
    public static DsdCsvDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new DsdCsvDataPartitioner(inputStream, encodingName);
    }

    DsdCsvDataPartitioner(InputStream inputStream, String encodingName)
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
        return new Iterator<DataPartitionerResult>() {
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

    private boolean hasNextDataPartitionerResult() {
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

    private DataPartitionerResult nextDataPartitionerResult() {
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

    private DataPartitionerResult getResultFromCsvRecord(String csvLine) {
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

    private CSVRecord parseLine(String csvLine) {
        try (CSVParser parser = CSVParser.parse(csvLine, CSVFormat.DEFAULT)) {
            return parser.getRecords().get(0);
        } catch (IOException e) {
            throw new InvalidDataException("Failed to parse CSV", e);
        }
    }

    private ByteArrayOutputStream toXml(CSVRecord csvRecord) throws XMLStreamException {
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

    private StartElement newStartElementEvent(String localName) {
        return xmlEventFactory.createStartElement("", "", localName);
    }

    private EndElement newEndElementEvent(String localName) {
        return xmlEventFactory.createEndElement("", "", localName);
    }

    private Characters newCharactersEvent(String value) {
        return xmlEventFactory.createCharacters(Jsoup.parse(value).text());
    }
}
