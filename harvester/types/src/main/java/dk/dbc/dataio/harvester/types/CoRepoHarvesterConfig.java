package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class CoRepoHarvesterConfig extends HarvesterConfig<CoRepoHarvesterConfig.Content> implements Serializable {
    private static final long serialVersionUID = 2511742266375579510L;

    @JsonCreator
    public CoRepoHarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public CoRepoHarvesterConfig() {
    }

    @Override
    public String getLogId() {
        return getContent().getName();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content implements Serializable {
        private static final long serialVersionUID = -7275576820112144156L;

        public Content() {
        }

        /**
         * Name of the CoRepo harvester
         */
        private String name;

        /**
         * Description
         */
        private String description;

        /**
         * Resource - which CoRepo to harvest
         */
        private String resource;

        /**
         * Time of the last harvest
         */
        @JsonProperty
        private Date timeOfLastHarvest;

        /**
         * Flag Indicating if the configuration is enabled
         */
        @JsonProperty
        private boolean enabled = false;

        /**
         * Reference to the linked RR Harvester
         */
        @JsonProperty
        private long rrHarvester;

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

        public String getResource() {
            return resource;
        }

        public Content withResource(String resource) {
            this.resource = resource;
            return this;
        }

        public Date getTimeOfLastHarvest() {
            return timeOfLastHarvest;
        }

        public Content withTimeOfLastHarvest(Date timeOfLastHarvest) {
            this.timeOfLastHarvest = timeOfLastHarvest;
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Content withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public long getRrHarvester() {
            return rrHarvester;
        }

        public Content withRrHarvester(long rrHarvester) {
            this.rrHarvester = rrHarvester;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Content)) return false;

            Content content = (Content) o;

            if (enabled != content.enabled) return false;
            if (rrHarvester != content.rrHarvester) return false;
            if (name != null ? !name.equals(content.name) : content.name != null) return false;
            if (description != null ? !description.equals(content.description) : content.description != null)
                return false;
            if (resource != null ? !resource.equals(content.resource) : content.resource != null) return false;
            return timeOfLastHarvest != null ? timeOfLastHarvest.equals(content.timeOfLastHarvest) : content.timeOfLastHarvest == null;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (resource != null ? resource.hashCode() : 0);
            result = 31 * result + (timeOfLastHarvest != null ? timeOfLastHarvest.hashCode() : 0);
            result = 31 * result + (enabled ? 1 : 0);
            result = 31 * result + (int) (rrHarvester ^ (rrHarvester >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "Content{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", resource='" + resource + '\'' +
                    ", timeOfLastHarvest=" + timeOfLastHarvest +
                    ", enabled=" + enabled +
                    ", rrHarvester=" + rrHarvester +
                    '}';
        }
    }
}
