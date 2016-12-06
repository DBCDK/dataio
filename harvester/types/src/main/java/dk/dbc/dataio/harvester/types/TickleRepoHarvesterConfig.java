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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.io.Serializable;

public class TickleRepoHarvesterConfig extends HarvesterConfig<TickleRepoHarvesterConfig.Content> implements Serializable {
    private static final long serialVersionUID = -1959690053893466276L;

    @JsonCreator
    public TickleRepoHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public TickleRepoHarvesterConfig() { }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class Content implements Serializable {
        private static final long serialVersionUID = -5437124801330551281L;

        public Content() { }

        // Data

        /** ID of harvest operation */
        private String id;

        /** JNDI name of rawrepo JDBC resource */
        private String name;

        /** Description */
        private String description;

        /** Destination for harvested items */
        private String destination;

        /** Format of harvested items */
        private String format;

        /** Job type of harvested items (default is TRANSIENT */
        private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;

        /** Flag Indicating if the configuration is enabled */
        @JsonProperty
        private boolean enabled = false;



        // Getters and Setters

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
            return this;
        }

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

        public String getDestination() {
            return destination;
        }

        public Content withDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public Content withFormat(String format) {
            this.format = format;
            return this;
        }

        public JobSpecification.Type getType() {
            return type;
        }

        public Content withType(JobSpecification.Type type) {
            this.type = type;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Content withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }


        // Other methods

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Content)) return false;

            Content content = (Content) o;

            if (enabled != content.enabled) return false;
            if (id != null ? !id.equals(content.id) : content.id != null) return false;
            if (name != null ? !name.equals(content.name) : content.name != null) return false;
            if (description != null ? !description.equals(content.description) : content.description != null)
                return false;
            if (destination != null ? !destination.equals(content.destination) : content.destination != null)
                return false;
            if (format != null ? !format.equals(content.format) : content.format != null) return false;
            return type == content.type;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", type=" + type +
                    ", enabled=" + enabled +
                    '}';
        }
    }
}
