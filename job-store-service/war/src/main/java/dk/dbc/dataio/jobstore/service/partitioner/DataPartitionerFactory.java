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

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Factory interface for creation of instances of DataPartitioner
 */
public interface DataPartitionerFactory {
    /**
     * Creates new DataPartitioner for given input data
     * @param inputStream stream from which data to be partitioned can be read
     * @param encoding encoding of data to be partitioned
     * @return DataPartitioner instance
     */
    DataPartitioner createDataPartitioner(InputStream inputStream, String encoding);

    interface DataPartitioner extends Iterable<String> {
        Charset getEncoding() throws InvalidEncodingException;
        long getBytesRead();
    }
}
