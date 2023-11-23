package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PromatHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaults() throws JSONBException {
        final PromatHarvesterConfig config = new PromatHarvesterConfig(1, 2,
                new PromatHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final PromatHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, PromatHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
    }

    @Test
    public void marshalling() throws JSONBException {
        final PromatHarvesterConfig config = new PromatHarvesterConfig(1, 2,
                new PromatHarvesterConfig.Content()
                        .withName("-name-")
                        .withDescription("-description-")
                        .withSchedule("* * * * *")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withTimeOfLastHarvest(new Date())
                        .withEnabled(true)
        );
        final String configAsString = jsonbContext.marshall(config);

        final PromatHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, PromatHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}
