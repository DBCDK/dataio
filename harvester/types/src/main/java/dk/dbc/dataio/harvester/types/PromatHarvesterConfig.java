package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class PromatHarvesterConfig
        extends HarvesterConfig<PromatHarvesterConfig.Content>
        implements Serializable {

    @JsonCreator
    public PromatHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public PromatHarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getName();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements Serializable {
        public Content() {
        }

        private String name;

        private String description;

        /**
         * Harvest schedule as standard UNIX crontab expression
         * with five fields minute, hour, day of month, month, and
         * day of week
         *
         * @see <a href="https://github.com/DBCDK/run-schedule">run-schedule</a>
         **/
        private String schedule;

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


        public String getName() {
            return name;
        }

        public Content withName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Content withDescription(String description) {
            this.description = description;
            return this;
        }

        public String getSchedule() {
            return schedule;
        }

        public Content withSchedule(String schedule) {
            this.schedule = schedule;
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
            if (name != null ? !name.equals(content.name) : content.name != null) {
                return false;
            }
            if (description != null ? !description.equals(content.description) : content.description != null) {
                return false;
            }
            if (schedule != null ? !schedule.equals(content.schedule) : content.schedule != null) {
                return false;
            }
            if (destination != null ? !destination.equals(content.destination) : content.destination != null) {
                return false;
            }
            if (format != null ? !format.equals(content.format) : content.format != null) {
                return false;
            }
            return timeOfLastHarvest != null ? timeOfLastHarvest.equals(content.timeOfLastHarvest) : content.timeOfLastHarvest == null;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", schedule='" + schedule + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", enabled=" + enabled +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    '}';
        }
    }
}
