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

package dk.dbc.dataio.sink.ticklerepo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TickleAttributes {
    @JsonProperty("submitter")
    private Integer agencyId;
    @JsonProperty
    private String datasetName;
    @JsonProperty
    private String bibliographicRecordId;
    @JsonProperty
    private String compareRecord;
    @JsonProperty
    private Boolean deleted;

    public Integer getAgencyId() {
        return agencyId;
    }

    public TickleAttributes withAgencyId(Integer agencyId) {
        this.agencyId = agencyId;
        return this;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public TickleAttributes withDatasetName(String datasetName) {
        this.datasetName = datasetName;
        return this;
    }

    public String getCompareRecord() {
        return compareRecord;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public TickleAttributes withBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
        return this;
    }

    public TickleAttributes withCompareRecord(String compareRecord) {
        this.compareRecord = compareRecord;
        return this;
    }

    public Boolean isDeleted() {
        return deleted != null && deleted;
    }

    public TickleAttributes withDeleted(Boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    @JsonIgnore
    public boolean isValid() {
        return agencyId != null
                && datasetName != null && !datasetName.trim().isEmpty()
                && bibliographicRecordId != null && !bibliographicRecordId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TickleAttributes{" +
                "agencyId=" + agencyId +
                ", datasetName='" + datasetName + '\'' +
                ", bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
