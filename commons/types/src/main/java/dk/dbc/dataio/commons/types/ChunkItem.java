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

package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Chunk item DTO class.
 */
public class ChunkItem implements Serializable {
    public static final ChunkItem UNDEFINED = null;
    private static final long serialVersionUID = -7214362358523195493L;

    public enum Status { SUCCESS, FAILURE, IGNORE }

    private final long id;
    private final byte[] data;
    private final Status status;

    /**
     * Class constructor
     *
     * @param id item identifier, must be larger than {@value dk.dbc.dataio.commons.types.Constants#CHUNK_ITEM_ID_LOWER_BOUND}
     * @param data item data, can be empty
     * @param status item status
     *
     * @throws NullPointerException when given null valued argument
     * @throws IllegalArgumentException when given id value of {@value dk.dbc.dataio.commons.types.Constants#CHUNK_ITEM_ID_LOWER_BOUND} or less
     */
    @JsonCreator
    public ChunkItem(
            @JsonProperty("id") long id,
            @JsonProperty("data") byte[] data,
            @JsonProperty("status") Status status) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.CHUNK_ITEM_ID_LOWER_BOUND);
        this.data = InvariantUtil.checkNotNullOrThrow(data, "data");
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChunkItem chunkItem = (ChunkItem) o;

        return id == chunkItem.id
                && Arrays.equals(data, chunkItem.data)
                && status == chunkItem.status;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + status.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChunkItem{");
        sb.append("id=").append(id);
        sb.append(", data=").append(new String(data, StandardCharsets.UTF_8));
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
