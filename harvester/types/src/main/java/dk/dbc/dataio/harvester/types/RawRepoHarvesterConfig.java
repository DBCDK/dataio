package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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
public class RawRepoHarvesterConfig {
    final private Set<Entry> entries;

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
    public static class Entry {
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

        public void setOpenAgencyTarget(OpenAgencyTarget openAgencyTarget) {
            this.openAgencyTarget = openAgencyTarget;
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
            if (getBatchSize() != entry.getBatchSize()) return false;
            if (getId() != null ? !getId().equals(entry.getId()) : entry.getId() != null) return false;
            if (getResource() != null ? !getResource().equals(entry.getResource()) : entry.getResource() != null)
                return false;
            if (getConsumerId() != null ? !getConsumerId().equals(entry.getConsumerId()) : entry.getConsumerId() != null)
                return false;
            if (getDestination() != null ? !getDestination().equals(entry.getDestination()) : entry.getDestination() != null)
                return false;
            if (getType() != entry.getType()) return false;
            if (getFormat() != null ? !getFormat().equals(entry.getFormat()) : entry.getFormat() != null) return false;
            if (formatOverrides != null ? !formatOverrides.equals(entry.formatOverrides) : entry.formatOverrides != null)
                return false;
            return !(getOpenAgencyTarget() != null ? !getOpenAgencyTarget().equals(entry.getOpenAgencyTarget()) : entry.getOpenAgencyTarget() != null);

        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getResource() != null ? getResource().hashCode() : 0);
            result = 31 * result + (getConsumerId() != null ? getConsumerId().hashCode() : 0);
            result = 31 * result + (getDestination() != null ? getDestination().hashCode() : 0);
            result = 31 * result + (getType() != null ? getType().hashCode() : 0);
            result = 31 * result + (getFormat() != null ? getFormat().hashCode() : 0);
            result = 31 * result + (formatOverrides != null ? formatOverrides.hashCode() : 0);
            result = 31 * result + (includeRelations ? 1 : 0);
            result = 31 * result + getBatchSize();
            result = 31 * result + (getOpenAgencyTarget() != null ? getOpenAgencyTarget().hashCode() : 0);
            return result;
        }

    }
}
