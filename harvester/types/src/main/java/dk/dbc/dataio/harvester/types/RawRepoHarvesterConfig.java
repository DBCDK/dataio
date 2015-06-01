package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.net.URL;
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
                    ", format='" + format + '\'' +
                    ", formatOverrides=" + formatOverrides +
                    ", includeRelations=" + includeRelations +
                    ", batchSize=" + batchSize +
                    ", openAgencyConfig=" + openAgencyTarget +
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

            if (includeRelations != entry.includeRelations) {
                return false;
            }
            if (batchSize != entry.batchSize) {
                return false;
            }
            if (id != null ? !id.equals(entry.id) : entry.id != null) {
                return false;
            }
            if (resource != null ? !resource.equals(entry.resource) : entry.resource != null) {
                return false;
            }
            if (consumerId != null ? !consumerId.equals(entry.consumerId) : entry.consumerId != null) {
                return false;
            }
            if (destination != null ? !destination.equals(entry.destination) : entry.destination != null) {
                return false;
            }
            if (format != null ? !format.equals(entry.format) : entry.format != null) {
                return false;
            }
            if (formatOverrides != null ? !formatOverrides.equals(entry.formatOverrides) : entry.formatOverrides != null) {
                return false;
            }
            return !(openAgencyTarget != null ? !openAgencyTarget.equals(entry.openAgencyTarget) : entry.openAgencyTarget != null);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (formatOverrides != null ? formatOverrides.hashCode() : 0);
            result = 31 * result + (includeRelations ? 1 : 0);
            result = 31 * result + batchSize;
            result = 31 * result + (openAgencyTarget != null ? openAgencyTarget.hashCode() : 0);
            return result;
        }
    }

    /**
     * Configuration for an OpenAgency target
     */
    public static class OpenAgencyTarget {
        private URL url;
        private String group;
        private String user;
        private String password;

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OpenAgencyTarget that = (OpenAgencyTarget) o;

            if (url != null ? !url.equals(that.url) : that.url != null) {
                return false;
            }
            if (group != null ? !group.equals(that.group) : that.group != null) {
                return false;
            }
            if (user != null ? !user.equals(that.user) : that.user != null) {
                return false;
            }
            return !(password != null ? !password.equals(that.password) : that.password != null);

        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + (group != null ? group.hashCode() : 0);
            result = 31 * result + (user != null ? user.hashCode() : 0);
            result = 31 * result + (password != null ? password.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "OpenAgencyTarget{" +
                    "url=" + url +
                    ", group='" + group + '\'' +
                    ", user='" + user + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
