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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Iterator;
import dk.dbc.marc.Iso2709IteratorReadError;
import dk.dbc.marc.Iso2709Unpacker;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

public class Iso2709DataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Iso2709DataPartitioner.class);

    private final Iso2709Iterator inputStream;
    private final MarcXchangeV1Writer marcWriter;
    private final MarcRecordInfoBuilder marcRecordInfoBuilder;

    private Charset encoding;
    private String specifiedEncoding;

    private DanMarc2Charset danMarc2Charset;
    private DocumentBuilderFactory documentBuilderFactory;

    /**
     * Creates new instance of default Iso2709 DataPartitioner
     * @param inputStream stream from which Iso2709 data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of default Iso2709 DataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    public static Iso2709DataPartitioner newInstance(InputStream inputStream, String specifiedEncoding) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        return new Iso2709DataPartitioner(inputStream, specifiedEncoding);
    }

    protected Iso2709DataPartitioner(InputStream inputStream, String specifiedEncoding) {
        BufferedInputStream bufferedInputStream = getInputStreamAsBufferedInputStream(inputStream);
        this.inputStream = new Iso2709Iterator(bufferedInputStream);
        this.encoding = StandardCharsets.UTF_8;
        this.specifiedEncoding = specifiedEncoding;
        validateSpecifiedEncoding();
        danMarc2Charset = new DanMarc2Charset();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        marcWriter = new MarcXchangeV1Writer();
        marcRecordInfoBuilder = new MarcRecordInfoBuilder();

    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return encoding;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getTotalBytesRead();
    }

    @Override
    public Iterator<ChunkItem> iterator() throws UnrecoverableDataException {
        return new Iterator<ChunkItem>() {
            @Override
            public boolean hasNext() {
                return hasNextDataPartitionerResult();
            }

            @Override
            public ChunkItem next() {
                return nextDataPartitionerResult().getChunkItem();
            }
        };
    }

    /*
     * Protected methods
     */

    protected boolean hasNextDataPartitionerResult() {
        return inputStream.hasNext();
    }

    protected DataPartitionerResult nextDataPartitionerResult() throws InvalidDataException {
        DataPartitionerResult result;
        final byte[] recordAsBytes = inputStream.next();
        try {
            final MarcRecord marcRecord = getMarcRecord(recordAsBytes);
            if (marcRecord == null) {
                result = DataPartitionerResult.EMPTY;
            } else {
                result = processMarcRecord(marcRecord, marcWriter);
            }
        } catch (MarcReaderException e) {
            LOGGER.error("Exception caught while creating MarcRecord", e);
            if (e instanceof MarcReaderInvalidRecordException) {
                ChunkItem chunkItem = ObjectFactory.buildFailedChunkItem(0, ((MarcReaderInvalidRecordException) e).getBytesRead());
                chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(e.getMessage()));
                return new DataPartitionerResult(chunkItem, null);
            } else {
                throw new InvalidDataException(e);
            }
        } catch(Iso2709IteratorReadError e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("Exception caught while decoding 2709", e);
            ChunkItem chunkItem = ObjectFactory.buildFailedChunkItem(0, recordAsBytes);
            chunkItem.appendDiagnostics(new Diagnostic(Diagnostic.Level.FATAL,"Exception caught while decoding 2709", e ));
            result = new DataPartitionerResult(chunkItem, null);
            return result;
        }
        return result;
    }

    /*
     * Private methods
     */

    private BufferedInputStream getInputStream(byte[] data) {
        return new BufferedInputStream(new ByteArrayInputStream(data));
    }

    /**
     * This method verifies if the specified encoding is latin1
     */
    private void validateSpecifiedEncoding()  {
        if(!EncodingsUtil.isEquivalent(specifiedEncoding, "latin1")) {
            throw new InvalidEncodingException(String.format(
                    "Specified encoding not supported: '%s' ", specifiedEncoding));
        }
    }

    /**
     * Process the MarcRecord obtained from the input stream
     * If the marcRecord contains no fields (special case), a result with a chunk item with status IGNORE is returned
     * If the marcRecord can be written, a result containing chunk item with status SUCCESS and record info is returned
     * If an error occurs while writing the record, a result with a chunk item with status FAILURE is returned
     * @param marcRecord the MarcRecord result from the marcReader.read() method
     * @param marcWriter the MarcWriter implementation used to write the marc record
     * @return data partitioner result
     */
    private DataPartitionerResult processMarcRecord(MarcRecord marcRecord, MarcWriter marcWriter) {
        ChunkItem chunkItem;
        Optional<MarcRecordInfo> recordInfo = Optional.empty();
        try {
            if (marcRecord.getFields().isEmpty()) {
                chunkItem = ObjectFactory.buildIgnoredChunkItem(0, "Empty Record");
            } else {
                chunkItem = ObjectFactory.buildSuccessfulChunkItem(0, marcWriter.write(marcRecord, encoding), ChunkItem.Type.MARCXCHANGE);
                recordInfo = marcRecordInfoBuilder.parse(marcRecord);
            }
        } catch (MarcWriterException e) {
            LOGGER.error("Exception caught while processing MarcRecord", e);
            chunkItem = ObjectFactory.buildFailedChunkItem(0, marcRecord.toString());
            chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(e.getMessage()));
        }
        return new DataPartitionerResult(chunkItem, recordInfo.orElse(null));
    }

    /**
     * This method converts a byte array representation of the record into a document representation of the record into a marc record.
     * @param recordAsBytes byte array representation of the record
     * @return marc record or null if no document could be created
     * @throws MarcReaderException if an error occurs while creating parser
     * @throws InvalidDataException if:
     * A documentBuilder cannot be created which satisfies the configuration requested.
     * It was not possible to create a Transformer instance or if an unrecoverable error occurs during the course of the transformation.
     */
    private MarcRecord getMarcRecord(byte[] recordAsBytes) throws MarcReaderException, InvalidDataException {
        try {
            Document marcXChangeRecordAsDocument = Iso2709Unpacker.createMarcXChangeRecord(recordAsBytes, danMarc2Charset, documentBuilderFactory);
            if (marcXChangeRecordAsDocument != null) {
                MarcXchangeV1Reader marcReader = new MarcXchangeV1Reader(getInputStream(documentToString(marcXChangeRecordAsDocument).getBytes(encoding)), encoding);
                return marcReader.read();
            }
            return null;
        } catch (ParserConfigurationException e) {
            LOGGER.error("Exception caught while creating MarcXChange Record", e);
            throw new InvalidDataException(e);
        } catch (TransformerException e) {
            LOGGER.error("Unrecoverable error occurred during transformation", e);
            throw new InvalidDataException(e);
        }
    }

    /**
     *
     * This method converts a document to a String
     * @param document the document to convert
     * @return String representation of the document
     *
     * @throws TransformerException if it was not possible to create a Transformer instance or
     *         if an unrecoverable error occurs during the course of the transformation.
     */
    private String documentToString(Document document) throws TransformerException {
        DOMSource domSource = new DOMSource(document);
        StreamResult result = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(domSource, result);
        return result.getWriter().toString();
    }


    private static BufferedInputStream getInputStreamAsBufferedInputStream(InputStream inputStream) {
        if( inputStream instanceof BufferedInputStream ) return (BufferedInputStream) inputStream;
        return new BufferedInputStream(inputStream);
    }
}
