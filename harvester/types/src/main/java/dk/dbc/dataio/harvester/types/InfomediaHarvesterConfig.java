/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.Date;

public class InfomediaHarvesterConfig extends HarvesterConfig<InfomediaHarvesterConfig.Content> implements Serializable {
    @JsonCreator
    public InfomediaHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public InfomediaHarvesterConfig() { }

    @Override
    public String getLogId() {
        return getContent().getId();
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public static class Content implements Serializable {
        public Content() { }

        /** ID of harvest operation */
        private String id;

        /**
         * Harvest schedule as standard UNIX crontab expression
         * with five fields minute, hour, day of month, month, and
         * day of week
         * @see <a href="https://www.unix.com/man-page/linux/5/crontab/"</a>
         **/
        private String schedule;

        /** Agency ID **/
        private String submitter;

        /** Description */
        private String description;

        /** Destination for harvested items */
        private String destination;

        /** Format of harvested items */
        private String format;

        /** Flag Indicating if the configuration is enabled */
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

        public String getSchedule() {
            return schedule;
        }

        public Content withSchedule(String schedule) {
            this.schedule = schedule;
            return this;
        }

        public String getSubmitter() {
            return submitter;
        }

        public Content withSubmitter(String submitter) {
            this.submitter = submitter;
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
            if (id != null ? !id.equals(content.id) : content.id != null) {
                return false;
            }
            if (schedule != null ? !schedule.equals(content.schedule) : content.schedule != null) {
                return false;
            }
            if (submitter != null ? !submitter.equals(content.submitter) : content.submitter != null) {
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
            return timeOfLastHarvest != null ? timeOfLastHarvest.equals(content.timeOfLastHarvest) : content.timeOfLastHarvest == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
            result = 31 * result + (submitter != null ? submitter.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "id='" + id + '\'' +
                    ", schedule='" + schedule + '\'' +
                    ", submitter='" + submitter + '\'' +
                    ", description='" + description + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", enabled=" + enabled +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    '}';
        }
    }
}
