package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RRV3HarvesterConfig extends HarvesterConfig<RRV3HarvesterConfig.Content> implements Serializable {
    @Serial
    private static final long serialVersionUID = 6278782413534093538L;

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
        @Serial
        private static final long serialVersionUID = -8707091224407698754L;

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

        /**
         * Filter for submitters
         */
        private SubmitterFilter submitterFilter;

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

        public SubmitterFilter getSubmitterFilter() {
            return submitterFilter;
        }

        public Content withSubmitterFilter(SubmitterFilter submitterFilter) {
            this.submitterFilter = submitterFilter;
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
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Content content = (Content) o;
            return Objects.equals(id, content.id)
                    && Objects.equals(description, content.description)
                    && enabled == content.enabled
                    && Objects.equals(resource, content.resource)
                    && Objects.equals(consumerId, content.consumerId)
                    && Objects.equals(destination, content.destination)
                    && type == content.type
                    && Objects.equals(format, content.format)
                    && Objects.equals(formatOverrides, content.formatOverrides)
                    && includeRelations == content.includeRelations
                    && includeLibraryRules == content.includeLibraryRules
                    && batchSize == content.batchSize
                    && harvesterType == content.harvesterType
                    && expand == content.expand
                    && Objects.equals(imsHoldingsTarget, content.imsHoldingsTarget)
                    && Objects.equals(note, content.note)
                    && Objects.equals(submitterFilter, content.submitterFilter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    id,
                    description,
                    enabled,
                    resource,
                    consumerId,
                    destination,
                    type,
                    format,
                    formatOverrides,
                    includeRelations,
                    includeLibraryRules,
                    batchSize,
                    harvesterType,
                    expand,
                    imsHoldingsTarget,
                    note,
                    submitterFilter);
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", description='" + description + '\'' +
                    ", enabled=" + enabled +
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
                    ", expand=" + expand +
                    ", imsHoldingsTarget='" + imsHoldingsTarget + '\'' +
                    ", note='" + note + '\'' +
                    ", submitterFilter=" + submitterFilter +
                    '}';
        }
    }
}
