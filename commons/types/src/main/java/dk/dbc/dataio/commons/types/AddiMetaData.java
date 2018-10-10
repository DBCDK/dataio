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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing Addi format meta data content
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class AddiMetaData {
    // If needed set -Duser.timezone on the JVM level to get desired behaviour from ZoneId.systemDefault()
    @JsonIgnore
    private static final DateTimeFormatter CREATION_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());

    @JsonProperty
    private Integer submitter;
    @JsonProperty
    private String format;
    @JsonProperty
    private String bibliographicRecordId;
    @JsonProperty
    private String trackingId;
    @JsonProperty
    private Boolean deleted;
    @JsonProperty
    private Date creationDate;
    @JsonProperty
    private String enrichmentTrail;
    @JsonProperty
    private Diagnostic diagnostic;
    @JsonProperty
    @JsonUnwrapped
    private LibraryRules libraryRules;
    @JsonProperty
    private String pid;
    @JsonProperty
    private String ocn;
    @JsonProperty
    private Map<String, Integer> holdingsStatusMap;

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

    public AddiMetaData withDeleted(Boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    @JsonIgnore
    public boolean isDeleted() {
        return deleted != null && deleted;
    }

    public AddiMetaData withCreationDate(Date creationDate) {
        if (creationDate != null) {
            this.creationDate = new Date(creationDate.getTime());
        }
        return this;
    }

    public Date creationDate() {
        if (creationDate != null) {
            return new Date(creationDate.getTime());
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

    public String pid() {
        return pid;
    }

    public AddiMetaData withPid(String pid) {
        this.pid = pid;
        return this;
    }


    public String ocn() {
        return ocn;
    }

    public AddiMetaData withOcn(String ocn) {
        this.ocn = ocn;
        return this;
    }

    public Map<String, Integer> holdingsStatusMap() {
        return holdingsStatusMap;
    }

    public AddiMetaData withHoldingsStatusMap(Map<String, Integer> holdingsStatusMap) {
        this.holdingsStatusMap = holdingsStatusMap;
        return this;
    }

    @JsonProperty
    public String formattedCreationDate() {
        if (creationDate() != null) {
            return CREATION_DATE_FORMATTER.format(creationDate.toInstant());
        }
        return null;
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
        return Objects.equals(submitter, that.submitter) &&
                Objects.equals(format, that.format) &&
                Objects.equals(bibliographicRecordId, that.bibliographicRecordId) &&
                Objects.equals(trackingId, that.trackingId) &&
                Objects.equals(deleted, that.deleted) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(enrichmentTrail, that.enrichmentTrail) &&
                Objects.equals(diagnostic, that.diagnostic) &&
                Objects.equals(libraryRules, that.libraryRules) &&
                Objects.equals(pid, that.pid) &&
                Objects.equals(ocn, that.ocn) &&
                Objects.equals(holdingsStatusMap, that.holdingsStatusMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submitter, format, bibliographicRecordId, trackingId, deleted,
                creationDate, enrichmentTrail, diagnostic, libraryRules, pid, ocn, holdingsStatusMap);
    }

    @Override
    public String toString() {
        return "AddiMetaData{" +
                "submitter=" + submitter +
                ", format='" + format + '\'' +
                ", bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", trackingId='" + trackingId + '\'' +
                ", deleted=" + deleted +
                ", creationDate=" + creationDate +
                ", enrichmentTrail='" + enrichmentTrail + '\'' +
                ", diagnostic=" + diagnostic +
                ", libraryRules=" + libraryRules + '\'' +
                ", pid='" + pid + '\'' +
                ", ocn='" + ocn + '\'' +
                ", holdingsStatusMap='" + holdingsStatusMap + "'" +
                '}';
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    public static class LibraryRules {
        @JsonProperty
        private String agencyType;
        @JsonProperty
        private Map<String, Object> rules = new HashMap<>();

        public LibraryRules withAgencyType(String type) {
            agencyType = type;
            return this;
        }

        public String agencyType() {
            return agencyType;
        }

        public LibraryRules withLibraryRule(String rule, Object value) {
            rules.put(rule, value);
            return this;
        }

        @JsonIgnore
        public Map<String, Object> getLibraryRules() {
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
