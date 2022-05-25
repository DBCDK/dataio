package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class OaiHarvesterConfig extends HarvesterConfig<OaiHarvesterConfig.Content> implements Serializable {
    @JsonCreator
    public OaiHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public OaiHarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getId();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements Serializable {
        public Content() {
        }

        /**
         * ID of harvest operation
         */
        private String id;

        /**
         * OAI service endpoint
         */
        private String endpoint;

        /**
         * OAI set spec
         */
        private String set;

        /**
         * Harvest schedule as standard UNIX crontab expression
         * with five fields minute, hour, day of month, month, and
         * day of week
         *
         * @see <a href="https://www.unix.com/man-page/linux/5/crontab/">crontab</a>
         **/
        private String schedule;

        /**
         * Description
         */
        private String description;

        /**
         * Destination for harvested items
         */
        private String destination;

        /**
         * Format of harvested items,
         * must correspond with the metadataPrefix used by OAI-PMH
         **/
        private String format;

        private String submitterNumber;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        private Date timeOfLastHarvest;

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
            return this;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public Content withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public String getSet() {
            return set;
        }

        public Content withSet(String set) {
            this.set = set;
            return this;
        }

        public String getSchedule() {
            return schedule;
        }

        public Content withSchedule(String schedule) {
            this.schedule = schedule;
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

        public String getSubmitterNumber() {
            return submitterNumber;
        }

        public Content withSubmitterNumber(String submitterNumber) {
            this.submitterNumber = submitterNumber;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Content withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Date getTimeOfLastHarvest() {
            if (timeOfLastHarvest != null) {
                return new Date(timeOfLastHarvest.getTime());
            }
            return null;
        }

        public Content withTimeOfLastHarvest(Date timeOfLastHarvest) {
            if (timeOfLastHarvest != null) {
                this.timeOfLastHarvest = new Date(timeOfLastHarvest.getTime());
            }
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
            if (!Objects.equals(id, content.id)) {
                return false;
            }
            if (!Objects.equals(endpoint, content.endpoint)) {
                return false;
            }
            if (!Objects.equals(set, content.set)) {
                return false;
            }
            if (!Objects.equals(schedule, content.schedule)) {
                return false;
            }
            if (!Objects.equals(description, content.description)) {
                return false;
            }
            if (!Objects.equals(destination, content.destination)) {
                return false;
            }
            if (!Objects.equals(format, content.format)) {
                return false;
            }
            if (!Objects.equals(submitterNumber, content.submitterNumber)) {
                return false;
            }
            return Objects.equals(timeOfLastHarvest, content.timeOfLastHarvest);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
            result = 31 * result + (set != null ? set.hashCode() : 0);
            result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (submitterNumber != null ? submitterNumber.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", endpoint='" + endpoint + '\'' +
                    ", set='" + set + '\'' +
                    ", schedule='" + schedule + '\'' +
                    ", description='" + description + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", submitterNumber='" + submitterNumber + '\'' +
                    ", enabled=" + enabled +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    '}';
        }
    }
}
