package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TickleRepoHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2,
                new TickleRepoHarvesterConfig.Content());
        String configAsString = jsonbContext.marshall(config);

        TickleRepoHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, TickleRepoHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("type default", configFromString.getContent().getType(),
                is(JobSpecification.Type.TRANSIENT));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
        assertThat("notificationsEnabled default", configFromString.getContent().hasNotificationsEnabled(),
                is(false));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2,
                new TickleRepoHarvesterConfig.Content()
                        .withId("-id-")
                        .withDatasetName("-name")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withType(JobSpecification.Type.TEST)
                        .withEnabled(true)
                        .withNotificationsEnabled(true)
        );
        String configAsString = jsonbContext.marshall(config);

        TickleRepoHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, TickleRepoHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }

    @Test
    public void getHarvesterToken() {
        TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2, new TickleRepoHarvesterConfig.Content());
        assertThat(config.getHarvesterToken(42), is("tickle-repo:1:2:42"));
        assertThat(config.getHarvesterToken(0), is("tickle-repo:1:2"));
    }
}
