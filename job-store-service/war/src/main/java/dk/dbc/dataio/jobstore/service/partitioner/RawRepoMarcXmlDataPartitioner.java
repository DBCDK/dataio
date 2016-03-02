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
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;


public class RawRepoMarcXmlDataPartitioner extends DefaultXmlDataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoMarcXmlDataPartitioner.class);
    private static final String TRACKING_ID = "trackingId";
    private final MarcRecordInfoBuilder marcRecordInfoBuilder;

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
        marcRecordInfoBuilder = new MarcRecordInfoBuilder();
    }

    @Override
    protected DataPartitionerResult nextDataPartitionerResult(ByteArrayOutputStream baos) throws InvalidDataException {
        DataPartitionerResult result;
        final String trackingId = extractedValues.get(TRACKING_ID);
        try {
            final MarcXchangeV1Reader marcXchangeV1Reader = new MarcXchangeV1Reader(getInputStream(baos.toByteArray()), getEncoding());
            final MarcRecord marcRecord = marcXchangeV1Reader.read();
            if(marcRecord == null) {
                throw new InvalidDataException("Marc Record was null");
            } else {
                final Optional<MarcRecordInfo> recordInfo = marcRecordInfoBuilder.parse(marcRecord);
                final ChunkItem chunkItem = new ChunkItem(0, baos.toByteArray(), ChunkItem.Status.SUCCESS,
                        Arrays.asList(ChunkItem.Type.DATACONTAINER, ChunkItem.Type.MARCXCHANGE), StandardCharsets.UTF_8);

                chunkItem.setTrackingId(trackingId);
                result = new DataPartitionerResult(chunkItem, recordInfo.orElse(null));
            }
        } catch (MarcReaderException e) {
            LOGGER.error("Exception caught while creating MarcRecord", e);
            if (e instanceof MarcReaderInvalidRecordException) {
                final ChunkItem chunkItem = ObjectFactory.buildFailedChunkItem(0, ((MarcReaderInvalidRecordException) e).getBytesRead(), ChunkItem.Type.STRING, trackingId);
                chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(e.getMessage()));
                result = new DataPartitionerResult(chunkItem, null);
            } else {
                throw new InvalidDataException(e);
            }
        }
        return result;
    }

    private MarcRecord getMarcRecord(ByteArrayOutputStream baos) throws MarcReaderException {
        MarcXchangeV1Reader marcReader = new MarcXchangeV1Reader(getInputStream(baos.toByteArray()), getEncoding());
        return marcReader.read();
    }

    private BufferedInputStream getInputStream(byte[] data) {
        return new BufferedInputStream(new ByteArrayInputStream(data));
    }
}


