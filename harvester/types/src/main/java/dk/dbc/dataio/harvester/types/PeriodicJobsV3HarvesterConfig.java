package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Configuration for the rawrepo v3 based periodic jobs harvester.
 * <p>
 * Extends {@link PeriodicJobsHarvesterConfig} without adding or changing anything,
 * the only purpose of the subtype being to let the harvester tell its own configs
 * apart from those of the rawrepo v1 based harvester when querying the flow-store
 * by type. Since consumers downstream of the harvester, most notably the
 * periodic-jobs sink, read the config as a {@link PeriodicJobsHarvesterConfig},
 * this type must remain a subtype of it for the polymorphic deserialization to
 * resolve.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodicJobsV3HarvesterConfig
        extends PeriodicJobsHarvesterConfig
        implements Serializable {

    @JsonCreator
    public PeriodicJobsV3HarvesterConfig(
            @JsonProperty("id") long id,
            @JsonProperty("version") long version,
            @JsonProperty("content") Content content)
            throws NullPointerException, IllegalArgumentException {
        super(id, version, content);
    }

    public PeriodicJobsV3HarvesterConfig() {
    }
}
