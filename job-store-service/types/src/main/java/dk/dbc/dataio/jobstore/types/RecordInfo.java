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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * This class contains information about a bibliographic record.
 * Some time in the future this will also encompass keys for sequence analysis.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public class RecordInfo {
    protected final String id;
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pid;

    @JsonCreator
    public RecordInfo(@JsonProperty("id") String id) {
        this.id = id != null ? StringUtil.removeWhitespace(id) : null;
    }

    public String getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public RecordInfo withPid(String pid) {
        this.pid = pid;
        return this;
    }

    @JsonIgnore
    public Set<String> getKeys(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        final Set<String> keys = new HashSet<>();
        if (id != null) {
            keys.add(id);
        }
        return keys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordInfo that = (RecordInfo) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
