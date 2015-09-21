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

package dk.dbc.dataio.jobstore.service.sequenceanalyser;

import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;

public class ChunkIdentifier implements CollisionDetectionElementIdentifier {
    public final long jobId;
    public final long chunkId;

    public ChunkIdentifier(long jobId, long chunkId) {
        this.jobId = jobId;
        this.chunkId = chunkId;
    }

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    @Override
    public String toString() {
        return "["+jobId+", "+chunkId+ "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChunkIdentifier that = (ChunkIdentifier) o;

        if (chunkId != that.chunkId) {
            return false;
        }
        if (jobId != that.jobId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + (int) (chunkId ^ (chunkId >>> 32));
        return result;
    }
}
