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
        } catch (NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Class representing a harvest operation.
     */
    public static class Entry {
        /** ID of harvest operation
         */
        private String id;
        /** JNDI name of rawrepo JDBC resource
         */
        private String resource;

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

        @Override
        public String toString() {
            return "Entry{" +
                    "id='" + id + '\'' +
                    ", resource='" + resource + '\'' +
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
            return result;
        }
    }
}
