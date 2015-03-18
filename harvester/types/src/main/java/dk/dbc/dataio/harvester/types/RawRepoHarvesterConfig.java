package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.HashSet;
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

        /** Harvest batch size (default 10000) */
        private int batchSize = 10000;

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

        @Override
        public String toString() {
            return "Entry{" +
                    "id='" + id + '\'' +
                    ", resource='" + resource + '\'' +
                    ", consumerId='" + consumerId + '\'' +
                    ", destination='" + destination + '\'' +
                    ", batchSize=" + batchSize +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;

            if (batchSize != entry.batchSize) {
                return false;
            }
            if (consumerId != null ? !consumerId.equals(entry.consumerId) : entry.consumerId != null) {
                return false;
            }
            if (destination != null ? !destination.equals(entry.destination) : entry.destination != null) {
                return false;
            }
            if (id != null ? !id.equals(entry.id) : entry.id != null) {
                return false;
            }
            if (resource != null ? !resource.equals(entry.resource) : entry.resource != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + batchSize;
            return result;
        }
    }
}
