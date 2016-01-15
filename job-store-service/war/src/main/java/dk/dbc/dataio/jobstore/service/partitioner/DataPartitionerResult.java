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
import dk.dbc.dataio.jobstore.types.RecordInfo;

import java.util.Optional;

/**
 * This class encapsulates a data partitioner result containing the actual chunk item as
 * well as record meta data
 */
public class DataPartitionerResult {
    private final ChunkItem chunkItem;
    private final RecordInfo recordInfo;

    public DataPartitionerResult(ChunkItem chunkItem, RecordInfo recordInfo) {
        this.chunkItem = chunkItem;
        this.recordInfo = recordInfo;
    }

    public Optional<ChunkItem> getChunkItem() {
        return chunkItem != null ? Optional.of(chunkItem) : Optional.empty();
    }

    public Optional<RecordInfo> getRecordInfo() {
        return recordInfo != null ? Optional.of(recordInfo) : Optional.empty();
    }
}
