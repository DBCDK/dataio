package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InfomediaHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaults() throws JSONBException {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final InfomediaHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, InfomediaHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
    }

    @Test
    public void marshalling() throws JSONBException {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("* * * * *")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(true)
                        .withTimeOfLastHarvest(new Date())
        );
        final String configAsString = jsonbContext.marshall(config);

        final InfomediaHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, InfomediaHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}
