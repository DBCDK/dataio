package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RRV3HarvesterConfig extends HarvesterConfig<RRV3HarvesterConfig.Content> implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum HarvesterType {STANDARD, IMS, WORLDCAT}

    @JsonCreator
    public RRV3HarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public RRV3HarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getId();
    }

    @JsonIgnore
    public String getHarvesterToken() {
        final HarvesterToken token = new HarvesterToken()
                .withHarvesterVariant(HarvesterToken.HarvesterVariant.RAW_REPO)
                .withId(getId())
                .withVersion(getVersion());
        return token.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements WorkerKey, Serializable {
        private static final long serialVersionUID = 1L;

        public Content() {
        }

        // Data

        /**
         * ID of harvest operation
         */
        private String id;

        /**
         * Description
         */
        private String description;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        /**
         * JNDI name of rawrepo JDBC resource
         */
        private String resource;

        /**
         * rawrepo queue consumer ID
         */
        private String consumerId;

        /**
         * Destination for harvested items
         */
        private String destination;

        /**
         * Job type of harvested items (default is TRANSIENT
         */
        private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;

        /**
         * Format of harvested items
         */
        private String format;

        /**
         * Optional format overrides for specific agencyIds
         */
        @JsonProperty
        private final Map<Integer, String> formatOverrides = new HashMap<>(); // We need a map implementation with putAll()

        /**
         * Flag indicating whether or not to include
         * record relations in marcXchange collections
         */
        @JsonProperty
        private boolean includeRelations = true;

        /**
         * Flag indicating whether or not to include
         * library rules in record metadata
         */
        @JsonProperty
        private boolean includeLibraryRules = false;

        /**
         * Harvest batch size (default 10000)
         */
        private int batchSize = 10000;

        /**
         * Type og RRHarvesterConfig
         */
        @JsonProperty
        private HarvesterType harvesterType = HarvesterType.STANDARD;

        /**
         * Flag indicating whether or not expand
         * authorization content into records
         */
        @JsonProperty
        private boolean expand = true;

        /**
         * The IMS Holdings Target Url
         */
        private String imsHoldingsTarget;

        /**
         * Note
         */
        private String note;


        // Getters and Setters

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Content withDescription(String description) {
            this.description = description;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Content withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getResource() {
            return resource;
        }

        public Content withResource(String resource) {
            this.resource = resource;
            return this;
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

        public Content withFormatOverridesEntry(Integer formatKey, String formatOverride) {
            this.formatOverrides.put(formatKey, formatOverride);
            return this;
        }

        public Content withFormatOverrides(HashMap<Integer, String> formatOverrides) {
            if (formatOverrides != null) {
                this.formatOverrides.clear();
                this.formatOverrides.putAll(formatOverrides);
            }
            return this;
        }

        public boolean hasIncludeRelations() {
            return includeRelations;
        }

        public Content withIncludeRelations(boolean includeRelations) {
            this.includeRelations = includeRelations;
            return this;
        }

        public boolean hasIncludeLibraryRules() {
            return includeLibraryRules;
        }

        public Content withIncludeLibraryRules(boolean includeLibraryRules) {
            this.includeLibraryRules = includeLibraryRules;
            return this;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Content withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public HarvesterType getHarvesterType() {
            return harvesterType;
        }

        public Content withHarvesterType(HarvesterType harvesterType) {
            this.harvesterType = harvesterType;
            return this;
        }

        public String getImsHoldingsTarget() {
            return imsHoldingsTarget;
        }

        public Content withImsHoldingsTarget(String imsHoldingsTarget) {
            this.imsHoldingsTarget = imsHoldingsTarget;
            return this;
        }

        public String getNote() {
            return note;
        }

        public Content withNote(String note) {
            this.note = note;
            return this;
        }

        public boolean expand() {
            return expand;
        }

        public Content withExpand(boolean expand) {
            this.expand = expand;
            return this;
        }

        @Override
        public List<Priority> priorities() {
            return List.of(WorkerKey.Priority.values());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Content)) return false;

            Content content = (Content) o;

            if (enabled != content.enabled) return false;
            if (expand != content.expand) return false;
            if (includeRelations != content.includeRelations) return false;
            if (includeLibraryRules != content.includeLibraryRules) return false;
            if (batchSize != content.batchSize) return false;
            if (!Objects.equals(id, content.id)) return false;
            if (!Objects.equals(description, content.description)) return false;
            if (!Objects.equals(resource, content.resource)) return false;
            if (!Objects.equals(consumerId, content.consumerId)) return false;
            if (!Objects.equals(destination, content.destination)) return false;
            if (type != content.type) return false;
            if (!Objects.equals(format, content.format)) return false;
            if (!formatOverrides.equals(content.formatOverrides)) return false;
            if (harvesterType != content.harvesterType) return false;
            if (!Objects.equals(imsHoldingsTarget, content.imsHoldingsTarget)) return false;
            return Objects.equals(note, content.note);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (expand ? 1 : 0);
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (consumerId != null ? consumerId.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (formatOverrides != null ? formatOverrides.hashCode() : 0);
            result = 31 * result + (includeRelations ? 1 : 0);
            result = 31 * result + (includeLibraryRules ? 1 : 0);
            result = 31 * result + batchSize;
            result = 31 * result + (harvesterType != null ? harvesterType.hashCode() : 0);
            result = 31 * result + (imsHoldingsTarget != null ? imsHoldingsTarget.hashCode() : 0);
            result = 31 * result + (note != null ? note.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", description='" + description + '\'' +
                    ", enabled=" + enabled +
                    ", expand=" + expand +
                    ", resource='" + resource + '\'' +
                    ", consumerId='" + consumerId + '\'' +
                    ", destination='" + destination + '\'' +
                    ", type=" + type +
                    ", format='" + format + '\'' +
                    ", formatOverrides=" + formatOverrides +
                    ", includeRelations=" + includeRelations +
                    ", includeLibraryRules=" + includeLibraryRules +
                    ", batchSize=" + batchSize +
                    ", harvesterType=" + harvesterType +
                    ", imsHoldingsTarget='" + imsHoldingsTarget + '\'' +
                    ", note='" + note + '\'' +
                    '}';
        }
    }
}
