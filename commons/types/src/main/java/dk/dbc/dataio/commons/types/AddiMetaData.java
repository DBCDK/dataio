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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Optional;

/**
 * Class representing Addi format meta data content
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class AddiMetaData {
    @JsonProperty
    private Integer submitter;
    @JsonProperty
    private String format;
    @JsonProperty
    private String bibliographicRecordId;
    @JsonProperty
    private String trackingId;

    public AddiMetaData withSubmitterNumber(Integer submitterNumber) {
        submitter = submitterNumber;
        return this;
    }

    public Optional<Integer> submitterNumber() {
        return getValue(submitter);
    }

    public AddiMetaData withFormat(String format) {
        this.format = format;
        return this;
    }

    public Optional<String> format() {
        return getValue(format);
    }

    public AddiMetaData withBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
        return this;
    }

    public Optional<String> bibliographicRecordId() {
        return getValue(bibliographicRecordId);
    }

    public AddiMetaData withTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public Optional<String> trackingId() {
        return getValue(trackingId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AddiMetaData that = (AddiMetaData) o;

        if (submitter != null ? !submitter.equals(that.submitter) : that.submitter != null) {
            return false;
        }
        if (format != null ? !format.equals(that.format) : that.format != null) {
            return false;
        }
        if (bibliographicRecordId != null ? !bibliographicRecordId.equals(that.bibliographicRecordId) : that.bibliographicRecordId != null) {
            return false;
        }
        return trackingId != null ? trackingId.equals(that.trackingId) : that.trackingId == null;

    }

    @Override
    public int hashCode() {
        int result = submitter != null ? submitter.hashCode() : 0;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (bibliographicRecordId != null ? bibliographicRecordId.hashCode() : 0);
        result = 31 * result + (trackingId != null ? trackingId.hashCode() : 0);
        return result;
    }

    private <T> Optional<T> getValue(T value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
