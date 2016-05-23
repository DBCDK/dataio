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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RRHarvesterConfig extends HarvesterConfig<RRHarvesterConfig.Content> implements  Serializable {
    private static final long serialVersionUID = 3701420845816493033L;

    @JsonCreator
    public RRHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    protected RRHarvesterConfig() { }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class Content implements Serializable {
        private static final long serialVersionUID = 2870875843923021216L;

        public Content() { }

        /** ID of harvest operation */
        private String id;

        /** Flag Indicating if the Configuation is enabled */
        @JsonProperty
        private boolean enabled = false;

        /** JNDI name of rawrepo JDBC resource */
        private String resource;

        /** rawrepo queue consumer ID */
        private String consumerId;

        /** Destination for harvested items */
        private String destination;

        /** Job type of harvested items (default is TRANSIENT */
        private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;

        /** Format of harvested items */
        private String format;

        /** Optional format overrides for specific agencyIds */
        @JsonProperty
        private final Map<Integer, String> formatOverrides=new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        /** Flag indicating whether or not to include
         record relations in marcXchange collections */
        @JsonProperty
        private boolean includeRelations = true;

        /** Harvest batch size (default 10000) */
        private int batchSize = 10000;

        private OpenAgencyTarget openAgencyTarget;

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
            return this;
        }

        public Content withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Content withResource(String resource) {
            this.resource = resource;
            return this;
        }

        public String getResource() {
            return resource;
        }

        public String getConsumerId() {
            return consumerId;
        }

        public Content withConsumerId(String consumerId) {
            this.consumerId = consumerId;
            return this;
        }

        public String getDestination() {
            return destination;
        }

        public Content withDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public JobSpecification.Type getType() {
            return type;
        }

        public Content withType(JobSpecification.Type type) {
            this.type = type;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public Content withFormat(String format) {
            this.format = format;
            return this;
        }


        public Map<Integer, String> getFormatOverrides() {
            return formatOverrides;
        }

        public Content withFormatOverridesEntry( Integer formatKey, String formatOverride) {
            this.formatOverrides.put( formatKey, formatOverride );
            return this;
        }

        public boolean isIncludeRelations() {
            return includeRelations;
        }

        public Content withIncludeRelations(boolean includeRelations) {
            this.includeRelations = includeRelations;
            return this;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Content withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public OpenAgencyTarget getOpenAgencyTarget() {
            return openAgencyTarget;
        }

        public Content withOpenAgencyTarget(OpenAgencyTarget openAgencyTarget) {
            this.openAgencyTarget = openAgencyTarget;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Content)) return false;
            Content content = (Content) o;
            return enabled == content.enabled &&
                    includeRelations == content.includeRelations &&
                    batchSize == content.batchSize &&
                    Objects.equals(id, content.id) &&
                    Objects.equals(resource, content.resource) &&
                    Objects.equals(consumerId, content.consumerId) &&
                    Objects.equals(destination, content.destination) &&
                    type == content.type &&
                    Objects.equals(format, content.format) &&
                    Objects.equals(formatOverrides, content.formatOverrides) &&
                    Objects.equals(openAgencyTarget, content.openAgencyTarget);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, enabled, resource, consumerId, destination, type, format, formatOverrides, includeRelations, batchSize, openAgencyTarget);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Content{");
            sb.append("batchSize=").append(batchSize);
            sb.append(", isEnabled=").append(enabled);
            sb.append(", resource='").append(resource).append('\'');
            sb.append(", consumerId='").append(consumerId).append('\'');
            sb.append(", destination='").append(destination).append('\'');
            sb.append(", type=").append(type);
            sb.append(", format='").append(format).append('\'');
            sb.append(", formatOverrides=").append(formatOverrides);
            sb.append(", includeRelations=").append(includeRelations);
            sb.append(", openAgencyTarget=").append(openAgencyTarget);
            sb.append('}');
            return sb.toString();
        }
    }
}
