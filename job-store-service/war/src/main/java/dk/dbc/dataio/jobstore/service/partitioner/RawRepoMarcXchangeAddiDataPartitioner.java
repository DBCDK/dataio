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
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;

import java.io.InputStream;

/**
 * A partitioner of Addi records read from an {@link InputStream} containing MarcXchange documents from RawRepo
 * <pre>
 * {@code
 *
 * final DataPartitioner dataPartitioner = RawRepoMarcXchangeAddiDataPartitioner.newInstance(inputStream, encoding);
 * for(DataPartitionerResult recordWrapper : dataPartitioner) {
 *     // do something with record.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the RawRepoMarcXchangeAddiDataPartitioner.newInstance() method returns
 * a {@link DataPartitioner}, enabling you to step through the results one at a time.
 * Also note, that if a fatal error occurs while reading the input stream, a {@link UnrecoverableDataException} or
 * sub type thereof is thrown. {@link UnrecoverableDataException} is a {@link RuntimeException} since the
 * {@link Iterable} interface all DataPartitioner implementations must implement does not allow checked exceptions
 * to be thrown.
 */
public class RawRepoMarcXchangeAddiDataPartitioner extends AddiDataPartitioner {
    /**
     * Creates new instance of DataPartitioner for Addi records containing marcXchange content
     * @param inputStream stream from which addi records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument or if given stream is incompatible with AddiReader
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of RawRepoMarcXchangeAddiDataPartitioner
     */
    public static RawRepoMarcXchangeAddiDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new RawRepoMarcXchangeAddiDataPartitioner(inputStream, encodingName);
    }

    private RawRepoMarcXchangeAddiDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
    }

    @Override
    protected ChunkItem.Type getChunkItemType() {
        return ChunkItem.Type.MARCXCHANGE;
    }
}
