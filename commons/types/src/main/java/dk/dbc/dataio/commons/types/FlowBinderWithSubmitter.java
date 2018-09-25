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

public class FlowBinderWithSubmitter implements Serializable {
    private final Long submitterId;
    private final Long flowBinderId;
    private final String flowBinderName;

    @JsonCreator
    public FlowBinderWithSubmitter(
            @JsonProperty("flowBinderName") String flowBinderName,
            @JsonProperty("flowBinderId") Long flowBinderId,
            @JsonProperty("submitterId") Long submitterId) {
        this.flowBinderName = flowBinderName;
        this.flowBinderId = flowBinderId;
        this.submitterId = submitterId;
    }

    public String getFlowBinderName() {
        return flowBinderName;
    }

    public Long getFlowBinderId() {
        return flowBinderId;
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowBinderWithSubmitter that = (FlowBinderWithSubmitter) o;
        return Objects.equals(submitterId, that.submitterId) &&
                Objects.equals(flowBinderId, that.flowBinderId) &&
                Objects.equals(flowBinderName, that.flowBinderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submitterId, flowBinderId, flowBinderName);
    }

    @Override
    public String toString() {
        return "FlowBinderWithSubmitter{" +
                "submitterId=" + submitterId +
                ", flowBinderId=" + flowBinderId +
                ", flowBinderName='" + flowBinderName + '\'' +
                '}';
    }
}
