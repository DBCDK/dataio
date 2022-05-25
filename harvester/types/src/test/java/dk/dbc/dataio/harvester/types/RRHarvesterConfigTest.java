package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RRHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 2, new RRHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final RRHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, RRHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString, is(config));
        assertThat("includeRelations default", configFromString.getContent().hasIncludeRelations(), is(true));
        assertThat("includeLibraryRules default", configFromString.getContent().hasIncludeLibraryRules(), is(false));
        assertThat("imsHarvester default", configFromString.getContent().getHarvesterType(), is(RRHarvesterConfig.HarvesterType.STANDARD));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 2,
                new RRHarvesterConfig.Content()
                        .withId("harvest log id")
                        .withDescription("Description")
                        .withEnabled(true)
                        .withResource("Resource")
                        .withConsumerId("ConsumerId")
                        .withDestination("Destination")
                        .withType(JobSpecification.Type.ACCTEST)
                        .withFormat("format")
                        .withFormatOverridesEntry(12, "formatX")
                        .withFormatOverridesEntry(191919, "formatY")
                        .withIncludeRelations(false)
                        .withIncludeLibraryRules(true)
                        .withBatchSize(12)
                        .withImsHoldingsTarget("ImsHoldingsTarget")
                        .withNote("Note")
        );

        final String configAsString = jsonbContext.marshall(config);

        final RRHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, RRHarvesterConfig.class);
        assertThat("unmarshalling", configFromString, is(config));
        assertThat("expand default", config.getContent().expand(), is(true));
    }

    @Test
    public void getHarvesterToken() {
        final RRHarvesterConfig config = new RRHarvesterConfig(42, 1, new RRHarvesterConfig.Content());
        assertThat(config.getHarvesterToken(), is("raw-repo:42:1"));
    }
}
