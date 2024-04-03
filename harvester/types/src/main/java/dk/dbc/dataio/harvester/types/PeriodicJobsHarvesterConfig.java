package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.HarvesterToken;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class PeriodicJobsHarvesterConfig
        extends HarvesterConfig<PeriodicJobsHarvesterConfig.Content>
        implements Serializable {

    public enum HarvesterType {STANDARD, DAILY_PROOFING, SUBJECT_PROOFING, STANDARD_WITH_HOLDINGS, STANDARD_WITHOUT_EXPANSION}
    public enum HoldingsFilter {WITH_HOLDINGS, WITHOUT_HOLDINGS}

    public enum PickupType {HTTP, MAIL, FTP, SFTP, ANY_SINK}

    @JsonCreator
    public PeriodicJobsHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public PeriodicJobsHarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getName();
    }

    @JsonIgnore
    public String getHarvesterToken() {
        final HarvesterToken token = new HarvesterToken()
                .withHarvesterVariant(HarvesterToken.HarvesterVariant.PERIODIC_JOBS)
                .withId(getId())
                .withVersion(getVersion());
        return token.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements Serializable {
        public Content() {
        }

        private String name;

        private Pickup pickup;

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
         * The Solr query that is the basis of this periodic harvest
         */
        private String query;

        private String queryFileId;

        /**
         * The Solr collection to query
         */
        private String collection;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        private Date timeOfLastHarvest;

        /**
         * JNDI name of raw-repo JDBC resource
         */
        private String resource;

        /**
         * Destination for harvested items
         */
        private String destination;

        /**
         * Format of harvested items
         */
        private String format;

        private String submitterNumber;

        /**
         * Type of harvester
         */
        @JsonProperty
        private HarvesterType harvesterType = HarvesterType.STANDARD;

        /**
         * Contact person eq. mail or initials
         */
        private String contact;

        private HoldingsFilter holdingsFilter;

        private String holdingsSolrUrl;

        public String getName() {
            return name;
        }

        public Content withName(String name) {
            this.name = name;
            return this;
        }

        public Pickup getPickup() {
            return pickup;
        }

        public Content withPickup(Pickup pickup) {
            this.pickup = pickup;
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

        public String getQuery() {
            return query;
        }

        public Content withQuery(String query) {
            this.query = query;
            return this;
        }

        public String getQueryFileId() {
            return queryFileId;
        }

        public Content withQueryFileId(String queryFileId) {
            this.queryFileId = queryFileId;
            return this;
        }

        public String getCollection() {
            return collection;
        }

        public Content withCollection(String collection) {
            this.collection = collection;
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

        public String getResource() {
            return resource;
        }

        public Content withResource(String resource) {
            this.resource = resource;
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

        public HarvesterType getHarvesterType() {
            return harvesterType;
        }

        public Content withHarvesterType(HarvesterType harvesterType) {
            this.harvesterType = harvesterType;
            return this;
        }

        public String getContact() {
            return contact;
        }

        public Content withContact(String contact) {
            this.contact = contact;
            return this;
        }

        public HoldingsFilter getHoldingsFilter() {
            return holdingsFilter;
        }

        public Content withHoldingsFilter(HoldingsFilter holdingsFilter) {
            this.holdingsFilter = holdingsFilter;
            return this;
        }

        public String getHoldingsSolrUrl() {
            return holdingsSolrUrl;
        }

        public Content withHoldingsSolrUrl(String holdingsSolrUrl) {
            this.holdingsSolrUrl = holdingsSolrUrl;
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
            if (!Objects.equals(name, content.name)) {
                return false;
            }
            if (!Objects.equals(pickup, content.pickup)) {
                return false;
            }
            if (!Objects.equals(description, content.description)) {
                return false;
            }
            if (!Objects.equals(schedule, content.schedule)) {
                return false;
            }
            if (!Objects.equals(query, content.query)) {
                return false;
            }
            if (!Objects.equals(collection, content.collection)) {
                return false;
            }
            if (!Objects.equals(timeOfLastHarvest, content.timeOfLastHarvest)) {
                return false;
            }
            if (!Objects.equals(resource, content.resource)) {
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
            if (harvesterType != content.harvesterType) {
                return false;
            }

            if (!Objects.equals(holdingsFilter, content.holdingsFilter)) {
                return false;
            }

            if (!Objects.equals(holdingsSolrUrl, content.holdingsSolrUrl)) {
                return false;
            }

            return Objects.equals(contact, content.contact);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (pickup != null ? pickup.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
            result = 31 * result + (query != null ? query.hashCode() : 0);
            result = 31 * result + (collection != null ? collection.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (destination != null ? destination.hashCode() : 0);
            result = 31 * result + (format != null ? format.hashCode() : 0);
            result = 31 * result + (submitterNumber != null ? submitterNumber.hashCode() : 0);
            result = 31 * result + (harvesterType != null ? harvesterType.hashCode() : 0);
            result = 31 * result + (contact != null ? contact.hashCode() : 0);
            result = 31 * result + (holdingsFilter != null ? holdingsFilter.hashCode() : 0);
            result = 31 * result + (holdingsSolrUrl != null ? holdingsSolrUrl.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "name='" + name + '\'' +
                    ", pickup='" + pickup + '\'' +
                    ", description='" + description + '\'' +
                    ", schedule='" + schedule + '\'' +
                    ", query='" + query + '\'' +
                    ", collection='" + collection + '\'' +
                    ", enabled=" + enabled +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    ", resource='" + resource + '\'' +
                    ", destination='" + destination + '\'' +
                    ", format='" + format + '\'' +
                    ", submitterNumber='" + submitterNumber + '\'' +
                    ", harvesterType=" + harvesterType +
                    ", contact='" + contact + '\'' +
                    '}';
        }
    }
}
