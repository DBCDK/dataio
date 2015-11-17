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
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.dataio.marc.reader.MarcReader;
import dk.dbc.dataio.marc.reader.MarcReaderException;
import dk.dbc.dataio.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.dataio.marc.writer.MarcWriter;
import dk.dbc.dataio.marc.writer.MarcXchangeV11Writer;
import dk.dbc.marc.DanMarc2Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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
        return new Dm2LineFormatDataPartitioner(inputStream, specifiedEncoding);
    }

    private static class Dm2LineFormatDataPartitioner implements DataPartitioner {
        private static final Logger LOGGER = LoggerFactory.getLogger(Dm2LineFormatDataPartitioner.class);

        private final ByteCountingInputStream inputStream;
        private String specifiedEncoding;
        private Charset encoding;

        private Iterator<ChunkItem> iterator;
        private DanMarc2Charset danMarc2Charset;
        private BufferedInputStream bufferedInputStream;

        public Dm2LineFormatDataPartitioner(InputStream inputStream, String specifiedEncoding) {
            this.inputStream = new ByteCountingInputStream(inputStream);
            this.encoding = StandardCharsets.UTF_8;
            this.specifiedEncoding = specifiedEncoding;
            this.danMarc2Charset = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
            validateSpecifiedEncoding();
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
            if (iterator == null) {
                bufferedInputStream = new BufferedInputStream(inputStream);
            }

            iterator = new Iterator<ChunkItem>() {
                private MarcRecord marcRecord = null;
                private MarcWriter marcWriter = new MarcXchangeV11Writer();
                private MarcReader marcReader = new DanMarc2LineFormatReader(bufferedInputStream, danMarc2Charset);

                @Override
                public boolean hasNext() {
                    try {
                        bufferedInputStream.mark(1);
                        int readResult = bufferedInputStream.read();
                        bufferedInputStream.reset();
                        return readResult > -1;
                    } catch (IOException e) {
                        LOGGER.error("Exception caught while reading input stream", e);
                        throw new InvalidDataException(e);
                    }
                }

                @Override
                public ChunkItem next() {
                    try {
                        marcRecord = marcReader.read();
                        if (marcRecord != null) {
                            byte[] marcRecordAsByteArray = marcWriter.write(marcRecord, encoding);
                            return new ChunkItem(0, marcRecordAsByteArray, ChunkItem.Status.SUCCESS);
                        }
                    } catch (MarcReaderException e) {
                        LOGGER.error("Exception caught while creating MarcRecord", e);
                        if(e instanceof MarcReaderInvalidRecordException) {
                            return new ChunkItem(0, ((MarcReaderInvalidRecordException) e).getLinesRead(), ChunkItem.Status.FAILURE);
                        } else {
                            throw new InvalidDataException(e);
                        }
                    }
                    return null;
                }
            };
            return iterator;
        }


        /*
         * Private methods
         */

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
