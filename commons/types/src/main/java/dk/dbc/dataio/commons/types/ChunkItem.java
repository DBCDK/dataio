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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Chunk item DTO class.
 */
public class ChunkItem implements Serializable {
    public static final ChunkItem UNDEFINED = null;
    private static final long serialVersionUID = -7214362358523195493L;


    public enum Status { SUCCESS, FAILURE, IGNORE }
    public enum Type { UNKNOWN,
        ADDI,
        DANMARC2LINEFORMAT,
        GENERICXML,
        MARCXCHANGE,
        STRING,
    }

    private long id;
    private final byte[] data;
    private Status status;
    @JsonProperty("type") private List<Type> type;
    @JsonProperty("diagnostics") private ArrayList<Diagnostic> diagnostics = null;
    @JsonProperty("encoding") private final String encoding;


    /**
     * Class constructor
     *
     * @param id item identifier, must be larger than {@value Constants#CHUNK_ITEM_ID_LOWER_BOUND}
     * @param data item data, can be empty
     * @param status item status
     * @param type item type as list to support Embeddable formats.
     * @param encoding item charset encoding
     * @throws NullPointerException when given null valued argument
     * @throws IllegalArgumentException when given id value of {@value dk.dbc.dataio.commons.types.Constants#CHUNK_ITEM_ID_LOWER_BOUND} or less
     */
    @JsonCreator
    public ChunkItem(
            @JsonProperty("id") long id,
            @JsonProperty("data") byte[] data,
            @JsonProperty("status") Status status,
            @JsonProperty("type") List<Type> type,
            @JsonProperty("encoding") String encoding) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.CHUNK_ITEM_ID_LOWER_BOUND);
        this.data = InvariantUtil.checkNotNullOrThrow(data, "data");
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
        // ToDo: type and encoding must have invariant checks after a transition period
        this.type = type == null ? type : new ArrayList<>(type);
        this.encoding = encoding;
    }


    public ChunkItem(long id, byte[] data, Status status) {
        this(id, data, status, Collections.singletonList(Type.UNKNOWN), StandardCharsets.UTF_8.name());
    }


    /**
     * If diagnostic level is different from WARNING:
     * Set Status to FAILURE and append Diagnostics to describe the reason
     * for failing the item.
     *
     * @param diagnostic Description of the reason for the failure
     */
    public void appendDiagnostics(Diagnostic diagnostic) {
        if(diagnostic.getLevel() != Diagnostic.Level.WARNING) {
            this.status = Status.FAILURE;
        }
        if (diagnostics == null) {
            diagnostics = new ArrayList<>();
        }
        diagnostics.add(diagnostic);
    }

    /**
     * If given list of diagnostics is not null or empty:
     *   - If diagnostic level is different from WARNING: Set Status to FAILURE.
     *   - Append all input Diagnostics.
     *
     * @param diagnostics containing list of descriptions of the reasons for failure
     */
    public void appendDiagnostics(List<Diagnostic> diagnostics) {
        if(diagnostics != null && !diagnostics.isEmpty()) {
            if(diagnostics.stream().anyMatch(diagnostic -> diagnostic.getLevel() != Diagnostic.Level.WARNING)) {
                this.status = Status.FAILURE;
            }
            if (this.diagnostics == null) {
                this.diagnostics = new ArrayList<>(diagnostics);
            } else {
                this.diagnostics.addAll(diagnostics);
            }
        }
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

    public List<Type> getType() {
        return type;
    }

    public ArrayList<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public String getEncoding() { return encoding; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkItem)) return false;
        ChunkItem chunkItem = (ChunkItem) o;
        return id == chunkItem.id &&
                Arrays.equals(data, chunkItem.data) &&
                status == chunkItem.status &&
                Objects.equals(type, chunkItem.type) &&
                Objects.equals(diagnostics, chunkItem.diagnostics) &&
                Objects.equals(encoding, chunkItem.encoding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data, status, type, diagnostics, encoding);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChunkItem{");
        sb.append("id=").append(id);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append(", status=").append(status);
        sb.append(", type=").append(type);
        sb.append(", endoding=").append(encoding);
        sb.append('}');
        return sb.toString();
    }
}
