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
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatchNameTest {
    @Test
    public void constructor() {
        final long jobId = 4242;
        final long chunkId = 2424;
        final BatchName batchName = new BatchName(jobId, chunkId);
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }

    @Test
    public void fromChunk() {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final BatchName batchName = BatchName.fromChunk(chunk);
        assertThat("jobId", batchName.getJobId(), is(chunk.getJobId()));
        assertThat("chunkId", batchName.getChunkId(), is(chunk.getChunkId()));
    }

    @Test
    public void fromString_invalidNumberOfTokens_throws() {
        assertThat(() -> BatchName.fromString("1-2-3"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void fromString_invalidTokenTypes_throws() {
        assertThat(() -> BatchName.fromString("one-two"), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void fromString() {
        final long jobId = 4242;
        final long chunkId = 2424;
        final BatchName batchName = BatchName.fromString(String.format("%d-%d", jobId, chunkId));
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }
}
