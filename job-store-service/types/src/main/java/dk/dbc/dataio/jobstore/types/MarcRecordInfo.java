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

package dk.dbc.dataio.jobstore.types;

/**
 * This class contains information about a bibliographic record
 * extended with information deduced from the fact that we know it is a MARC record
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MarcRecordInfo extends RecordInfo {
    public enum RecordType {
        STANDALONE, HEAD, SECTION, VOLUME
    }

    private final RecordType type;
    private final boolean delete;
    private final String parentRelation;

    /**
     * constructor
     * @param id identifier of marc record
     * @param type type of marc record
     * @param isDelete flag indicating if marc record is delete marked
     * @param parentRelation identifier of marc record parent, can be null or empty
     */
    @JsonCreator
    public MarcRecordInfo(
            @JsonProperty("id") String id,
            @JsonProperty("type") RecordType type,
            @JsonProperty("delete") boolean isDelete,
            @JsonProperty("parentRelation") String parentRelation) {
        super(id);
        this.type = type;
        this.delete = isDelete;
        if (parentRelation != null) {
            parentRelation = parentRelation.trim();
            if (parentRelation.isEmpty()) {
                parentRelation = null;
            }
        }
        this.parentRelation = parentRelation;
    }

    public boolean isDelete() {
        return delete;
    }

    public RecordType getType() {
        return type;
    }

    public String getParentRelation() {
        return parentRelation;
    }

    @JsonIgnore
    public boolean isHead() {
        return type == RecordType.HEAD;
    }

    @JsonIgnore
    public boolean isSection() {
        return type == RecordType.SECTION;
    }

    @JsonIgnore
    public boolean isVolume() {
        return type == RecordType.VOLUME;
    }

    public boolean hasParentRelation() {
        return parentRelation != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MarcRecordInfo that = (MarcRecordInfo) o;

        if (delete != that.delete) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        return parentRelation != null ? parentRelation.equals(that.parentRelation) : that.parentRelation == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (delete ? 1 : 0);
        result = 31 * result + (parentRelation != null ? parentRelation.hashCode() : 0);
        return result;
    }
}
