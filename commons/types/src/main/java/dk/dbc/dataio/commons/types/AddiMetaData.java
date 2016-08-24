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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    @JsonProperty
    private Date creationDate;
    @JsonProperty
    private String enrichmentTrail;
    @JsonProperty
    private Diagnostic diagnostic;
    @JsonProperty
    @JsonUnwrapped
    private LibraryRules libraryRules;

    public AddiMetaData withSubmitterNumber(Integer submitterNumber) {
        submitter = submitterNumber;
        return this;
    }

    public Integer submitterNumber() {
        return submitter;
    }

    public AddiMetaData withFormat(String format) {
        this.format = format;
        return this;
    }

    public String format() {
        return format;
    }

    public AddiMetaData withBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
        return this;
    }

    public String bibliographicRecordId() {
        return bibliographicRecordId;
    }

    public AddiMetaData withTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public String trackingId() {
        return trackingId;
    }

    public AddiMetaData withCreationDate(Date creationDate) {
        if (creationDate != null) {
            this.creationDate = new Date(creationDate.getTime());
        }
        return this;
    }

    public Date creationDate() {
        if (creationDate != null) {
            return new Date(creationDate().getTime());
        }
        return null;
    }

    public AddiMetaData withEnrichmentTrail(String enrichmentTrail) {
        this.enrichmentTrail = enrichmentTrail;
        return this;
    }

    public String enrichmentTrail() {
        return enrichmentTrail;
    }

    public AddiMetaData withDiagnostic(Diagnostic diagnostic) {
        this.diagnostic = diagnostic;
        return this;
    }

    public Diagnostic diagnostic() {
        return diagnostic;
    }

    public LibraryRules libraryRules() {
        return libraryRules;
    }

    public AddiMetaData withLibraryRules(LibraryRules libraryRules) {
        this.libraryRules = libraryRules;
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
        if (trackingId != null ? !trackingId.equals(that.trackingId) : that.trackingId != null) {
            return false;
        }
        if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) {
            return false;
        }
        if (enrichmentTrail != null ? !enrichmentTrail.equals(that.enrichmentTrail) : that.enrichmentTrail != null) {
            return false;
        }
        if (diagnostic != null ? !diagnostic.equals(that.diagnostic) : that.diagnostic != null) {
            return false;
        }
        return libraryRules != null ? libraryRules.equals(that.libraryRules) : that.libraryRules == null;

    }

    @Override
    public int hashCode() {
        int result = submitter != null ? submitter.hashCode() : 0;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (bibliographicRecordId != null ? bibliographicRecordId.hashCode() : 0);
        result = 31 * result + (trackingId != null ? trackingId.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (enrichmentTrail != null ? enrichmentTrail.hashCode() : 0);
        result = 31 * result + (diagnostic != null ? diagnostic.hashCode() : 0);
        result = 31 * result + (libraryRules != null ? libraryRules.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AddiMetaData{" +
                "submitter=" + submitter +
                ", format='" + format + '\'' +
                ", bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", trackingId='" + trackingId + '\'' +
                ", creationDate=" + creationDate +
                ", enrichmentTrail='" + enrichmentTrail + '\'' +
                ", diagnostic=" + diagnostic +
                ", libraryRules=" + libraryRules +
                '}';
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    public static class LibraryRules {
        @JsonProperty
        private String agencyType;
        @JsonProperty
        private Map<String, Boolean> rules = new HashMap<>();

        public LibraryRules withAgencyType(String type) {
            agencyType = type;
            return this;
        }

        public String agencyType() {
            return agencyType;
        }

        public LibraryRules withLibraryRule(String rule, Boolean value) {
            rules.put(rule, value);
            return this;
        }

        @JsonIgnore
        public Map<String, Boolean> getLibraryRules() {
            return rules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LibraryRules that = (LibraryRules) o;

            if (agencyType != null ? !agencyType.equals(that.agencyType) : that.agencyType != null) {
                return false;
            }
            return rules.equals(that.rules);

        }

        @Override
        public int hashCode() {
            int result = agencyType != null ? agencyType.hashCode() : 0;
            result = 31 * result + rules.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "LibraryRules{" +
                    "agencyType='" + agencyType + '\'' +
                    ", rules=" + rules +
                    '}';
        }
    }
}
