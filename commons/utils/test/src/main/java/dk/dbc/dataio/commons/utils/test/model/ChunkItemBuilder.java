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

import dk.dbc.dataio.commons.types.ChunkItem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class ChunkItemBuilder {
    private long id = 0L;
    private byte[] data = "data".getBytes();
    private ChunkItem.Status status = ChunkItem.Status.SUCCESS;
    private ArrayList<ChunkItem.Type> type = new ArrayList<>( Arrays.asList(ChunkItem.Type.UNKNOWN));
    private String encoding = "UTF-8";

    public ChunkItemBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public ChunkItemBuilder setData(byte[] data) {
        this.data = data;
        return this;
    }

    public ChunkItemBuilder setData(String data) {
        setData(data.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public ChunkItemBuilder setStatus(ChunkItem.Status status) {
        this.status = status;
        return this;
    }

    public ChunkItemBuilder setType( ChunkItem.Type type ) {
        this.type= new ArrayList<>(Arrays.asList( type ));
        return this;
    }

    public ChunkItemBuilder setEncoding( String encoding) {
        this.encoding = encoding;
        return this;
    }

    public ChunkItem build() {
        return new ChunkItem(id, data, status, type, encoding);
    }
}
