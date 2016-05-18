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

package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO class for USH-Solr harvester configuration
 */
public class UshSolrHarvesterConfig implements Serializable {
    private static final long serialVersionUID = 5981757061573803169L;

    private final long id;
    private final long version;
    private final Content content;

    @JsonCreator
    public UshSolrHarvesterConfig(@JsonProperty("id") long id,
                @JsonProperty("version") long version,
                @JsonProperty("content") Content content) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.version = InvariantUtil.checkLowerBoundOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
        this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Content getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UshSolrHarvesterConfig that = (UshSolrHarvesterConfig) o;

        if (id != that.id) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        return content.equals(that.content);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + content.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UshSolrHarvesterConfig{" +
                "id=" + id +
                ", version=" + version +
                ", content=" + content +
                '}';
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class Content implements Serializable {
        private static final long serialVersionUID = -2405667555579332660L;

        private String name;
        private String description;
        private String format;
        private String destination;
        private int ushOaiHarvesterJobId;
        private UshHarvesterProperties ushHarvesterProperties;
        private Date timeOfLastHarvest;

        public String getName() {
            return name;
        }

        public Content withName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Content withDescription(String description) {
            this.description = description;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public Content withFormat(String format) {
            this.format = format;
            return this;
        }

        public String getDestination() {
            return destination;
        }

        public Content withDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public Date getTimeOfLastHarvest() {
            if (timeOfLastHarvest != null) {
                return new Date(timeOfLastHarvest.getTime());
            }
            return null;
        }

        public Content withTimeOfLastHarvest(Date lastHarvested) {
            if (lastHarvested != null) {
                this.timeOfLastHarvest = new Date(lastHarvested.getTime());
            }
            return this;
        }

        public int getUshOaiHarvesterJobId() {
            return ushOaiHarvesterJobId;
        }

        public Content withUshOaiHarvesterJobId(int jobId) {
            this.ushOaiHarvesterJobId = jobId;
            return this;
        }

        @JsonIgnore
        public UshHarvesterProperties getUshHarvesterProperties() {
            return ushHarvesterProperties;
        }

        public Content withUshHarvesterProperties(UshHarvesterProperties ushHarvesterProperties) {
            this.ushHarvesterProperties = ushHarvesterProperties;
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

            Content content = (Content) o;

            if (ushOaiHarvesterJobId != content.ushOaiHarvesterJobId) {
                return false;
            }
            if (name != null ? !name.equals(content.name) : content.name != null) {
                return false;
            }
            if (description != null ? !description.equals(content.description) : content.description != null) {
                return false;
            }
            if (format != null ? !format.equals(content.format) : content.format != null) {
                return false;
            }
            if (destination != null ? !destination.equals(content.destination) : content.destination != null) {
                return false;
            }
            return timeOfLastHarvest != null ? timeOfLastHarvest.equals(content.timeOfLastHarvest) : content.timeOfLastHarvest == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            result = 31 * result + ushOaiHarvesterJobId;
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", format='" + format + '\'' +
                    ", destination='" + destination + '\'' +
                    ", ushOaiHarvesterJobId=" + ushOaiHarvesterJobId +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    '}';
        }
    }
}
