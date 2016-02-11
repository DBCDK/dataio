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
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static dk.dbc.dataio.commons.types.ChunkItem.Type.DATACONTAINER;
import static dk.dbc.dataio.commons.types.ChunkItem.Type.MARCXCHANGE;


public class RawRepoMarcXmlDataPartitioner extends DefaultXmlDataPartitioner {

    private final static String TRACKING_ID = "trackingId";

    /**
     * Creates new instance of rawRepoMarcXmlDataPartitioner
     * @param inputStream stream from which XML data to be partitioned can be read
     * @param encoding encoding of XML data to be partitioned
     * @return new instance of rawRepoMarcXmlDataPartitioner
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    public static RawRepoMarcXmlDataPartitioner newInstance(InputStream inputStream, String encoding) throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encoding, "encoding");
        return new RawRepoMarcXmlDataPartitioner(inputStream, encoding);
    }

    private RawRepoMarcXmlDataPartitioner(InputStream inputStream, String expectedEncoding) {
        super(inputStream, expectedEncoding);
        extractedKeys.add(TRACKING_ID);
    }

    @Override
    protected ChunkItem nextChunkItem(ByteArrayOutputStream baos, ChunkItem.Status status) {
        ChunkItem chunkItem = new ChunkItem(0, baos.toByteArray(), status, Arrays.asList(DATACONTAINER, MARCXCHANGE), StandardCharsets.UTF_8);
        chunkItem.setTrackingId(extractedValues.get(TRACKING_ID));
        return chunkItem;
    }
}


