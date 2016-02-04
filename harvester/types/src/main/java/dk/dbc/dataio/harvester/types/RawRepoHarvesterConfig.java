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

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class representing a RawRepo harvester configuration.
 * <p>
 * A configuration can contain multiple entries, where each entry represents
 * an independent harvest operation.
 * </p>
 */
public class RawRepoHarvesterConfig implements Serializable {
    private static final long serialVersionUID = 8380293486069807815L;

    final private HashSet<Entry> entries;

    public RawRepoHarvesterConfig() {
        entries = new HashSet<>();
    }

    /**
     * Adds given entry to config.
     * @param entry configuration entry for an independent harvest operation
     * @return true if this configuration did not already contain the specified entry, otherwise false.
     * @throws IllegalArgumentException if specified entry is invalid
     */
    public boolean addEntry(Entry entry) throws IllegalArgumentException {
        verifyEntry(entry);
        return entries.add(entry);
    }

    /**
     * @return set of configuration entries.
     */
    public Set<Entry> getEntries() {
        return entries;
    }

    @Override
    public String toString() {
        return "RawRepoHarvesterConfig{" +
                "entries=" + entries +
                '}';
    }

    private void verifyEntry(Entry entry) throws IllegalArgumentException {
        try {
            InvariantUtil.checkNotNullOrThrow(entry, "entry");
            InvariantUtil.checkNotNullNotEmptyOrThrow(entry.getId(), "entry.id");
            InvariantUtil.checkNotNullNotEmptyOrThrow(entry.getResource(), "entry.resource");
            InvariantUtil.checkNotNullNotEmptyOrThrow(entry.getConsumerId(), "entry.consumerId");
            InvariantUtil.checkNotNullNotEmptyOrThrow(entry.getDestination(), "entry.destination");
            InvariantUtil.checkNotNullNotEmptyOrThrow(entry.getFormat(), "entry.format");
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Class representing a harvest operation.
     */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1351171822608996512L;

        /** ID of harvest operation */
        private String id;

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
        private final Map<Integer, String> formatOverrides;

        /** Flag indicating whether or not to include
            record relations in marcXchange collections */
        @JsonProperty
        private boolean includeRelations = true;

        /** Harvest batch size (default 10000) */
        private int batchSize = 10000;

        private OpenAgencyTarget openAgencyTarget;

        public Entry() {
            formatOverrides = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public Entry setId(String id) {
            this.id = id;
            return this;
        }

        public String getResource() {
            return resource;
        }

        public Entry setResource(String resource) {
            this.resource = resource;
            return this;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Entry setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public String getConsumerId() {
            return consumerId;
        }

        public Entry setConsumerId(String consumerId) {
            this.consumerId = consumerId;
            return this;
        }

        public String getDestination() {
            return destination;
        }

        public Entry setDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public JobSpecification.Type getType() {
            return type;
        }

        public Entry setType(JobSpecification.Type type) {
            this.type = type;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public Entry setFormat(String format) {
            this.format = format;
            return this;
        }

        public String getFormat(int agencyId) {
            final String formatOverride = formatOverrides.get(agencyId);
            return formatOverride != null ? formatOverride : format;
        }

        public Map<Integer, String> getFormatOverrides() {
            return formatOverrides;
        }

        public Entry setFormatOverride(int agencyId, String format) {
            formatOverrides.put(agencyId, format);
            return this;
        }

        public boolean includeRelations() {
            return includeRelations;
        }

        public Entry setIncludeRelations(boolean includeRelations) {
            this.includeRelations = includeRelations;
            return this;
        }

        public OpenAgencyTarget getOpenAgencyTarget() {
            return openAgencyTarget;
        }

        public Entry setOpenAgencyTarget(OpenAgencyTarget openAgencyTarget) {
            this.openAgencyTarget = openAgencyTarget;
            return this;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "id='" + id + '\'' +
                    ", resource='" + resource + '\'' +
                    ", consumerId='" + consumerId + '\'' +
                    ", destination='" + destination + '\'' +
                    ", type='" + type.name() + '\'' +
                    ", format='" + format + '\'' +
                    ", formatOverrides=" + formatOverrides +
                    ", includeRelations=" + includeRelations +
                    ", batchSize=" + batchSize +
                    ", openAgencyConfig=" + openAgencyTarget +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;

            Entry entry = (Entry) o;

            if (includeRelations != entry.includeRelations) return false;
            if (batchSize != entry.batchSize) return false;
            if (id != null ? !id.equals(entry.id) : entry.id != null) return false;
            if (resource != null ? !resource.equals(entry.resource) : entry.resource != null) return false;
            if (consumerId != null ? !consumerId.equals(entry.consumerId) : entry.consumerId != null) return false;
            if (destination != null ? !destination.equals(entry.destination) : entry.destination != null) return false;
            if (type != entry.type) return false;
            if (format != null ? !format.equals(entry.format) : entry.format != null) return false;
            if (formatOverrides != null ? !formatOverrides.equals(entry.formatOverrides) : entry.formatOverrides != null)
                return false;
            return !(openAgencyTarget != null ? !openAgencyTarget.equals(entry.openAgencyTarget) : entry.openAgencyTarget != null);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (formatOverrides != null ? formatOverrides.hashCode() : 0);
            result = 31 * result + (includeRelations ? 1 : 0);
            result = 31 * result + batchSize;
            result = 31 * result + (openAgencyTarget != null ? openAgencyTarget.hashCode() : 0);
            return result;
        }
    }
}
