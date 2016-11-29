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

package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;

import java.util.StringTokenizer;

/**
 * Simple batch-exchange batch name abstraction
 */
public class BatchName {
    private final long jobId;
    private final long chunkId;

    BatchName(long jobId, long chunkId) {
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
        return String.format("%d-%d", jobId, chunkId);
    }

    /**
     * Creates new batch name from chunk
     * @param chunk chunk for which a batch name is to be generated
     * @return BatchName object
     */
    public static BatchName fromChunk(Chunk chunk) {
        return new BatchName(chunk.getJobId(), chunk.getChunkId());
    }

    /**
     * Creates new batch name from given string which must be formatted as [JOB_ID]-[CHUNK_ID]
     * @param name name to be parsed
     * @return BatchName object
     * @throws IllegalArgumentException if given invalid name string
     */
    public static BatchName fromString(String name) throws IllegalArgumentException {
        final StringTokenizer tokenizer = new StringTokenizer(name, "-");
        if (tokenizer.countTokens() != 2) {
            throw new IllegalArgumentException("Name does not match [JOB_ID]-[CHUNK_ID] pattern: " + name);
        }
        try {
            return new BatchName(
                    Long.parseLong(tokenizer.nextToken()),
                    Long.parseLong(tokenizer.nextToken()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid batch name: " + name, e);
        }
    }
}
