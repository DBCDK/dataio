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
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * FlowComponent DTO class.
 */
public class FlowComponent implements Serializable {
    public static final FlowComponentContent UNDEFINED_NEXT = null;

    private static final long serialVersionUID = 2743968388816680751L;

    private final long id;
    private long version;
    private FlowComponentContent content;
    private FlowComponentContent next;

    /**
     * Class constructor
     *
     * @param id flow component id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
     * @param version flow component version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
     * @param content flow component content
     * @param next next edition of flow component (can be null)
     *
     * @throws NullPointerException when given null valued argument
     * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
     */
    @JsonCreator
    public FlowComponent(@JsonProperty("id") long id,
                         @JsonProperty("version") long version,
                         @JsonProperty("content") FlowComponentContent content,
                         @JsonProperty("next") FlowComponentContent next) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.version = InvariantUtil.checkLowerBoundOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
        this.next = next;
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public FlowComponent withVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowComponent withContent(FlowComponentContent content) {
        this.content = content;
        return this;
    }

    public FlowComponentContent getContent() {
        return content;
    }

    public FlowComponent withNext(FlowComponentContent next) {
        this.next = next;
        return this;
    }

    public FlowComponentContent getNext() {
        return next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowComponent that = (FlowComponent) o;

        return id == that.id
                && version == that.version
                && content.equals(that.content)
                && !(next != null ? !next.equals(that.next) : that.next != null);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        result = 31 * result + (next != null ? next.hashCode() : 0);
        return result;
    }
}