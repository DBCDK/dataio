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

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A partitioner of Addi records read from an {@link InputStream} containing MarcXchange documents from RawRepo
 * <pre>
 * {@code
 *
 * final DataPartitioner dataPartitioner = MarcXchangeAddiDataPartitioner.newInstance(inputStream, encoding);
 * for(DataPartitionerResult recordWrapper : dataPartitioner) {
 *     // do something with record.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the MarcXchangeAddiDataPartitioner.newInstance() method returns
 * a {@link DataPartitioner}, enabling you to step through the results one at a time.
 * Also note, that if a fatal error occurs while reading the input stream, a {@link UnrecoverableDataException} or
 * sub type thereof is thrown. {@link UnrecoverableDataException} is a {@link RuntimeException} since the
 * {@link Iterable} interface all DataPartitioner implementations must implement does not allow checked exceptions
 * to be thrown.
 */
public class MarcXchangeAddiDataPartitioner extends AddiDataPartitioner {
    /**
     * Creates new instance of DataPartitioner for Addi records containing marcXchange content
     * @param inputStream stream from which addi records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument or if given stream is incompatible with AddiReader
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of MarcXchangeAddiDataPartitioner
     */
    public static MarcXchangeAddiDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new MarcXchangeAddiDataPartitioner(inputStream, encodingName);
    }

    private MarcXchangeAddiDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
    }

    @Override
    protected ChunkItem.Type getChunkItemType() {
        return ChunkItem.Type.MARCXCHANGE;
    }

    @Override
    protected Optional<RecordInfo> getRecordInfo(AddiMetaData addiMetaData, byte[] content) {
        final MarcXchangeV1Reader marcReader;
        try {
            marcReader = new MarcXchangeV1Reader(getInputStream(content), StandardCharsets.UTF_8);
            final MarcRecordInfoBuilder marcRecordInfoBuilder = new MarcRecordInfoBuilder();
            Optional<MarcRecordInfo> marcRecordInfo = marcRecordInfoBuilder.parse(marcReader.read());
            return Optional.of(marcRecordInfo.get());
        } catch (MarcReaderException e) {
            throw new IllegalArgumentException("Marc record info could not be created. ", e);
        }
    }

    private BufferedInputStream getInputStream(byte[] data) {
        return new BufferedInputStream(new ByteArrayInputStream(data));
    }

}
