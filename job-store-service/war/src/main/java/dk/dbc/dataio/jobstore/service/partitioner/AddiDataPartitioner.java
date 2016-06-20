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

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.util.EncodingsUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Optional;

public abstract class AddiDataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddiDataPartitioner.class);

    private final ByteCountingInputStream inputStream;
    private final AddiReader addiReader;
    private final JSONBContext jsonbContext;
    private final Charset encoding;

    /**
     * Super class constructor
     * @param inputStream stream from which addi records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument or if given stream is incompatible with AddiReader
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     */
    public AddiDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encodingName, "specifiedEncoding");
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.addiReader = new AddiReader(this.inputStream);
        this.jsonbContext = new JSONBContext();
        this.encoding = EncodingsUtil.toEncoding(encodingName);
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

    protected abstract ChunkItem.Type getChunkItemType();

    private boolean hasNextDataPartitionerResult() throws InvalidDataException {
        try {
            return addiReader.hasNext();
        } catch (IOException e) {
            throw new InvalidDataException(e);
        }
    }

    private DataPartitionerResult nextDataPartitionerResult() throws InvalidDataException {
        DataPartitionerResult result;
        try {
            final AddiRecord addiRecord = addiReader.next();
            if (addiRecord == null) {
                result = DataPartitionerResult.EMPTY;
            } else {
                result = processAddiRecord(addiRecord);
            }
        } catch (IOException e) {
            LOGGER.error("Exception caught while creating AddiRecord", e);
            throw new InvalidDataException(e);
        }
        return result;
    }

    private DataPartitionerResult processAddiRecord(AddiRecord addiRecord) {
        ChunkItem chunkItem;
        Optional<RecordInfo> recordInfo = Optional.empty();
        try {
            if (addiRecord.getMetaData().length == 0 && addiRecord.getContentData().length == 0) {
                chunkItem = ChunkItem.ignoredChunkItem()
                    .withData("Empty Record")
                    .withType(ChunkItem.Type.STRING);
            } else {
                final AddiMetaData addiMetaData = getAddiMetaData(addiRecord);
                chunkItem = ChunkItem.successfulChunkItem()
                    .withTrackingId(addiMetaData.trackingId().orElse(null))
                    .withData(addiRecord.getContentData())
                    .withEncoding(encoding)
                    .withType(getChunkItemType());
                recordInfo = getRecordInfo(addiMetaData, addiRecord.getContentData());
            }
        } catch (JSONBException | RuntimeException e) {
            LOGGER.error("Exception caught while processing AddiRecord", e);
            chunkItem = ChunkItem.failedChunkItem()
                .withData(addiRecord.getBytes())
                .withEncoding(encoding)
                .withType(ChunkItem.Type.BYTES)
                .withDiagnostics(ObjectFactory.buildFatalDiagnostic(e.getMessage()));
        }
        return new DataPartitionerResult(chunkItem, recordInfo.orElse(null));
    }

    private AddiMetaData getAddiMetaData(AddiRecord addiRecord) throws JSONBException {
        return jsonbContext.unmarshall(
                new String(addiRecord.getMetaData(), encoding), AddiMetaData.class);
    }

    protected Optional<RecordInfo> getRecordInfo(AddiMetaData addiMetaData, byte[] content) {
        return Optional.of(new RecordInfo(addiMetaData.bibliographicRecordId().orElse(null)));
    }
}
