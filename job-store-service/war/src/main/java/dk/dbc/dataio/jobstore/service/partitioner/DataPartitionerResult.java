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

/**
 * This class encapsulates a data partitioner result containing the actual chunk item as
 * well as record meta data
 */
public class DataPartitionerResult {
    public static final DataPartitionerResult EMPTY = new DataPartitionerResult(null, null, 0);
    private final ChunkItem chunkItem;
    private final RecordInfo recordInfo;
    private final int positionInDatafile;

    public DataPartitionerResult(ChunkItem chunkItem, RecordInfo recordInfo, int positionInDatafile) {
        this.chunkItem = chunkItem;
        this.recordInfo = recordInfo;
        this.positionInDatafile = positionInDatafile;
    }

    public ChunkItem getChunkItem() {
        return chunkItem;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public int getPositionInDatafile() {
        return positionInDatafile;
    }

    public boolean isEmpty() {
        return chunkItem == null && recordInfo == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataPartitionerResult that = (DataPartitionerResult) o;

        if (positionInDatafile != that.positionInDatafile) {
            return false;
        }
        if (chunkItem != null ? !chunkItem.equals(that.chunkItem) : that.chunkItem != null) {
            return false;
        }
        return recordInfo != null ? recordInfo.equals(that.recordInfo) : that.recordInfo == null;
    }

    @Override
    public int hashCode() {
        int result = chunkItem != null ? chunkItem.hashCode() : 0;
        result = 31 * result + (recordInfo != null ? recordInfo.hashCode() : 0);
        result = 31 * result + positionInDatafile;
        return result;
    }
}
