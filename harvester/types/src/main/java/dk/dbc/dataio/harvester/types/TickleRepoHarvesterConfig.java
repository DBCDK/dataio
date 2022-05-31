package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.io.Serializable;
import java.util.Date;

public class TickleRepoHarvesterConfig extends HarvesterConfig<TickleRepoHarvesterConfig.Content> implements Serializable {
    private static final long serialVersionUID = -1959690053893466276L;

    public enum HarvesterType {STANDARD, VIAF}

    @JsonCreator
    public TickleRepoHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public TickleRepoHarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getId();
    }

    @JsonIgnore
    public String getHarvesterToken(int batchId) {
        final HarvesterToken token = new HarvesterToken()
                .withHarvesterVariant(HarvesterToken.HarvesterVariant.TICKLE_REPO)
                .withId(getId())
                .withVersion(getVersion());
        if (batchId > 0) {
            token.withRemainder("" + batchId);
        }
        return token.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements Serializable {
        private static final long serialVersionUID = -5437124801330551281L;

        public Content() {
        }

        /**
         * ID of harvest operation
         */
        private String id;

        /**
         * JNDI name of tickle repo JDBC resource
         */
        private String datasetName;

        /**
         * Description
         */
        private String description;

        /**
         * Destination for harvested items
         */
        private String destination;

        /**
         * Format of harvested items
         */
        private String format;

        /**
         * Job type of harvested items (default is TRANSIENT
         */
        private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        /**
         * Flag Indicating if job notifications are enabled
         */
        @JsonProperty
        private boolean notificationsEnabled = false;

        private int lastBatchHarvested;

        private Date timeOfLastBatchHarvested;

        @JsonProperty
        private HarvesterType harvesterType = HarvesterType.STANDARD;

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
            return this;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public Content withDatasetName(String datasetName) {
            this.datasetName = datasetName;
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

        public boolean hasNotificationsEnabled() {
            return notificationsEnabled;
        }

        public Content withNotificationsEnabled(boolean enabled) {
            this.notificationsEnabled = enabled;
            return this;
        }

        public int getLastBatchHarvested() {
            return lastBatchHarvested;
        }

        public Content withLastBatchHarvested(int batchId) {
            this.lastBatchHarvested = batchId;
            return this;
        }

        public Date getTimeOfLastBatchHarvested() {
            if (timeOfLastBatchHarvested != null) {
                return new Date(timeOfLastBatchHarvested.getTime());
            }
            return null;
        }

        public Content withTimeOfLastBatchHarvested(Date timeOfLastBatchHarvested) {
            if (timeOfLastBatchHarvested != null) {
                this.timeOfLastBatchHarvested = new Date(timeOfLastBatchHarvested.getTime());
            }
            return this;
        }

        public HarvesterType getHarvesterType() {
            return harvesterType;
        }

        public Content withHarvesterType(HarvesterType harvesterType) {
            this.harvesterType = harvesterType;
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

            if (enabled != content.enabled) {
                return false;
            }
            if (notificationsEnabled != content.notificationsEnabled) {
                return false;
            }
            if (lastBatchHarvested != content.lastBatchHarvested) {
                return false;
            }
            if (id != null ? !id.equals(content.id) : content.id != null) {
                return false;
            }
            if (datasetName != null ? !datasetName.equals(content.datasetName) : content.datasetName != null) {
                return false;
            }
            if (description != null ? !description.equals(content.description) : content.description != null) {
                return false;
            }
            if (destination != null ? !destination.equals(content.destination) : content.destination != null) {
                return false;
            }
            if (format != null ? !format.equals(content.format) : content.format != null) {
                return false;
            }
            if (harvesterType != content.harvesterType) {
                return false;
            }
            return type == content.type;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (datasetName != null ? datasetName.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (notificationsEnabled ? 1 : 0);
            result = 31 * result + lastBatchHarvested;
            result = 31 * result + (harvesterType != null ? harvesterType.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", datasetName='" + datasetName + '\'' +
                    ", description='" + description + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", type=" + type +
                    ", enabled=" + enabled +
                    ", notificationsEnabled=" + notificationsEnabled +
                    ", lastBatchHarvested=" + lastBatchHarvested +
                    ", harvesterType=" + harvesterType +
                    '}';
        }
    }
}
