/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import java.io.Serializable;
import java.util.Objects;

public class FlowBinderIdent implements Serializable {
    private Long flowBinderId;
    private String flowBinderName;

    @JsonCreator
    public FlowBinderIdent(
            @JsonProperty("flowBinderName") String flowBinderName,
            @JsonProperty("flowBinderId") Long flowBinderId) {
        this.flowBinderName = flowBinderName;
        this.flowBinderId = flowBinderId;
    }

    // For GWT serialization
    private FlowBinderIdent() {}

    public String getFlowBinderName() {
        return flowBinderName;
    }

    public Long getFlowBinderId() {
        return flowBinderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowBinderIdent that = (FlowBinderIdent) o;
        return Objects.equals(flowBinderId, that.flowBinderId) &&
                Objects.equals(flowBinderName, that.flowBinderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowBinderId, flowBinderName);
    }

    @Override
    public String toString() {
        return "FlowBinderWithSubmitter{" +
                "flowBinderId=" + flowBinderId +
                ", flowBinderName='" + flowBinderName + '\'' +
                '}';
    }
}
