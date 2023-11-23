package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CoRepoHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        final CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(1, 2, new CoRepoHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final CoRepoHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, CoRepoHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString, is(config));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(3, 4,
                new CoRepoHarvesterConfig.Content()
                        .withName("Namo")
                        .withDescription("Descripo")
                        .withTimeOfLastHarvest(new Date(12345678))
                        .withResource("Resource")
                        .withEnabled(true)
                        .withRrHarvester(321L)
        );
        final String configAsString = jsonbContext.marshall(config);

        final CoRepoHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, CoRepoHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}
