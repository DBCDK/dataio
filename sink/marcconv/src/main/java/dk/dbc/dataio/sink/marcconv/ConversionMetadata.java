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

package dk.dbc.dataio.sink.marcconv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversionMetadata {
    private final String origin = "dataio/sink/marcconv";
    private final String category = "dataout";
    private final boolean claimed = false;

    @JsonProperty("name")
    private String filename;

    @JsonProperty("agency")
    private Integer agencyId;

    private Integer jobId;

    public String getOrigin() {
        return origin;
    }

    public String getCategory() {
        return category;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public String getFilename() {
        return filename;
    }

    public ConversionMetadata withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public ConversionMetadata withAgencyId(Integer agencyId) {
        this.agencyId = agencyId;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ConversionMetadata withJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConversionMetadata that = (ConversionMetadata) o;
        return claimed == that.claimed &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(category, that.category) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(agencyId, that.agencyId) &&
                Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, category, claimed, filename, agencyId, jobId);
    }

    @Override
    public String toString() {
        return "ConversionMetadata{" +
                "origin='" + origin + '\'' +
                ", category='" + category + '\'' +
                ", claimed=" + claimed +
                ", filename='" + filename + '\'' +
                ", agencyId=" + agencyId +
                ", jobId=" + jobId +
                '}';
    }
}
