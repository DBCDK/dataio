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
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.dataio.marc.reader.MarcReaderException;
import dk.dbc.dataio.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.dataio.marc.writer.MarcWriter;
import dk.dbc.dataio.marc.writer.MarcWriterException;
import dk.dbc.dataio.marc.writer.MarcXchangeV1Writer;
import dk.dbc.marc.DanMarc2Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.dataio.commons.types.ChunkItem.Status;
import static dk.dbc.dataio.commons.types.ChunkItem.Type;

public class DanMarc2LineFormatDataPartitionerFactory implements DataPartitionerFactory {

    /**
     * Creates new instance of DanMarc2 LineFormat DataPartitioner
     *
     * @param inputStream       stream from which data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of DanMarc2 LineFormat DataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    @Override
    public DataPartitioner createDataPartitioner(InputStream inputStream, String specifiedEncoding) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        return new DanMarc2LineFormatDataPartitioner(inputStream, specifiedEncoding);
    }

    private static class DanMarc2LineFormatDataPartitioner implements DataPartitioner {
        private static final Logger LOGGER = LoggerFactory.getLogger(DanMarc2LineFormatDataPartitioner.class);

        private final ByteCountingInputStream inputStream;
        private final MarcXchangeV1Writer marcWriter;
        private final DanMarc2LineFormatReader marcReader;
        private String specifiedEncoding;
        private Charset encoding;

        private Iterator<ChunkItem> iterator;
        private DanMarc2Charset danMarc2Charset;

        public DanMarc2LineFormatDataPartitioner(InputStream inputStream, String specifiedEncoding) {
            this.inputStream = new ByteCountingInputStream(inputStream);
            this.encoding = StandardCharsets.UTF_8;
            this.specifiedEncoding = specifiedEncoding;
            this.danMarc2Charset = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
            validateSpecifiedEncoding();
            marcWriter = new MarcXchangeV1Writer();
            marcReader = new DanMarc2LineFormatReader(this.inputStream, danMarc2Charset);
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
        public Iterator<ChunkItem> iterator() {
            iterator = new Iterator<ChunkItem>() {
                @Override
                public boolean hasNext() {
                    try {
                        return marcReader.hasNext();
                    } catch (MarcReaderException e) {
                        throw new InvalidDataException(e);
                    }
                }

                @Override
                public ChunkItem next() {
                    try {
                        return processMarcReaderReadResult(marcReader.read(), marcWriter);
                    } catch (MarcReaderException e) {
                        LOGGER.error("Exception caught while creating MarcRecord", e);
                        if (e instanceof MarcReaderInvalidRecordException) {
                            ChunkItem chunkItem = buildChunkItem(0, ((MarcReaderInvalidRecordException) e).getBytesRead(), Status.FAILURE, Type.STRING, encoding);
                            chunkItem.appendDiagnostics(buildDiagnostic(e.getMessage()));
                            return chunkItem;
                        } else {
                            throw new InvalidDataException(e);
                        }
                    }
                }
            };
            return iterator;
        }


        /*
         * Private methods
         */


        /**
         * Process the result of the marcReader.read() method.
         * If the marcRecord contains no fields (special case), a new chunk item with status IGNORE is returned
         * If the marcRecord can be written, a new chunk item with status SUCCESS is returned
         * If an error occours while writing the record, a new chunk item with status FAILURE is returned
         * @param marcRecord the marcRecord result from the marcReader.read() method
         * @param marcWriter the marcWriter used to write the marcRecord
         * @return chunkItem
         */
        private ChunkItem processMarcReaderReadResult(MarcRecord marcRecord, MarcWriter marcWriter) {
            ChunkItem chunkItem;
            try {
                if(marcRecord.getFields().isEmpty()) {
                    chunkItem = buildChunkItem(0, "Empty Record".getBytes(encoding), Status.IGNORE, Type.STRING, encoding);
                } else {
                    byte[] marcRecordAsByteArray = marcWriter.write(marcRecord, encoding);
                    chunkItem = buildChunkItem(0, marcRecordAsByteArray, Status.SUCCESS, Type.MARCXCHANGE, encoding);
                }
            } catch (MarcWriterException e) {
                LOGGER.error("Exception caught while writing MarcRecord", e);
                chunkItem = buildChunkItem(0, marcRecord.toString().getBytes(encoding), Status.FAILURE, Type.STRING, encoding);
                chunkItem.appendDiagnostics(buildDiagnostic(e.getMessage()));
            }
            return chunkItem;
        }

        /**
         * Builds a new ChunkItem from the input values
         * @param id of the ChunkItem
         * @param data to set on the ChunkItem
         * @param status of the ChunkItem
         * @param type of the data
         * @param encoding of the data
         * @return chunkItem
         */
        private ChunkItem buildChunkItem(long id, byte[] data, Status status, Type type, Charset encoding) {
            return new ChunkItem(id, data, status, new ArrayList<>(Collections.singletonList(type)), encoding.name());
        }

        /**
         * Creates a new Diagnostic with Level FATAL and with input String as message
         * @param message the input message
         * @return diagnostic
         */
        private Diagnostic buildDiagnostic(String message) {
            return new Diagnostic(Diagnostic.Level.FATAL, message);
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
    }

}
