package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class InfomediaHarvesterConfig extends HarvesterConfig<InfomediaHarvesterConfig.Content> implements Serializable {
    @JsonCreator
    public InfomediaHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public InfomediaHarvesterConfig() {
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
         * Format of harvested items
         */
        private String format;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        private Date timeOfLastHarvest;
        private Date nextPublicationDate;

        public String getId() {
            return id;
        }

        public Content withId(String id) {
            this.id = id;
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

        public Date getNextPublicationDate() {
            if (nextPublicationDate != null) {
                return new Date(nextPublicationDate.getTime());
            }
            return null;
        }

        public Content withNextPublicationDate(Date nextPublicationDate) {
            if (nextPublicationDate != null) {
                this.nextPublicationDate = new Date(nextPublicationDate.getTime());
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
            if (!Objects.equals(timeOfLastHarvest, content.timeOfLastHarvest)) {
                return false;
            }
            return Objects.equals(nextPublicationDate, content.nextPublicationDate);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            result = 31 * result + (nextPublicationDate != null ? nextPublicationDate.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", schedule='" + schedule + '\'' +
                    ", description='" + description + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", enabled=" + enabled +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    ", nextPublicationDate=" + nextPublicationDate +
                    '}';
        }
    }
}
