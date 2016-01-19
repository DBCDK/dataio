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

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChunkBuilder {
    private long jobId = 3;
    private long chunkId = 1;
    private final Chunk.Type type;
    private List<ChunkItem> items = new ArrayList<>(Collections.singletonList(new ChunkItemBuilder().build()));
    private List<ChunkItem> next = null;

    public ChunkBuilder(Chunk.Type type) {
        this.type = type;
    }

    public ChunkBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ChunkBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkBuilder setItems(List<ChunkItem> items) {
        this.items = items;
        return this;
    }

    public ChunkBuilder setNextItems(List<ChunkItem> nextItems) {
        this.next = nextItems;
        return this;
    }

    public Chunk build() {
        final Chunk chunk = new Chunk(jobId, chunkId, type);
        chunk.addAllItems(items, next);
        return chunk;
    }
}
