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
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.CharacterEncodingScheme;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

public class DanMarc2LineFormatDataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanMarc2LineFormatDataPartitioner.class);

    protected final DanMarc2LineFormatReader marcReader;
    protected int positionInDatafile = 0;

    private final ByteCountingInputStream inputStream;
    private final MarcXchangeV1Writer marcWriter;
    private final MarcRecordInfoBuilder marcRecordInfoBuilder;

    private String specifiedEncoding;
    private Charset encoding;
    private DanMarc2Charset danMarc2Charset;

    /**
     * Creates new instance of DanMarc2 LineFormat DataPartitioner
     * @param inputStream stream from which data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of DanMarc2 LineFormat DataPartitioner
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws PrematureEndOfDataException if unable to read sufficient data from inputStream
     */
    public static DanMarc2LineFormatDataPartitioner newInstance(InputStream inputStream, String specifiedEncoding)
            throws NullPointerException, IllegalArgumentException, PrematureEndOfDataException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        return new DanMarc2LineFormatDataPartitioner(inputStream, specifiedEncoding);
    }

    protected DanMarc2LineFormatDataPartitioner(InputStream inputStream, String specifiedEncoding) throws PrematureEndOfDataException {
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.encoding = StandardCharsets.UTF_8;
        this.specifiedEncoding = specifiedEncoding;
        this.danMarc2Charset = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
        validateSpecifiedEncoding();
        marcWriter = new MarcXchangeV1Writer();
        try {
            marcReader = new DanMarc2LineFormatReader(this.inputStream, danMarc2Charset);
        } catch (IllegalStateException e) {
            throw new PrematureEndOfDataException(e.getCause());
        }
        marcRecordInfoBuilder = new MarcRecordInfoBuilder();
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return encoding;
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

    protected boolean hasNextDataPartitionerResult() throws PrematureEndOfDataException {
        try {
            return marcReader.hasNext();
        } catch (MarcReaderException e) {
            throw new PrematureEndOfDataException(e);
        }
    }

    protected DataPartitionerResult nextDataPartitionerResult() throws PrematureEndOfDataException {
        DataPartitionerResult result;
        try {
            final MarcRecord marcRecord = marcReader.read();
            if (marcRecord == null) {
                result = DataPartitionerResult.EMPTY;
            } else {
                result = processMarcRecord(marcRecord, marcWriter);
            }
        } catch (MarcReaderException e) {
            if (e instanceof MarcReaderInvalidRecordException) {
                result = new DataPartitionerResult(
                        ChunkItem.failedChunkItem()
                                .withType(ChunkItem.Type.BYTES)
                                .withData(((MarcReaderInvalidRecordException) e).getBytesRead())
                                .withDiagnostics(new Diagnostic(
                                        Diagnostic.Level.ERROR, e.getMessage(), e)),
                        null, positionInDatafile++);
            } else {
                throw new PrematureEndOfDataException(e);
            }
        }
        return result;
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
                chunkItem = ChunkItem.ignoredChunkItem()
                        .withData("Empty Record");
            } else {
                chunkItem = ChunkItem.successfulChunkItem()
                        .withType(ChunkItem.Type.MARCXCHANGE)
                        .withData(marcWriter.write(marcRecord, encoding));
                recordInfo = marcRecordInfoBuilder.parse(marcRecord);
            }
        } catch (MarcWriterException e) {
            LOGGER.error("Exception caught while processing MarcRecord", e);
            chunkItem = ChunkItem.failedChunkItem()
                    .withType(ChunkItem.Type.STRING)
                    .withData(marcRecord.toString())
                    .withDiagnostics(new Diagnostic(
                            Diagnostic.Level.FATAL, e.getMessage(), e));
        }
        return new DataPartitionerResult(chunkItem, recordInfo.orElse(null), positionInDatafile++);
    }

    /**
     * This method verifies if the specified encoding is latin1
     */
    private void validateSpecifiedEncoding()  {
        if(!StandardCharsets.ISO_8859_1.name().equals(CharacterEncodingScheme.charsetOf(specifiedEncoding).name())) {
            throw new InvalidEncodingException(String.format(
                    "Specified encoding not supported: '%s' ", specifiedEncoding));
        }
    }
}
