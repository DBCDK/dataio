package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
        assertThat("imsHarvester default", configFromString.getContent().isImsHarvester(), is(false));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 2,
                new RRHarvesterConfig.Content()
                        .withId("harvest log id")
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
                        .withOpenAgencyTarget(new OpenAgencyTarget())
                        .withImsHarvester(false)
                        .withImsHoldingsTarget("ImsHoldingsTarget")
        );
        final String configAsString = jsonbContext.marshall(config);

        final RRHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, RRHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}